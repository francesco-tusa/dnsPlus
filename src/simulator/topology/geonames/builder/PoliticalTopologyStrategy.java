package simulator.topology.geonames.builder;

import java.io.BufferedReader;
import java.util.*;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.regions.Region;
import simulator.topology.analysis.TopologyStatisticsCalculator; 
import utils.CustomLogger;

public class PoliticalTopologyStrategy implements TopologyBuilderStrategy {
    private static final Logger logger = CustomLogger.getLogger(PoliticalTopologyStrategy.class.getName());
    
    private final SimConfiguration config = SimConfiguration.get();

    // Data structures for building the hierarchy
    private final Map<Integer, RelevantAdminInfo> relevantAdminMap = new HashMap<>();
    private final Map<Integer, Long> adm1PopMap = new HashMap<>();
    private final Map<Integer, Region> adm1BoundsMap = new HashMap<>();
    private final Map<Integer, Long> adm2PopMap = new HashMap<>();
    private final Map<Integer, Region> adm2BoundsMap = new HashMap<>();
    private final Map<Integer, GeoNamesBuilderNode> nodeMap = new HashMap<>();

    private final AreaEstimator areaEstimator = new PowerLawAreaEstimator(); 

    private static class RelevantAdminInfo {
        int id; String name; GeoNamesBuilderNode.NodeType type; String code; String feature; String country; String admin1;
        long officialPop;
        RelevantAdminInfo(GeoNamesEntry e, GeoNamesBuilderNode.NodeType t) {
            this.id = e.geonameId; this.name = e.name; this.type = t; this.feature = e.featureCode;
            this.country = e.countryCode; this.admin1 = e.admin1Code;
            if (t == GeoNamesBuilderNode.NodeType.COUNTRY) { this.code = e.countryCode; this.officialPop = e.population; }
            else if (t == GeoNamesBuilderNode.NodeType.ADM1) this.code = e.admin1Code;
            else if (t == GeoNamesBuilderNode.NodeType.ADM2) this.code = e.admin2Code;
        }
    }

    @Override
    public GeoNamesBuilderNode createSubset(GeoNamesBuilderNode root) {
        logger.info("Generating Political Subset (Bangladesh & Beijing)...");
        GeoNamesBuilderNode subsetRoot = new GeoNamesBuilderNode(root);
        
        for (GeoNamesBuilderNode continent : root.children) {
            if ("AS".equals(continent.code)) {
                GeoNamesBuilderNode subsetAsia = new GeoNamesBuilderNode(continent);
                subsetRoot.addChild(subsetAsia);
                for (GeoNamesBuilderNode country : continent.children) {
                    if ("BD".equals(country.code)) subsetAsia.addChild(country); 
                    if ("CN".equals(country.code)) {
                        GeoNamesBuilderNode subsetChina = new GeoNamesBuilderNode(country);
                        subsetAsia.addChild(subsetChina);
                        for (GeoNamesBuilderNode adm1 : country.children) {
                            if (adm1.name.contains("Beijing")) subsetChina.addChild(adm1);
                        }
                    }
                }
            }
        }
        return subsetRoot;
    }

    @Override
    public GeoNamesBuilderNode build(GeoNamesDataLoader loader) {
        logger.info("Executing Political Topology Strategy...");
        
        processPass1(loader);
        GeoNamesBuilderNode root = buildInitialHierarchy(loader);
        
        List<GeoNamesBuilderNode> nodesToExpand = new ArrayList<>();
        assignInitialProps(root, nodesToExpand);
        addAdm2Layer(nodesToExpand);
        distributeAdm1Pop(nodesToExpand);

        logger.info("Pass 6.5: Inflating zero-size regions based on final population...");
        applyAreaEstimation(root);
        estimateAdm2Bounds(nodesToExpand);
        
        logger.info("Pass 7: Scaling populations...");
        scalePopulations(root, loader);
        
        logger.info("Validating pre-expansion leaf bounds (Sorted by Pop Desc)...");
        validateLeafBounds(root, "N/A"); 

        if (config.topology.enablePoliticalExpansion) {
            int maxBranching = config.topology.branchingFactor;
            
            // Pass 8: Coarse Expansion
            long thresholdCoarse = config.topology.politicalCoarseThreshold; 
            LeafExpansionStrategy gridPass1 = new GridLeafExpansionStrategy(false, maxBranching);
            
            logger.info(String.format("Pass 8: Expanding Leaves > %d (Branching=%s)...", 
                    thresholdCoarse, (maxBranching > 0 ? maxBranching : "Flat")));
            expandLeaves(root, thresholdCoarse, gridPass1, GeoNamesBuilderNode.NodeType.GRID_COARSE);
            
            // Pass 9: Fine Expansion
            long thresholdFine = config.topology.politicalLeafCapacity;
            LeafExpansionStrategy gridPass2 = new GridLeafExpansionStrategy(true, maxBranching);
            
            logger.info(String.format("Pass 9: Expanding Leaves > %d (Branching=%s)...", 
                    thresholdFine, (maxBranching > 0 ? maxBranching : "Flat")));
            
            expandLeaves(root, thresholdFine, gridPass2, GeoNamesBuilderNode.NodeType.GRID_FINE);
        } else {
            logger.info("Pass 8 & 9 (Grid Expansion) SKIPPED by configuration.");
        }
        
        logger.info("Pass 10: Final Aggregation...");
        finalAggregate(root);

        if (config.topology.enablePoliticalAnalysis) {
            TopologyStatisticsCalculator.analyzeContinentExtremities(root);
        }

        TopologyStatisticsCalculator.logLeafRegionStatistics(root);
        TopologyStatisticsCalculator.printNodeTypeStatistics(root);
        
        return root;
    }

    // ... [Rest of the file remains exactly the same] ...
    private void processPass1(GeoNamesDataLoader loader) {
        try (BufferedReader reader = loader.getRawDataReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                GeoNamesEntry entry = new GeoNamesEntry(line.split("\t", -1));
                if (entry.geonameId == -1) continue;
                GeoNamesBuilderNode.NodeType type = determineType(entry.featureClass, entry.featureCode);
                if (type == null) continue;
                if (type == GeoNamesBuilderNode.NodeType.PPL) {
                    String adm1Key = entry.countryCode + "." + entry.admin1Code;
                    Integer adm1Id = loader.admin1CodeToIdMap.get(adm1Key);
                    if (adm1Id != null) {
                        adm1PopMap.merge(adm1Id, entry.population, Long::sum);
                        adm1BoundsMap.computeIfAbsent(adm1Id, k -> new Region()).expand(new Location(entry.longitude, entry.latitude, 0));
                        String adm2Key = adm1Key + "." + entry.admin2Code;
                        Integer adm2Id = loader.admin2CodeToIdMap.get(adm2Key);
                        if (adm2Id != null) {
                            adm2PopMap.merge(adm2Id, entry.population, Long::sum);
                            adm2BoundsMap.computeIfAbsent(adm2Id, k -> new Region()).expand(new Location(entry.longitude, entry.latitude, 0));
                        }
                    }
                } else {
                    relevantAdminMap.put(entry.geonameId, new RelevantAdminInfo(entry, type));
                }
            }
        } catch (Exception e) { logger.severe("Pass 1 failed: " + e); }
    }

    private GeoNamesBuilderNode.NodeType determineType(String fcl, String fcode) {
        if ("PCLI".equals(fcode)) return GeoNamesBuilderNode.NodeType.COUNTRY;
        if ("ADM1".equals(fcode)) return GeoNamesBuilderNode.NodeType.ADM1;
        if ("ADM2".equals(fcode)) return GeoNamesBuilderNode.NodeType.ADM2;
        if ("P".equals(fcl)) return GeoNamesBuilderNode.NodeType.PPL;
        return null;
    }

    private GeoNamesBuilderNode buildInitialHierarchy(GeoNamesDataLoader loader) {
        GeoNamesBuilderNode world = new GeoNamesBuilderNode(0, "World", GeoNamesBuilderNode.NodeType.WORLD, "WORLD", "");
        Map<String, GeoNamesBuilderNode> continents = new HashMap<>();
        nodeMap.clear();
        nodeMap.put(0, world);

        for (String iso : loader.countryCodeToIdMap.keySet()) {
            int id = loader.countryCodeToIdMap.get(iso);
            String name = loader.countryCodeToNameMap.getOrDefault(iso, iso);
            GeoNamesBuilderNode country = new GeoNamesBuilderNode(id, name, GeoNamesBuilderNode.NodeType.COUNTRY, iso, "PCLI");
            RelevantAdminInfo info = relevantAdminMap.get(id);
            if (info != null) country.officialPopulation = info.officialPop;
            nodeMap.put(id, country);
            String contCode = loader.countryToContinentMap.getOrDefault(iso, "XX");
            continents.computeIfAbsent(contCode, k -> {
                GeoNamesBuilderNode c = new GeoNamesBuilderNode(0, k, GeoNamesBuilderNode.NodeType.CONTINENT, k, "");
                world.addChild(c);
                return c;
            }).addChild(country);
        }
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            if (info.type == GeoNamesBuilderNode.NodeType.ADM1) {
                GeoNamesBuilderNode adm1 = new GeoNamesBuilderNode(info.id, info.name, info.type, info.code, info.feature);
                nodeMap.put(info.id, adm1);
                Integer countryId = loader.countryCodeToIdMap.get(info.country);
                if (countryId != null) {
                    GeoNamesBuilderNode country = nodeMap.get(countryId);
                    if (country != null) country.addChild(adm1);
                }
            }
        }
        return world;
    }

    private void assignInitialProps(GeoNamesBuilderNode node, List<GeoNamesBuilderNode> expandList) {
        if (node.type == GeoNamesBuilderNode.NodeType.ADM1) {
            node.aggregatedPopulation = adm1PopMap.getOrDefault(node.geonameId, 0L);
            node.bounds = adm1BoundsMap.getOrDefault(node.geonameId, new Region());
            
            // Note: We populate expandList regardless, but check config later before acting on it
            if (node.aggregatedPopulation > config.topology.politicalCoarseThreshold) expandList.add(node);
        }
        for (GeoNamesBuilderNode child : node.children) assignInitialProps(child, expandList);
    }

    private void addAdm2Layer(List<GeoNamesBuilderNode> adm1Nodes) {
        for (GeoNamesBuilderNode adm1 : adm1Nodes) {
            RelevantAdminInfo info = relevantAdminMap.get(adm1.geonameId);
            if (info == null) continue;
            for (RelevantAdminInfo cand : relevantAdminMap.values()) {
                if (cand.type == GeoNamesBuilderNode.NodeType.ADM2 && 
                    cand.country.equals(info.country) && cand.admin1.equals(info.code)) {
                    GeoNamesBuilderNode adm2 = new GeoNamesBuilderNode(cand.id, cand.name, cand.type, cand.code, cand.feature);
                    adm2.aggregatedPopulation = adm2PopMap.getOrDefault(cand.id, 0L);
                    adm2.bounds = adm2BoundsMap.getOrDefault(cand.id, new Region());
                    adm1.addChild(adm2);
                }
            }
        }
    }

    private void distributeAdm1Pop(List<GeoNamesBuilderNode> nodes) {
        for (GeoNamesBuilderNode adm1 : nodes) {
            long total = adm1.aggregatedPopulation;
            List<GeoNamesBuilderNode> zeroPopChildren = new ArrayList<>();
            long currentSum = 0;
            for(GeoNamesBuilderNode c : adm1.children) {
                if (c.aggregatedPopulation == 0) zeroPopChildren.add(c);
                else currentSum += c.aggregatedPopulation;
            }
            long remaining = total - currentSum;
            if (remaining > 0 && !zeroPopChildren.isEmpty()) {
                long share = remaining / zeroPopChildren.size();
                long remainder = remaining % zeroPopChildren.size();
                for (int i = 0; i < zeroPopChildren.size(); i++) {
                    GeoNamesBuilderNode child = zeroPopChildren.get(i);
                    child.aggregatedPopulation = share + (i < remainder ? 1 : 0);
                }
            }
        }
    }

    private void estimateAdm2Bounds(List<GeoNamesBuilderNode> nodes) {
        for (GeoNamesBuilderNode adm1 : nodes) {
            if (adm1.bounds == null || adm1.bounds.getBottomLeft() == null) continue;
            List<GeoNamesBuilderNode> missingBounds = new ArrayList<>();
            for (GeoNamesBuilderNode child : adm1.children) {
                if (child.bounds == null || child.bounds.getBottomLeft() == null) missingBounds.add(child);
            }
            if (missingBounds.isEmpty()) continue;
            int n = missingBounds.size();
            int cols = (int) Math.ceil(Math.sqrt(n));
            int rows = (int) Math.ceil((double) n / cols);
            double w = adm1.bounds.getWidth() / cols;
            double h = adm1.bounds.getHeight() / rows;
            double startX = adm1.bounds.getBottomLeft().getX();
            double startY = adm1.bounds.getBottomLeft().getY();
            missingBounds.sort(Comparator.comparing(n2 -> n2.name));
            for (int i = 0; i < n; i++) {
                int r = i / cols;
                int c = i % cols;
                GeoNamesBuilderNode child = missingBounds.get(i);
                child.bounds = new Region(
                    new Location(startX + c*w, startY + r*h, 0),
                    new Location(startX + (c+1)*w, startY + (r+1)*h, 0)
                );
            }
        }
    }

    private void applyAreaEstimation(GeoNamesBuilderNode node) {
        if (node.type == GeoNamesBuilderNode.NodeType.ADM1 || 
            node.type == GeoNamesBuilderNode.NodeType.ADM2 ||
            node.type == GeoNamesBuilderNode.NodeType.COUNTRY) {
            if (node.bounds != null) {
                node.bounds = areaEstimator.estimate(node.bounds, node.aggregatedPopulation);
            }
        }
        if (node.children != null) {
            for (GeoNamesBuilderNode child : node.children) applyAreaEstimation(child);
        }
    }

    private void scalePopulations(GeoNamesBuilderNode root, GeoNamesDataLoader loader) {
        for (GeoNamesBuilderNode continent : root.children) {
            for (GeoNamesBuilderNode country : continent.children) {
                if (country.type != GeoNamesBuilderNode.NodeType.COUNTRY) continue;
                Long official = country.officialPopulation != null ? country.officialPopulation : 0L;
                Double rate = loader.countryIsoToPenetrationMap.get(country.code);
                if (rate == null) rate = 0.0;
                country.internetPenetrationRate = rate;
                long currentTotal = sumSubtreePopulation(country);
                double scaleFactor = 1.0;
                if (official > 0 && currentTotal > 0) scaleFactor = (double) official / currentTotal;
                applyScaling(country, scaleFactor, rate);
            }
        }
    }
    
    private long sumSubtreePopulation(GeoNamesBuilderNode node) {
        if (node == null) return 0;
        if (node.children.isEmpty()) return node.aggregatedPopulation;
        long sum = 0;
        for (GeoNamesBuilderNode child : node.children) sum += sumSubtreePopulation(child);
        return sum;
    }

    private void applyScaling(GeoNamesBuilderNode node, double scale, double rate) {
        node.aggregatedPopulation = Math.max(0, Math.round(node.aggregatedPopulation * scale));
        node.internetPopulation = Math.min(node.aggregatedPopulation, 
            Math.max(0, Math.round(node.aggregatedPopulation * rate)));
        for (GeoNamesBuilderNode child : node.children) applyScaling(child, scale, rate);
    }

    private void expandLeaves(GeoNamesBuilderNode node, long threshold, LeafExpansionStrategy strategy, GeoNamesBuilderNode.NodeType type) {
        if (node.children.isEmpty()) {
            strategy.expand(node, threshold, type);
            return;
        }
        List<GeoNamesBuilderNode> childrenCopy = new ArrayList<>(node.children);
        for (GeoNamesBuilderNode child : childrenCopy) expandLeaves(child, threshold, strategy, type);
    }

    private void finalAggregate(GeoNamesBuilderNode node) {
        if (node.children.isEmpty()) return;
        long aggPop = 0;
        long netPop = 0;
        Region bounds = new Region();
        for (GeoNamesBuilderNode child : node.children) {
            finalAggregate(child);
            aggPop += child.aggregatedPopulation;
            netPop += child.internetPopulation;
            if (child.bounds != null) bounds.expand(child.bounds);
        }
        node.aggregatedPopulation = aggPop;
        node.internetPopulation = netPop;
        if (node.type == GeoNamesBuilderNode.NodeType.WORLD) {
            node.bounds = new Region(new Location(-180, -90, 0), new Location(180, 90, 0));
        } else {
            if (bounds.getBottomLeft() != null) node.bounds = bounds;
        }
    }

    private void validateLeafBounds(GeoNamesBuilderNode root, String initialContext) {
        List<ZeroSizeIssue> issues = new ArrayList<>();
        collectZeroSizeLeaves(root, initialContext, issues);
        issues.sort((a, b) -> Long.compare(b.node.aggregatedPopulation, a.node.aggregatedPopulation));
        if (!issues.isEmpty()) logger.info(String.format("Found %d Zero-Size Leaves. Top listings by Population:", issues.size()));
        for (ZeroSizeIssue issue : issues) {
            GeoNamesBuilderNode node = issue.node;
            logger.warning(String.format("WARNING: Zero-Size Leaf Detected! Node '%s' (ID: %d) | Pop: %d | ADM1: [%s] | Bounds: %s", 
                node.name, node.geonameId, node.aggregatedPopulation, issue.adm1Context, node.bounds != null ? node.bounds.toShortString() : "null"));
        }
    }

    private void collectZeroSizeLeaves(GeoNamesBuilderNode node, String currentAdm1Info, List<ZeroSizeIssue> issues) {
        String adm1Context = currentAdm1Info;
        if (node.type == GeoNamesBuilderNode.NodeType.ADM1) adm1Context = String.format("%s (ID: %d)", node.name, node.geonameId);
        if (node.children.isEmpty()) {
            if (node.bounds != null && node.bounds.getBottomLeft() != null) {
                double w = node.bounds.getWidth();
                double h = node.bounds.getHeight();
                if (w < 1e-6 || h < 1e-6) issues.add(new ZeroSizeIssue(node, adm1Context));
            }
            return;
        }
        for (GeoNamesBuilderNode child : node.children) collectZeroSizeLeaves(child, adm1Context, issues);
    }

    private interface AreaEstimator { Region estimate(Region currentBounds, long population); }
    private static class PowerLawAreaEstimator implements AreaEstimator {
        private static final double K_CONST = 0.061779; private static final double ALPHA_EXP = 0.631462;
        private static final double KM_PER_DEGREE = 111.0; private static final double MIN_DEGREE = 0.02; private static final double MAX_DEGREE = 2.0;
        @Override
        public Region estimate(Region r, long population) {
            if (r == null || r.getBottomLeft() == null) return new Region();
            if (r.getWidth() > 1e-4 && r.getHeight() > 1e-4) return r;
            long effectivePop = (population <= 0) ? 1000 : population;
            double impliedAreaKm2 = K_CONST * Math.pow(effectivePop, ALPHA_EXP);
            double sideLengthKm = Math.sqrt(impliedAreaKm2);
            double degrees = sideLengthKm / KM_PER_DEGREE;
            return expandPoint(r, degrees, MIN_DEGREE, MAX_DEGREE);
        }
    }
    private static Region expandPoint(Region r, double targetDegrees, double min, double max) {
        double size = Math.max(min, Math.min(max, targetDegrees));
        double half = size / 2.0;
        double centerX = (r.getBottomLeft().getX() + r.getTopRight().getX()) / 2.0;
        double centerY = (r.getBottomLeft().getY() + r.getTopRight().getY()) / 2.0;
        return new Region(new Location(centerX - half, centerY - half, 0), new Location(centerX + half, centerY + half, 0));
    }
    private static class ZeroSizeIssue {
        final GeoNamesBuilderNode node; final String adm1Context;
        ZeroSizeIssue(GeoNamesBuilderNode node, String adm1Context) { this.node = node; this.adm1Context = adm1Context; }
    }
}