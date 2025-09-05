package simulator.core;

import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.visualisation.TopologyVisualiser;

public abstract class VisualisedSimulationRunner<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>> extends SimulationRunner<C, R, F> {

    protected TopologyVisualiser visualiser;

    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        this.visualiser = TopologyVisualiser.getInstance();
    }

    /**
     * Overrides the setup method to call a new, dedicated method for attaching clients
     * and then visualizing the topology. This ensures the correct order of operations.
     */
    @Override
    protected void setupSimulation() {
        super.setupSimulation();
        attachClientsAndVisualize();
    }

    /**
     * This method is designed to be overridden by subclasses. The base implementation
     * handles the visualization. Subclasses should call this base method *after*
     * they have attached their clients.
     */
    protected void attachClientsAndVisualize() {
        if (this.rootNode != null) {
            visualizeTopology(this.rootNode);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (visualiser != null) {
            System.out.println("\n--- Displaying Final Visualisation ---");
            visualiser.displayGraph();
        }
    }

    private void visualizeTopology(TreeNode node) {
        if (node == null || visualiser == null) return;
        visualiser.addNode(node);
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                visualiser.addNode(child);
                visualiser.addEdge(node.getName(), child.getName());
                visualizeTopology(child);
            }
        }
    }
}