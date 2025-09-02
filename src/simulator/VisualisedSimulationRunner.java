package simulator;

import simulator.topology.TopologyConfiguration;
import simulator.topology.TopologyFactory;
import simulator.visualisation.TopologyVisualiser;

public abstract class VisualisedSimulationRunner<
    C extends TopologyConfiguration,
    R extends TreeNode,
    F extends TopologyFactory<C, R>> extends SimulationRunner<C, R, F> {

    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        this.visualiser = TopologyVisualiser.getInstance();
    }

    @Override
    protected void generateTopology() {
        super.generateTopology();
        if (this.rootNode != null) {
            visualizeTopology(this.rootNode);
        }
    }

    /**
     * Overrides the cleanup method to make the visualiser window visible
     * at the end of the simulation run.
     */
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
