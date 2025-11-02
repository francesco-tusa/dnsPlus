package simulator.core;

import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger;
import utils.ExperimentTimestamp; // Import the timestamp utility

/**
 * Abstract base class for all simulation runners.
 * Now includes a setter to override the default log level
 * and a timestamp for unique run identification.
 */
public abstract class SimulationRunner<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>> {

    private static final Logger logger = CustomLogger.getLogger(SimulationRunner.class.getName());

    protected F topologyFactory;
    protected C topologyConfig;
    protected R rootNode;
    
    private Level overrideLogLevel = null;
    protected String simulationTimestamp; // For unique filenames

    protected abstract void executeScenarios();
    protected abstract Level getLogLevel(); // This remains for the default level

    /**
     * Public setter to allow overriding the log level before running.
     * @param level The new global log level (e.g., Level.FINE for verbose).
     */
    public void setLogLevel(Level level) {
        this.overrideLogLevel = level;
    }

    public final void run(F factory, C config) {
        try {
            initialise(factory, config);
            generateTopology();
            setupSimulation();
            executeScenarios();
        } catch (Exception e) {
            logger.severe("Simulation failed: " + e.getMessage());
            logger.log(Level.SEVERE, "Exception stack trace:", e);
        } finally {
            cleanup();
        }
    }

    protected void initialise(F factory, C config) {
        this.simulationTimestamp = ExperimentTimestamp.getTimestamp();
        
        Level levelToUse = (this.overrideLogLevel != null) ? this.overrideLogLevel : getLogLevel();
        
        CustomLogger.setGlobalLogLevel(levelToUse, this.simulationTimestamp);
        
        logger.info("\n--- Initialising Simulation Runner (Run ID: " + this.simulationTimestamp + ") ---");
        if (factory == null) throw new IllegalArgumentException("TopologyFactory cannot be null.");
        if (config == null) throw new IllegalArgumentException("TopologyConfiguration cannot be null.");
        this.topologyFactory = factory;
        this.topologyConfig = config;
        logger.info("Using Factory: " + factory.getClass().getSimpleName());
        logger.info("Using Configuration: " + config.getClass().getSimpleName());
    }

    protected void generateTopology() {
        logger.info("\n--- Generating Topology ---");
        this.rootNode = topologyFactory.generateTopology(topologyConfig);
        if (this.rootNode == null) {
            throw new IllegalStateException("Topology generation failed to produce a root node.");
        }
        logger.info("--- Topology Generation Complete ---");
        logger.info("Root Node: " + rootNode.getName() + " (" + rootNode.getClass().getSimpleName() + ")");
    }

    protected void setupSimulation() {
        logger.info("\n--- Performing Simulation Setup (Default: None) ---");
    }

    protected void cleanup() {
        logger.info("\n--- Simulation Run Finished (Run ID: " + this.simulationTimestamp + ") ---");
    }
}