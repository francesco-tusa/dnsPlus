package simulator.topology.geonames.builder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.config.SimConfiguration;
import simulator.topology.geonames.builder.GeoNamesBuilderNode.NodeType;
import utils.CustomLogger;

public class RTreeTopologyStrategy implements TopologyBuilderStrategy {
    private static final Logger logger = CustomLogger.getLogger(RTreeTopologyStrategy.class.getName());

    private final SimConfiguration config = SimConfiguration.get();
    private final int branchFactor;
    private final int leafCapacity;

    public RTreeTopologyStrategy() {
        this.branchFactor = config.topology.rTreeBranchingFactor;
        this.leafCapacity = config.topology.rTreeLeafCapacity;
        
        if (this.branchFactor < 2) throw new IllegalArgumentException("RTree Branching factor must be >= 2");
        if (this.leafCapacity < 1) throw new IllegalArgumentException("RTree Leaf Capacity must be >= 1");
    }

    @Override
    public String getOutputFilePath() { return config.paths.fullTopologyRTree; }
    
    @Override
    public String getSubsetOutputFilePath() { return config.paths.subsetTopology; }

    @Override
    public GeoNamesBuilderNode build(GeoNamesDataLoader loader) {
        logger.info(String.format("Executing Hybrid R-Tree Strategy (Branching=%d, LeafCap=%d)...", branchFactor, leafCapacity));

        List<GeoNamesEntry> allPpls = loadAllPpls(config.paths.allCountriesFile);
        logger.info("Loaded " + allPpls.size() + " Populated Places.");

        Map<String, List<GeoNamesEntry>> pplsByCountry = allPpls.stream()
            .collect(Collectors.groupingBy(e -> e.countryCode));

        List<GeoNamesBuilderNode> countryNodes = new ArrayList<>();

        // --- PHASE 1: Build Internal Country Structures (R-Tree Below Country) ---
        for (String isoCode : pplsByCountry.keySet()) {
            List<GeoNamesEntry> points = pplsByCountry.get(isoCode);
            if (points.isEmpty()) continue;

            // 1a. Cluster points into Leaf Brokers (ADM2) and aggregate into States (ADM1)
            GeoNamesBuilderNode countryRoot = STRBulkLoader.build(points, branchFactor, leafCapacity, NodeType.ADM1);
            
            if (countryRoot == null) continue;

            // 1b. Identity Preservation: Ensure this node acts as the COUNTRY
            countryRoot.name = loader.countryCodeToNameMap.getOrDefault(isoCode, isoCode);
            countryRoot.type = NodeType.COUNTRY; // STRICTLY set as COUNTRY
            countryRoot.code = isoCode;
            countryRoot.featureCode = "PCLI";
            
            // 1c. Data Injection: Scale Population & Apply Penetration
            Long officialPop = loader.countryCodeToPopulationMap.get(isoCode);
            if (officialPop != null && countryRoot.aggregatedPopulation > 0) {
                scalePopulation(countryRoot, officialPop);
                countryRoot.officialPopulation = officialPop;
            }

            Double penetration = loader.countryIsoToPenetrationMap.get(isoCode);
            if (penetration == null) penetration = 0.0;
            applyPenetrationRate(countryRoot, penetration);

            countryNodes.add(countryRoot);
        }

        logger.info("Built " + countryNodes.size() + " country-level R-Trees. Now aggregating globally...");

        // --- PHASE 2: Build Global Super-Structure (R-Tree Above Country) ---
        // We pack the COUNTRY nodes into SPATIAL CONTINENTS to minimize overlap.
        
        GeoNamesBuilderNode worldRoot = STRBulkLoader.buildFromNodes(countryNodes, branchFactor, NodeType.CONTINENT);
        
        if (worldRoot == null) {
             worldRoot = new GeoNamesBuilderNode(0, "World", NodeType.WORLD, "WORLD", "");
        } else {
             worldRoot.name = "World";
             worldRoot.type = NodeType.WORLD;
             worldRoot.code = "WORLD";
        }

        logger.info("World Aggregated Pop: " + worldRoot.aggregatedPopulation);
        logger.info("World Internet Pop: " + worldRoot.internetPopulation);

        return worldRoot;
    }
    
    // ... [Helper methods createSubset, scalePopulation, loadAllPpls, applyPenetrationRate remain unchanged] ...
    
    @Override
    public GeoNamesBuilderNode createSubset(GeoNamesBuilderNode root) { return null; }
    
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