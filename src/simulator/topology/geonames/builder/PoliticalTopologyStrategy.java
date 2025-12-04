package simulator.topology.geonames.builder;

import java.io.BufferedReader;
import java.util.*;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.regions.Region;
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
                    // Keep Bangladesh
                    if ("BD".equals(country.code)) {
                        subsetAsia.addChild(country); 
                    }
                    // Keep China -> Beijing
                    if ("CN".equals(country.code)) {
                        GeoNamesBuilderNode subsetChina = new GeoNamesBuilderNode(country);
                        subsetAsia.addChild(subsetChina);
                        for (GeoNamesBuilderNode adm1 : country.children) {
                            if (adm1.name.contains("Beijing")) {
                                subsetChina.addChild(adm1);
                            }
                        }
                    }
                }
            }
        }
        return subsetRoot;
    }

    @Override
    public String getOutputFilePath() {
        return config.paths.fullTopologyPolitical;
    }

    @Override
    public String getSubsetOutputFilePath() {
        return config.paths.subsetTopology;
    }

    @Override
    public GeoNamesBuilderNode build(GeoNamesDataLoader loader) {
        logger.info("Executing Political Topology Strategy...");
        
        processPass1(loader);
        
        // Pass 2: Build Hierarchy
        GeoNamesBuilderNode root = buildInitialHierarchy(loader);
        
        // Pass 3-7: Bounds & Scaling
        List<GeoNamesBuilderNode> nodesToExpand = new ArrayList<>();
        assignInitialProps(root, nodesToExpand);
        addAdm2Layer(nodesToExpand);
        distributeAdm1Pop(nodesToExpand);
        estimateAdm2Bounds(nodesToExpand);
        
        logger.info("Pass 7: Scaling populations...");
        scalePopulations(root, loader);
        
        // Retrieve parameters from TopologyConfig
        int maxBranching = config.topology.branchingFactor;
        long thresholdCoarse = config.topology.politicalThresholdLvl1; // Default 1,000,000
        long thresholdFine = config.topology.politicalThresholdLvl2;   // Default 10,000

        // Pass 8: Coarse Expansion
        // Uses "Bin Packing" logic (distributeEqually=false) to create large semantic districts.
        // Node Type: GRID_COARSE
        LeafExpansionStrategy gridPass1 = new GridLeafExpansionStrategy(false, maxBranching);
        
        logger.info(String.format("Pass 8: Expanding Leaves > %d (Branching=%s)...", 
                thresholdCoarse, (maxBranching > 0 ? maxBranching : "Flat")));
        
        expandLeaves(root, thresholdCoarse, gridPass1, GeoNamesBuilderNode.NodeType.GRID_COARSE);
        
        // Pass 9: Fine Expansion
        // Uses "Load Balancing" logic (distributeEqually=true) to create recursive routing trees.
        // Node Type: GRID_FINE
        LeafExpansionStrategy gridPass2 = new GridLeafExpansionStrategy(true, maxBranching);
        
        logger.info(String.format("Pass 9: Expanding Leaves > %d (Branching=%s)...", 
                thresholdFine, (maxBranching > 0 ? maxBranching : "Flat")));
        
        expandLeaves(root, thresholdFine, gridPass2, GeoNamesBuilderNode.NodeType.GRID_FINE);
        
        // Pass 10: Final Aggregation
        logger.info("Pass 10: Final Aggregation...");
        finalAggregate(root);
        
        // Diagnostics
        printTopologyStats(root);
        
        return root;
    }

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
            // ADM1s larger than the Coarse Threshold are candidates for expansion
            if (node.aggregatedPopulation > config.topology.politicalThresholdLvl1) expandList.add(node);
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

    private void scalePopulations(GeoNamesBuilderNode root, GeoNamesDataLoader loader) {
        for (GeoNamesBuilderNode continent : root.children) {
            for (GeoNamesBuilderNode country : continent.children) {
                if (country.type != GeoNamesBuilderNode.NodeType.COUNTRY) continue;
                
                Long official = country.officialPopulation != null ? country.officialPopulation : 0L;
                Double rate = loader.countryIsoToPenetrationMap.get(country.code);
                if (rate == null) rate = 0.0;
                country.internetPenetrationRate = rate;
                
                // Trust children sum
                long currentTotal = sumSubtreePopulation(country);
                
                double scaleFactor = 1.0;
                if (official > 0 && currentTotal > 0) {
                    scaleFactor = (double) official / currentTotal;
                }
                
                applyScaling(country, scaleFactor, rate);
            }
        }
    }
    
    private long sumSubtreePopulation(GeoNamesBuilderNode node) {
        if (node == null) return 0;
        if (node.children.isEmpty()) return node.aggregatedPopulation;
        long sum = 0;
        for (GeoNamesBuilderNode child : node.children) {
            sum += sumSubtreePopulation(child);
        }
        return sum;
    }

    private void applyScaling(GeoNamesBuilderNode node, double scale, double rate) {
        node.aggregatedPopulation = Math.max(0, Math.round(node.aggregatedPopulation * scale));
        node.internetPopulation = Math.min(node.aggregatedPopulation, 
            Math.max(0, Math.round(node.aggregatedPopulation * rate)));
            
        for (GeoNamesBuilderNode child : node.children) {
            applyScaling(child, scale, rate);
        }
    }

    private void expandLeaves(GeoNamesBuilderNode node, long threshold, LeafExpansionStrategy strategy, GeoNamesBuilderNode.NodeType type) {
        if (node.children.isEmpty()) {
            strategy.expand(node, threshold, type);
            return;
        }
        List<GeoNamesBuilderNode> childrenCopy = new ArrayList<>(node.children);
        for (GeoNamesBuilderNode child : childrenCopy) {
            expandLeaves(child, threshold, strategy, type);
        }
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
        if (bounds.getBottomLeft() != null) node.bounds = bounds;
    }

    private void printTopologyStats(GeoNamesBuilderNode root) {
        Map<GeoNamesBuilderNode.NodeType, Long> typeCounts = new HashMap<>();
        Map<GeoNamesBuilderNode.NodeType, Long> typePop = new HashMap<>();
        Map<GeoNamesBuilderNode.NodeType, Long> typeNetPop = new HashMap<>();

        Queue<GeoNamesBuilderNode> queue = new LinkedList<>();
        queue.add(root);
        
        while (!queue.isEmpty()) {
            GeoNamesBuilderNode node = queue.poll();
            typeCounts.merge(node.type, 1L, Long::sum);
            typePop.merge(node.type, node.aggregatedPopulation, Long::sum);
            typeNetPop.merge(node.type, node.internetPopulation, Long::sum);
            
            if (node.children != null) queue.addAll(node.children);
        }

        logger.info("\n=== TOPOLOGY DIAGNOSTICS ===");
        logger.info(String.format("%-12s | %-10s | %-15s | %-15s", "TYPE", "COUNT", "TOTAL POP", "INTERNET POP"));
        logger.info("------------------------------------------------------------");
        for (GeoNamesBuilderNode.NodeType type : GeoNamesBuilderNode.NodeType.values()) {
            logger.info(String.format("%-12s | %-10d | %-15d | %-15d", 
                type, 
                typeCounts.getOrDefault(type, 0L), 
                typePop.getOrDefault(type, 0L),
                typeNetPop.getOrDefault(type, 0L)
            ));
        }
        logger.info("============================\n");
    }
}