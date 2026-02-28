package simulator.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;
import utils.SimulationRandom;

public class SimConfiguration {
    private static final Logger logger = CustomLogger.getLogger(SimConfiguration.class.getName());
    
    // The Singleton Instance
    private static SimConfiguration instance;
    
    // Storage for batch overrides
    private static Properties programmedOverrides = null;

    public final long simulationSeed;
    
    public final PathsConfig paths;
    public final TopologyConfig topology;
    public final BrokerConfig broker;
    public final WorkloadConfig workload;

    /**
     * Resets the configuration, applies overrides, and immediately re-initializes 
     * the system (including the Random Seed).
     */
    public static synchronized void resetAndOverride(Properties overrides) {
        programmedOverrides = overrides;
        instance = new SimConfiguration();
    }

    public static synchronized SimConfiguration get() {
        if (instance == null) {
            instance = new SimConfiguration();
        }
        return instance;
    }

    private SimConfiguration() {
        Properties props = loadProperties();

        // 1. Apply Overrides
        if (programmedOverrides != null) {
            props.putAll(programmedOverrides);
        }

        // 2. Load Seed
        String seedStr = props.getProperty("simulation.seed");
        if (seedStr != null && !seedStr.isEmpty()) {
            this.simulationSeed = Long.parseLong(seedStr);
        } else {
            this.simulationSeed = System.currentTimeMillis();
            logger.warning("No 'simulation.seed' found. Using System.currentTimeMillis(): " + this.simulationSeed);
        }

        // 3. Initialize Randomness
        SimulationRandom.init(this.simulationSeed);

        // 4. Load other configs
        this.paths = new PathsConfig(props);
        this.topology = new TopologyConfig(props);
        this.broker = new BrokerConfig(props);
        this.workload = new WorkloadConfig(props);
        
        logger.info("Configuration Loaded (Eager). Seed: " + this.simulationSeed);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        String filePath = "resources/simulation.properties";
        try (FileInputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (IOException ex) {
            logger.severe("Error loading configuration: " + ex.getMessage());
        }
        return props;
    }
}