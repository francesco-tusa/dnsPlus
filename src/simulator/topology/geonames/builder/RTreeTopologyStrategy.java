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
    private final double maxCountryWidth;

    public RTreeTopologyStrategy() {
        this.branchFactor = config.topology.branchingFactor;
        this.leafCapacity = config.topology.rTreeLeafCapacity;
        this.maxCountryWidth = config.topology.rTreeMaxCountryWidth;
        
        if (this.branchFactor < 2) throw new IllegalArgumentException("RTree Branching factor must be >= 2");
        if (this.leafCapacity < 1) throw new IllegalArgumentException("RTree Leaf Capacity must be >= 1");
    }

    @Override
    public GeoNamesBuilderNode build(GeoNamesDataLoader loader) {
        logger.info(String.format("Executing Hybrid R-Tree Strategy (Branch=%d, LeafCap=%d, SplitWidth=%.1f)...", 
                branchFactor, leafCapacity, maxCountryWidth));

        List<GeoNamesEntry> allPpls = loadAllPpls(config.paths.allCountriesFile);
        logger.info("Loaded " + allPpls.size() + " Populated Places.");

        Map<String, List<GeoNamesEntry>> pplsByCountry = allPpls.stream()
            .collect(Collectors.groupingBy(e -> e.countryCode));

        List<GeoNamesBuilderNode> globalNodes = new ArrayList<>();
        int splitCount = 0;

        for (String isoCode : pplsByCountry.keySet()) {
            List<GeoNamesEntry> points = pplsByCountry.get(isoCode);
            if (points.isEmpty()) continue;

            // 1. Build the local R-Tree for the country
            // The root is temporarily typed as ADM1 so we can potentially use it as a container
            GeoNamesBuilderNode countryRoot = STRBulkLoader.build(points, branchFactor, leafCapacity, NodeType.ADM1);
            if (countryRoot == null) continue;

            // 2. Apply Country Metadata & Stats
            String countryName = loader.countryCodeToNameMap.getOrDefault(isoCode, isoCode);
            countryRoot.name = countryName;
            countryRoot.type = NodeType.COUNTRY;
            countryRoot.code = isoCode;
            countryRoot.featureCode = "PCLI";
            
            Long officialPop = loader.countryCodeToPopulationMap.get(isoCode);
            if (officialPop != null && countryRoot.aggregatedPopulation > 0) {
                scalePopulation(countryRoot, officialPop);
                countryRoot.officialPopulation = officialPop;
            }
            Double penetration = loader.countryIsoToPenetrationMap.get(isoCode);
            if (penetration == null) penetration = 0.0;
            applyPenetrationRate(countryRoot, penetration);

            // 3. Check for "Approximation Error" (Massive Bounding Box)
            // If the country is too wide, we "explode" it into its constituent clusters.
            double width = countryRoot.bounds.getWidth();
            
            if (width > maxCountryWidth && !countryRoot.children.isEmpty()) {
                splitCount++;
                int part = 1;
                // Add the *children* (Leaf Clusters or ADM1s) to the global list instead of the Country node
                for (GeoNamesBuilderNode child : countryRoot.children) {
                    // We tag these parts with the Country Code so they are still identified as "part of Country X"
                    // but they will be spatially indexed independently.
                    child.name = countryName + " (Part " + part++ + ")";
                    // We keep them as 'COUNTRY' type to preserve logic that looks for countries,
                    // or we could use 'ADM1' if we want them strictly hierarchical. 
                    // Using 'COUNTRY' ensures they are treated as major regions.
                    child.type = NodeType.COUNTRY; 
                    child.code = isoCode; 
                    child.officialPopulation = 0L; // Avoid double counting official stats
                    
                    globalNodes.add(child);
                }
            } else {
                // Country is compact enough; keep it as a single node
                globalNodes.add(countryRoot);
            }
        }

        logger.info("Split " + splitCount + " wide countries into smaller spatial clusters to reduce overlap.");
        logger.info("Building Global Index from " + globalNodes.size() + " spatial nodes...");

        // 4. Build Global Super-Structure
        GeoNamesBuilderNode worldRoot = STRBulkLoader.buildFromNodes(globalNodes, branchFactor, NodeType.CONTINENT);
        
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