package simulator.core;

import java.util.logging.Level;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger;

/**
 * Abstract base class for all simulation runners.
 * Now includes a setter to override the default log level.
 */
public abstract class SimulationRunner<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>> {

    protected F topologyFactory;
    protected C topologyConfig;
    protected R rootNode;
    
    // New field to hold a log level set via the setter
    private Level overrideLogLevel = null;

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
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    protected void initialise(F factory, C config) {
        // Use the override level if it was set, otherwise use the default.
        Level levelToUse = (this.overrideLogLevel != null) ? this.overrideLogLevel : getLogLevel();
        CustomLogger.setGlobalLogLevel(levelToUse);
        
        System.out.println("\n--- Initialising Simulation Runner ---");
        if (factory == null) throw new IllegalArgumentException("TopologyFactory cannot be null.");
        if (config == null) throw new IllegalArgumentException("TopologyConfiguration cannot be null.");
        this.topologyFactory = factory;
        this.topologyConfig = config;
        System.out.println("Using Factory: " + factory.getClass().getSimpleName());
        System.out.println("Using Configuration: " + config.getClass().getSimpleName());
    }

    protected void generateTopology() {
        System.out.println("\n--- Generating Topology ---");
        this.rootNode = topologyFactory.generateTopology(topologyConfig);
        if (this.rootNode == null) {
            throw new IllegalStateException("Topology generation failed to produce a root node.");
        }
        System.out.println("--- Topology Generation Complete ---");
        System.out.println("Root Node: " + rootNode.getName() + " (" + rootNode.getClass().getSimpleName() + ")");
    }

    protected void setupSimulation() {
        System.out.println("\n--- Performing Simulation Setup (Default: None) ---");
    }

    protected void cleanup() {
        System.out.println("\n--- Simulation Run Finished ---");
    }
}