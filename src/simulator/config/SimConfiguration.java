package simulator.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class SimConfiguration {
    private static final Logger logger = CustomLogger.getLogger(SimConfiguration.class.getName());
    private static SimConfiguration instance;

    // Sub-Configs
    public final PathsConfig paths;
    public final TopologyConfig topology;
    public final BrokerConfig broker;
    public final WorkloadConfig workload;

    private SimConfiguration() {
        Properties props = loadProperties();
        this.paths = new PathsConfig(props);
        this.topology = new TopologyConfig(props);
        this.broker = new BrokerConfig(props);
        this.workload = new WorkloadConfig(props);

        logger.info("Configuration Loaded Successfully.");
    }

    public static synchronized SimConfiguration get() {
        if (instance == null) {
            instance = new SimConfiguration();
        }
        return instance;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        // This path is relative to your PROJECT ROOT
        String filePath = "resources/simulation.properties";

        try (FileInputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (IOException ex) {
            logger.severe("Error loading configuration: " + ex.getMessage());   
        }
        return props;
    }
}