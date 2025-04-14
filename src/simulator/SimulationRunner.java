package simulator;

import simulator.topology.TopologyConfiguration;
import simulator.topology.TopologyFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Abstract base class for running simulations.
 * Handles topology generation using a provided factory and configuration,
 * and defines abstract methods for executing specific simulation scenarios.
 *
 * @param <C> The specific type of TopologyConfiguration used.
 * @param <R> The specific type of the root TreeNode generated.
 * @param <F> The specific type of TopologyFactory used.
 */
public abstract class SimulationRunner<
    C extends TopologyConfiguration,
    R extends TreeNode,
    F extends TopologyFactory> {

    protected F topologyFactory;
    protected C topologyConfig;
    protected R rootNode;

    /**
     * Runs the complete simulation process: setup, topology generation, scenario execution, cleanup.
     *
     * @param factory The TopologyFactory instance to use.
     * @param config The TopologyConfiguration instance for the factory.
     */
    public final void run(F factory, C config) {
        try {
            // 1. Initialization
            initialise(factory, config);

            // 2. Generate Topology
            generateTopology();

            // 3. Setup Simulation (optional steps after topology exists)
            setupSimulation();

            // 4. Execute Simulation Scenarios
            executeScenarios();

        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5. Cleanup (optional)
            cleanup();
        }
    }

    /**
     * Initialises the runner with the factory and configuration.
     * Basic validation occurs here.
     *
     * @param factory The TopologyFactory instance.
     * @param config The TopologyConfiguration instance.
     */
    protected void initialise(F factory, C config) {
        System.out.println("--- Initialising Simulation Runner ---");
        if (factory == null) {
            throw new IllegalArgumentException("TopologyFactory cannot be null.");
        }
        if (config == null) {
            throw new IllegalArgumentException("TopologyConfiguration cannot be null.");
        }
        this.topologyFactory = factory;
        this.topologyConfig = config;
        System.out.println("Using Factory: " + factory.getClass().getSimpleName());
        System.out.println("Using Configuration: " + config.getClass().getSimpleName());
    }

    /**
     * Generates the topology using the configured factory and configuration.
     * Stores the root node.
     */
    @SuppressWarnings("unchecked") // Suppress cast warning, validated by factory contract
    protected void generateTopology() {
        System.out.println("\n--- Generating Topology ---");
        TreeNode generatedRoot = topologyFactory.generateTopology(topologyConfig);
        if (generatedRoot == null) {
            throw new IllegalStateException("Topology generation failed to produce a root node.");
        }
        try {
            // Attempt to cast to the expected root type R
            this.rootNode = (R) generatedRoot;
            System.out.println("--- Topology Generation Complete ---");
            System.out.println("Root Node: " + rootNode.getName() + " (" + rootNode.getClass().getSimpleName() + ")");
            System.out.println();
            printTopologyStructure(); // Print the generated structure
        } catch (ClassCastException e) {
            throw new IllegalStateException("Generated root node type (" + generatedRoot.getClass().getSimpleName()
                + ") does not match expected type.", e);
        }
    }

    /**
     * Optional setup phase after topology generation but before scenario execution.
     * Subclasses can override this to perform tasks like initializing metrics, etc.
     */
    protected void setupSimulation() {
        // Default: No specific setup needed.
        System.out.println("\n--- Performing Simulation Setup (Default: None) ---");
    }

    /**
     * Abstract method where subclasses implement the core simulation logic,
     * such as sending subscriptions and publications.
     * This method is called after the topology is successfully generated.
     */
    protected abstract void executeScenarios();

    /**
     * Optional cleanup phase after simulation execution or failure.
     * Subclasses can override this to release resources, print summaries, etc.
     */
    protected void cleanup() {
        // Default: No specific cleanup needed.
        System.out.println("\n--- Simulation Run Finished ---");
    }

    // --- Common Helper Methods ---

    /** Prints the generated topology structure */
    protected void printTopologyStructure() {
        if (rootNode != null) {
            System.out.println("--- Generated Topology Structure ---");
            printTree(rootNode, 0);
            System.out.println("----------------------------------");
            // Optionally print root region if applicable
            if (rootNode instanceof simulator.regions.BrokerWithRegion rootBroker) {
                 System.out.println("Final Root Region: " + rootBroker.getRegion());
            }
             System.out.println();
        } else {
            System.out.println("Cannot print topology: Root node is null.");
        }
    }

    /**
     * Prints the tree structure starting from the given node, including node level.
     * (Copied from previous examples, could be made non-static if preferred)
     */
    public static void printTree(TreeNode node, int level) {
        if (node == null) return;
        for (int i = 0; i < level; i++) System.out.print("  ");
        System.out.print("- [L" + level + "] " + node.getName() + " (" + node.getClass().getSimpleName() + ")");
        if (node instanceof simulator.regions.BrokerWithRegion broker) {
             System.out.print(" Region: " + broker.getRegion());
        } else if (node instanceof SubscriberWithLocation sub) {
             System.out.print(" Location: " + sub.getLocation());
        } else if (node instanceof PublisherWithLocation pub) {
             System.out.print(" Location: " + pub.getLocation());
        }
        System.out.println();
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                printTree(child, level + 1);
            }
        }
    }

    /** Generic helper to find a node by name and type using BFS */
    protected <T extends TreeNode> T findNodeByName(String name, Class<T> type) {
        if (rootNode == null || name == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(rootNode);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current) && name.equals(current.getName())) {
                return type.cast(current);
            }
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
        System.err.println("Warning: Node not found - Name: " + name + ", Type: " + type.getSimpleName());
        return null; // Not found
    }

     /** Finds the first node of a given type using BFS */
    protected <T extends TreeNode> T findFirstNodeOfType(Class<T> type) {
        if (rootNode == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(rootNode);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
         System.err.println("Warning: No node found of type: " + type.getSimpleName());
        return null; // Not found
    }

    /** Prints subscription tables for all brokers */
    protected void printAllBrokerSubscriptionTables() {
        if (rootNode == null) return;
        System.out.println("\n--- Broker Subscription Tables ---");
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(rootNode);
        boolean foundBroker = false;
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            // Check specifically for BrokerWithRegion as it has the print method
            if (current instanceof simulator.regions.BrokerWithRegion broker) {
                 foundBroker = true;
                 broker.printSubscriptionsTable();
            }
            // Traverse children regardless of current node type
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
        if (!foundBroker) {
            System.out.println("No brokers with subscription tables found in the topology.");
        }
         System.out.println("--------------------------------");
    }
}