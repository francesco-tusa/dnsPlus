package simulator.topology.geonames.builder;

import java.io.File;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import simulator.config.SimConfiguration;
import simulator.config.TopologyConfig;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;
import utils.ExperimentTimestamp;

/**
 * The main entry point for the "Offline" phase.
 * It reads raw GeoNames data, constructs an intermediate tree structure,
 * and writes the final topology definition to a JSON file.
 */
public class TopologyFileBuilder {
    private static final Logger logger = CustomLogger.getLogger(TopologyFileBuilder.class.getName());
    
    // Hardcoded output directory as per requirement
    private static final String TOPOLOGY_OUTPUT_DIR = "output/topologies";

    public static void main(String[] args) {
        GeoNamesDataLoader loader = new GeoNamesDataLoader();
        loader.loadAll();

        SimConfiguration simConfig = SimConfiguration.get();
        TopologyConfig.StrategyType type = simConfig.topology.generationStrategy;
        TopologyBuilderStrategy strategy;

        // Determine prefix based on strategy for clear naming
        String filePrefix;

        switch (type) {
            case POLITICAL -> {
                strategy = new PoliticalTopologyStrategy();
                filePrefix = "political_topology";
            }
            case RTREE -> {
                strategy = new RTreeTopologyStrategy();
                filePrefix = "rtree_topology";
            }
            default -> throw new IllegalStateException("Unsupported Topology Strategy for Builder: " + type);
        }

        logger.info("Executing Strategy: " + strategy.getClass().getSimpleName());
        
        // 1. Generate Timestamp
        String timestamp = ExperimentTimestamp.getTimestamp();
        logger.info("Topology Generation Timestamp: " + timestamp);
        
        // Build the intermediate node structure
        GeoNamesBuilderNode root = strategy.build(loader);
        
        // 2. Export Full Topology to output/topologies/
        String jsonFileName = String.format("%s_%s.json", filePrefix, timestamp);
        File jsonFile = new File(TOPOLOGY_OUTPUT_DIR, jsonFileName);
        
        exportToJson(root, jsonFile);
        
        // 3. Generate Map Snapshot in the same folder with descriptive name
        String mapFileName = String.format("%s_map_%s.png", filePrefix, timestamp);
        File mapFile = new File(TOPOLOGY_OUTPUT_DIR, mapFileName);
        
        generateTopologyMap(root, mapFile);
        
        // Build and export the subset (if supported)
        // We keep the subset logic relative to the main output for consistency
        GeoNamesBuilderNode subset = strategy.createSubset(root);
        if (subset != null) {
            String subsetFileName = String.format("subset_%s_%s.json", filePrefix, timestamp);
            File subsetFile = new File(TOPOLOGY_OUTPUT_DIR, subsetFileName);
            exportToJson(subset, subsetFile);
        } else {
            logger.info("Skipping subset generation (not implemented for this strategy).");
        }
    }

    private static void exportToJson(GeoNamesBuilderNode root, File file) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(file, root);
            logger.info("Exported topology to: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to export JSON to " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }
    
    /**
     * Initializes the UI, renders the Level 1 regions, and saves the image to the specific file.
     */
    private static void generateTopologyMap(GeoNamesBuilderNode root, File destinationFile) {
        if (root == null || root.children == null) return;
        
        logger.info("Generating Topology Map Snapshot...");
        SimulationVisualiser vis = SimulationVisualiser.getInstance();
        vis.launch();
        
        // Wait for UI to initialize
        try {
            int retries = 0;
            while (!vis.isInitialized() && retries < 20) {
                Thread.sleep(100);
                retries++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Populate the visualiser with Level 1 children (Continents/Major Regions)
        for (GeoNamesBuilderNode child : root.children) {
            if (child.bounds != null) {
                vis.updateRegion(child.name, child.bounds);
            }
        }
        
        // Allow a brief moment for repaint before saving
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        vis.saveMapImage(destinationFile);
        vis.close();
    }
}