package simulator.core;

import java.util.logging.Level;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger;

/**
 * Abstract base class for all simulation runners.
 * @param <C> The specific type of TopologyConfiguration required.
 * @param <R> The specific type of the root TreeNode, constrained to a BrokerWithRegion.
 * @param <F> The specific type of the TopologyFactory.
 */
public abstract class SimulationRunner<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>> {

    protected F topologyFactory;
    protected C topologyConfig;
    protected R rootNode;

    // --- Abstract Methods for Subclasses ---
    protected abstract void executeScenarios();
    
    /**
     * Specifies the desired logging level for this simulation run.
     * @return The Level (e.g., FINE for functional, INFO for performance).
     */
    protected abstract Level getLogLevel();

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
        CustomLogger.setGlobalLogLevel(getLogLevel());

        System.out.println("\n--- Initialising Simulation Runner ---");
        if (factory == null) throw new IllegalArgumentException("TopologyFactory cannot be null.");
        if (config == null) throw new IllegalArgumentException("TopologyConfiguration cannot be null.");
        this.topologyFactory = factory;
        this.topologyConfig = config;
        System.out.println("Using Factory: " + factory.getClass().getSimpleName());
        System.out.println("Using Configuration: " + config.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    protected void generateTopology() {
        System.out.println("\n--- Generating Topology ---");
        this.rootNode = topologyFactory.generateTopology(topologyConfig);
        if (this.rootNode == null) {
            throw new IllegalStateException("Topology generation failed to produce a root node.");
        }
        System.out.println("--- Topology Generation Complete ---");
        System.out.println("Root Node: " + rootNode.getName() + " (" + rootNode.getClass().getSimpleName() + ")");
        System.out.println();
    }

    protected void setupSimulation() {
        System.out.println("\n--- Performing Simulation Setup (Default: None) ---");
    }

    protected void cleanup() {
        System.out.println("\n--- Simulation Run Finished ---");
    }
}