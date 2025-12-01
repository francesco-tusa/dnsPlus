package simulator.topology.geonames.builder;

import java.io.File;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import simulator.config.SimConfiguration;
import simulator.config.TopologyConfig;
import utils.CustomLogger;

/**
 * The main entry point for the "Offline" phase.
 * It reads raw GeoNames data, constructs an intermediate tree structure,
 * and writes the final topology definition to a JSON file.
 */
public class TopologyFileBuilder {
    private static final Logger logger = CustomLogger.getLogger(TopologyFileBuilder.class.getName());

    public static void main(String[] args) {
        GeoNamesDataLoader loader = new GeoNamesDataLoader();
        loader.loadAll();

        SimConfiguration simConfig = SimConfiguration.get();
        TopologyConfig.StrategyType type = simConfig.topology.generationStrategy;
        TopologyBuilderStrategy strategy;

        switch (type) {
            case POLITICAL -> strategy = new PoliticalTopologyStrategy();
            case RTREE -> strategy = new RTreeTopologyStrategy();
            default -> throw new IllegalStateException("Unsupported Topology Strategy for Builder: " + type);
        }

        logger.info("Executing Strategy: " + strategy.getClass().getSimpleName());
        
        // Build the intermediate node structure
        GeoNamesBuilderNode root = strategy.build(loader);
        exportToJson(root, strategy.getOutputFilePath());
        
        // Build and export the subset (if supported)
        GeoNamesBuilderNode subset = strategy.createSubset(root);
        if (subset != null) {
            exportToJson(subset, strategy.getSubsetOutputFilePath());
        } else {
            logger.info("Skipping subset generation (not implemented for this strategy).");
        }
    }

    private static void exportToJson(GeoNamesBuilderNode root, String path) {
        try {
            File file = new File(path);
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(file, root);
            logger.info("Exported topology to: " + path);
        } catch (Exception e) {
            logger.severe("Failed to export JSON to " + path + ": " + e.getMessage());
        }
    }
}