package simulator.topology.geonames.builder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import simulator.topology.TopologyPaths;
import utils.CustomLogger;

public class RTreeTopologyStrategy implements TopologyBuilderStrategy {
    private static final Logger logger = CustomLogger.getLogger(RTreeTopologyStrategy.class.getName());

    private static final int DEFAULT_MAX_CHILDREN = 20;
    private final int maxChildren;

    public RTreeTopologyStrategy() {
        this(DEFAULT_MAX_CHILDREN);
    }

    public RTreeTopologyStrategy(int maxChildren) {
        if (maxChildren < 2) throw new IllegalArgumentException("Branching factor must be >= 2");
        this.maxChildren = maxChildren;
    }

    @Override
    public String getOutputFilePath() {
        return TopologyPaths.FULL_TOPOLOGY_RTREE;
    }
    
    @Override
    public String getSubsetOutputFilePath() {
        return TopologyPaths.SUBSET_TOPOLOGY_RTREE;
    }

    @Override
    public GeoNamesBuilderNode build(GeoNamesDataLoader loader) {
        logger.info("Executing R-Tree Topology Strategy (STR Bulk Loading, M=" + maxChildren + ")...");

        List<GeoNamesEntry> allPpls = loadAllPpls(TopologyPaths.ALL_COUNTRIES_FILE);
        logger.info("Loaded " + allPpls.size() + " Populated Places.");

        Map<String, List<GeoNamesEntry>> pplsByCountry = allPpls.stream()
            .collect(Collectors.groupingBy(e -> e.countryCode));

        List<GeoNamesBuilderNode> countryNodes = new ArrayList<>();

        for (String isoCode : pplsByCountry.keySet()) {
            List<GeoNamesEntry> points = pplsByCountry.get(isoCode);
            if (points.isEmpty()) continue;

            // 1. Build Internal Tree
            GeoNamesBuilderNode countryRoot = STRTreeBuilder.build(points, maxChildren);
            
            countryRoot.name = loader.countryCodeToNameMap.getOrDefault(isoCode, isoCode);
            countryRoot.type = GeoNamesBuilderNode.NodeType.COUNTRY;
            countryRoot.code = isoCode;
            countryRoot.featureCode = "PCLI";
            
            // 2. Scale Population
            Long officialPop = loader.countryCodeToPopulationMap.get(isoCode);
            if (officialPop != null && countryRoot.aggregatedPopulation > 0) {
                scalePopulation(countryRoot, officialPop);
                countryRoot.officialPopulation = officialPop;
            }

            // 3. Apply Internet Penetration
            Double penetration = loader.countryIsoToPenetrationMap.get(isoCode);
            if (penetration == null) penetration = 0.0;
            
            applyPenetrationRate(countryRoot, penetration);

            countryNodes.add(countryRoot);
        }

        logger.info("Building Global Index from " + countryNodes.size() + " countries...");
        GeoNamesBuilderNode worldRoot = STRTreeBuilder.buildFromNodes(countryNodes, maxChildren);
        
        worldRoot.name = "World";
        worldRoot.type = GeoNamesBuilderNode.NodeType.WORLD;
        worldRoot.code = "WORLD";
        
        logger.info("World Aggregated Pop: " + worldRoot.aggregatedPopulation);
        logger.info("World Internet Pop: " + worldRoot.internetPopulation);

        return worldRoot;
    }
    
    @Override
    public GeoNamesBuilderNode createSubset(GeoNamesBuilderNode root) {
        logger.warning("Subset generation not yet implemented for R-Tree strategy.");
        return null;
    }
    
    private void scalePopulation(GeoNamesBuilderNode node, long targetPop) {
        long currentTotal = node.aggregatedPopulation;
        if (currentTotal <= 0) return;
        double scaleFactor = (double) targetPop / currentTotal;
        applyRecursiveScaling(node, scaleFactor);
    }

    private void applyRecursiveScaling(GeoNamesBuilderNode node, double scale) {
        node.aggregatedPopulation = Math.max(0, Math.round(node.aggregatedPopulation * scale));
        for (GeoNamesBuilderNode child : node.children) applyRecursiveScaling(child, scale);
    }

    private List<GeoNamesEntry> loadAllPpls(String filePath) {
        List<GeoNamesEntry> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 15) continue; 
                String fcl = parts[6];
                if (!"P".equals(fcl)) continue; 
                GeoNamesEntry entry = new GeoNamesEntry(parts);
                if (entry.geonameId != -1 && entry.population > 0) list.add(entry);
            }
        } catch (Exception e) { logger.severe("Error loading PPLs: " + e.getMessage()); }
        return list;
    }

    private void applyPenetrationRate(GeoNamesBuilderNode node, double rate) {
        node.internetPenetrationRate = rate;
        if (node.aggregatedPopulation > 0) {
            node.internetPopulation = (long) (node.aggregatedPopulation * rate);
        }
        for (GeoNamesBuilderNode child : node.children) {
            applyPenetrationRate(child, rate);
        }
    }
}