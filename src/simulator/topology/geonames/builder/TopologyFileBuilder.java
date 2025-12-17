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
    
    private static final String TOPOLOGY_OUTPUT_DIR = "output/topologies";

    public static void main(String[] args) {
        // 1. Setup Directories & Timestamp
        File outputDir = new File(TOPOLOGY_OUTPUT_DIR);
        if (!outputDir.exists()) outputDir.mkdirs();
        
        String timestamp = ExperimentTimestamp.getTimestamp();
        
        // We attach a FileHandler to the root logger to capture logs from all strategies
        Logger rootLogger = Logger.getLogger(""); 
        java.util.logging.FileHandler fileHandler = null;
        
        try {
            // Determine strategy type early for naming
            SimConfiguration simConfig = SimConfiguration.get();
            TopologyConfig.StrategyType type = simConfig.topology.generationStrategy;
            String filePrefix = (type == TopologyConfig.StrategyType.POLITICAL) ? "political_topology" : "rtree_topology";

            // Create Log File
            String logFileName = String.format("%s_generation_%s.log", filePrefix, timestamp);
            File logFile = new File(outputDir, logFileName);
            
            fileHandler = new java.util.logging.FileHandler(logFile.getAbsolutePath());
            fileHandler.setFormatter(new utils.SimpleFileFormatter());

            if (simConfig.paths.enableVerboseLogs) {
                // If config says verbose, allow detailed logs (FINE/FINEST)
                fileHandler.setLevel(java.util.logging.Level.ALL);
                logger.info("Verbose logging ENABLED (Level: ALL)");
            } else {
                // Default: Clean output
                fileHandler.setLevel(java.util.logging.Level.INFO);
                logger.info("Verbose logging DISABLED (Level: INFO)");
            }

            rootLogger.addHandler(fileHandler);
            
            logger.info("=== TOPOLOGY GENERATION START ===");
            logger.info("Log File: " + logFile.getAbsolutePath());
            logger.info("Timestamp: " + timestamp);

            GeoNamesDataLoader loader = new GeoNamesDataLoader();
            loader.loadAll();

            TopologyBuilderStrategy strategy;
            switch (type) {
                case POLITICAL -> strategy = new PoliticalTopologyStrategy();
                case RTREE -> strategy = new RTreeTopologyStrategy();
                default -> throw new IllegalStateException("Unsupported Topology Strategy for Builder: " + type);
            }

            logger.info("Executing Strategy: " + strategy.getClass().getSimpleName());
            
            // Build the intermediate node structure
            GeoNamesBuilderNode root = strategy.build(loader);
            
            // Export Full Topology
            String jsonFileName = String.format("%s_%s.json", filePrefix, timestamp);
            File jsonFile = new File(outputDir, jsonFileName);
            exportToJson(root, jsonFile);
            
            // Generate Map Snapshot
            String mapFileName = String.format("%s_map_%s.png", filePrefix, timestamp);
            File mapFile = new File(outputDir, mapFileName);
            generateTopologyMap(root, mapFile);
            
            // Export Subset
            GeoNamesBuilderNode subset = strategy.createSubset(root);
            if (subset != null) {
                String subsetFileName = String.format("subset_%s_%s.json", filePrefix, timestamp);
                File subsetFile = new File(outputDir, subsetFileName);
                exportToJson(subset, subsetFile);
            } else {
                logger.info("Skipping subset generation.");
            }
            
            logger.info("=== TOPOLOGY GENERATION SUCCESS ===");

        } catch (Exception e) {
            logger.severe("CRITICAL FAILURE during topology generation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup Handler
            if (fileHandler != null) {
                rootLogger.removeHandler(fileHandler);
                fileHandler.close();
            }
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