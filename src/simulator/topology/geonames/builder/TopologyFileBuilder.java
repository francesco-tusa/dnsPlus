package simulator.topology.geonames.builder;

import java.io.File;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import simulator.config.SimConfiguration;
import simulator.config.TopologyConfig;
import simulator.topology.analysis.TopologyStatisticsCalculator; 
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;
import utils.ExperimentTimestamp;

public class TopologyFileBuilder {
    private static final Logger logger = CustomLogger.getLogger(TopologyFileBuilder.class.getName());
    
    private static final String TOPOLOGY_OUTPUT_DIR = "output/topologies";

    public static void main(String[] args) {
        File outputDir = new File(TOPOLOGY_OUTPUT_DIR);
        if (!outputDir.exists()) outputDir.mkdirs();
        
        String timestamp = ExperimentTimestamp.getTimestamp();
        Logger rootLogger = Logger.getLogger(""); 
        java.util.logging.FileHandler fileHandler = null;
        
        try {
            SimConfiguration simConfig = SimConfiguration.get();
            TopologyConfig.StrategyType type = simConfig.topology.generationStrategy;
            String filePrefix = (type == TopologyConfig.StrategyType.POLITICAL) ? "political_topology" : "rtree_topology";

            String logFileName = String.format("%s_generation_%s.log", filePrefix, timestamp);
            File logFile = new File(outputDir, logFileName);
            
            fileHandler = new java.util.logging.FileHandler(logFile.getAbsolutePath());
            fileHandler.setFormatter(new utils.SimpleFileFormatter());

            if (simConfig.paths.enableVerboseLogs) {
                fileHandler.setLevel(java.util.logging.Level.ALL);
                logger.info("Verbose logging ENABLED (Level: ALL)");
            } else {
                fileHandler.setLevel(java.util.logging.Level.INFO);
                logger.info("Verbose logging DISABLED (Level: INFO)");
            }

            rootLogger.addHandler(fileHandler);
            
            logger.info("=== TOPOLOGY GENERATION START ===");
            logger.info("Log File: " + logFile.getAbsolutePath());
            logger.info("Timestamp: " + timestamp);
            
            // [NEW] Log the detailed configuration summary
            logConfigurationSummary();

            GeoNamesDataLoader loader = new GeoNamesDataLoader();
            loader.loadAll();

            TopologyBuilderStrategy strategy;
            switch (type) {
                case POLITICAL -> strategy = new PoliticalTopologyStrategy();
                case RTREE -> strategy = new RTreeTopologyStrategy();
                default -> throw new IllegalStateException("Unsupported Topology Strategy for Builder: " + type);
            }

            logger.info("Executing Strategy: " + strategy.getClass().getSimpleName());
            
            GeoNamesBuilderNode root = strategy.build(loader);
            
            if (simConfig.topology.enableTopologyAnalysis) {
                TopologyStatisticsCalculator.logTopologyStats(root);
            } else {
                logger.info("Topology structural analysis disabled in config.");
            }
            
            String jsonFileName = String.format("%s_%s.json", filePrefix, timestamp);
            File jsonFile = new File(outputDir, jsonFileName);
            exportToJson(root, jsonFile);
            
            String mapFileName = String.format("%s_map_%s.png", filePrefix, timestamp);
            File mapFile = new File(outputDir, mapFileName);
            generateTopologyMap(root, mapFile);
            
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
            if (fileHandler != null) {
                rootLogger.removeHandler(fileHandler);
                fileHandler.close();
            }
        }
    }

    /**
     * Logs a formatted summary of the active topology configuration parameters.
     */
    private static void logConfigurationSummary() {
        TopologyConfig tConf = SimConfiguration.get().topology;
        
        logger.info("");
        logger.info("=== TOPOLOGY CONFIGURATION SUMMARY ===");
        
        // General Settings
        logger.info(String.format("   -> %-25s : %s", "Generation Strategy", tConf.generationStrategy));
        logger.info(String.format("   -> %-25s : %s", "Output Directory", TOPOLOGY_OUTPUT_DIR));
        logger.info(String.format("   -> %-25s : %d", "Base Branching Factor", tConf.branchingFactor));

        // Strategy Specifics
        if (tConf.generationStrategy == TopologyConfig.StrategyType.POLITICAL) {
            if (tConf.enablePoliticalExpansion) {
                logger.info("   --- Political Expansion (Active) ---");
                logger.info(String.format("   -> %-25s : %,d", "Coarse Threshold (Pass 8)", tConf.politicalCoarseThreshold));
                logger.info(String.format("   -> %-25s : %,d", "Leaf Capacity (Pass 9)", tConf.politicalLeafCapacity));
            } else {
                logger.info("   --- Political Expansion ---");
                logger.info(String.format("   -> %-25s : %s", "Expansion Status", "DISABLED (Flat ADM2 Leaves)"));
            }
            logger.info(String.format("   -> %-25s : %s", "Political Analysis", tConf.enablePoliticalAnalysis));
        } else if (tConf.generationStrategy == TopologyConfig.StrategyType.RTREE) {
            logger.info("   --- R-Tree Settings ---");
            logger.info(String.format("   -> %-25s : %d", "Leaf Capacity", tConf.rTreeLeafCapacity));
            logger.info(String.format("   -> %-25s : %.2f", "Max Country Width", tConf.rTreeMaxCountryWidth));
        }

        // Analysis Settings
        logger.info("   --- General Analysis ---");
        logger.info(String.format("   -> %-25s : %s", "Structural Analysis", tConf.enableTopologyAnalysis));
        logger.info("==========================================");
        logger.info("");
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
    
    private static void generateTopologyMap(GeoNamesBuilderNode root, File destinationFile) {
        if (root == null || root.children == null) return;
        logger.info("Generating Topology Map Snapshot...");
        SimulationVisualiser vis = SimulationVisualiser.getInstance();
        vis.launch();
        try {
            int retries = 0;
            while (!vis.isInitialized() && retries < 20) {
                Thread.sleep(100);
                retries++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (GeoNamesBuilderNode child : root.children) {
            if (child.bounds != null) {
                vis.updateRegion(child.name, child.bounds);
            }
        }
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        vis.saveMapImage(destinationFile);
        vis.close();
    }
}