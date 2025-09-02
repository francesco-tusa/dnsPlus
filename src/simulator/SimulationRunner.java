package simulator;

import simulator.topology.TopologyConfiguration;
import simulator.topology.TopologyFactory;
import simulator.visualisation.TopologyVisualiser;

public abstract class SimulationRunner<
    C extends TopologyConfiguration,
    R extends TreeNode,
    F extends TopologyFactory<C, R>> {

    protected F topologyFactory;
    protected C topologyConfig;
    protected R rootNode;
    protected TopologyVisualiser visualiser;

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
        System.out.println("--- Initialising Simulation Runner ---");
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
        printTopologyStructure();
    }

    protected void setupSimulation() {
        System.out.println("\n--- Performing Simulation Setup (Default: None) ---");
    }
    
    protected abstract void executeScenarios();

    protected void cleanup() {
        System.out.println("\n--- Simulation Run Finished ---");
    }

    protected void printTopologyStructure() {
        if (rootNode != null) {
            System.out.println("--- Generated Topology Structure ---");
            printTree(rootNode, 0);
            System.out.println("----------------------------------");
            if (rootNode instanceof simulator.regions.BrokerWithRegion rootBroker) {
                 System.out.println("Final Root Region: " + rootBroker.getRegion());
            }
             System.out.println();
        } else {
            System.out.println("Cannot print topology: Root node is null.");
        }
    }
    
    public static void printTree(TreeNode node, int level) {
        if (node == null) return;
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < level; i++) prefix.append("  ");
        prefix.append("- [L").append(level).append("] ").append(node.getName()).append(" (").append(node.getClass().getSimpleName()).append(")");
        
        if (node instanceof simulator.regions.BrokerWithRegion broker) {
             prefix.append(" Region: ").append(broker.getRegion());
        } else if (node instanceof SubscriberWithLocation sub) {
             prefix.append(" Location: ").append(sub.getLocation());
        } else if (node instanceof PublisherWithLocation pub) {
             prefix.append(" Location: ").append(pub.getLocation());
        }
        System.out.println(prefix.toString());
        
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                printTree(child, level + 1);
            }
        }
    }
}