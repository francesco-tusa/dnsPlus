package simulator.topology.geonames.builder;

import java.io.File;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import utils.CustomLogger;

public class GeoNamesTopologyBuilder {
    private static final Logger logger = CustomLogger.getLogger(GeoNamesTopologyBuilder.class.getName());

    public static void main(String[] args) {
        String resourcesDir = "resources/world";
        String outputDir = "output";
        String geonamesFile = resourcesDir + "/allCountries.txt";

        // 1. Load Data
        GeoNamesDataLoader loader = new GeoNamesDataLoader();
        loader.loadAll(resourcesDir);

        // 2. Execute Strategy
        TopologyBuilderStrategy strategy = new PoliticalTopologyStrategy();
        logger.info("Executing Strategy: " + strategy.getClass().getSimpleName());
        
        GeoNamesBuilderNode root = strategy.build(loader, geonamesFile);

        // 3. Export Full (Using Strategy's filename)
        String fullTopologyPath = outputDir + "/" + strategy.getOutputFileName();
        exportToJson(root, fullTopologyPath);
        
        // 4. Export Subset (Specific to Political/Testing)
        // We keep the fixed name for the subset to maintain compatibility with the functional test config
        if (strategy instanceof PoliticalTopologyStrategy) {
            GeoNamesBuilderNode subset = createSubset(root);
            exportToJson(subset, outputDir + "/geonames_subset_bangladesh_beijing.json");
        }
    }

    private static void exportToJson(GeoNamesBuilderNode root, String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(new File(path), root);
            logger.info("Exported topology to: " + path);
        } catch (Exception e) {
            logger.severe("Failed to export JSON: " + e.getMessage());
        }
    }
    
    private static GeoNamesBuilderNode createSubset(GeoNamesBuilderNode root) {
        // Clone root
        GeoNamesBuilderNode subsetRoot = new GeoNamesBuilderNode(root);
        
        // Find Asia
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
}