package simulator.core;

import java.util.logging.Logger;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.visualisation.TopologyVisualiser;
import utils.CustomLogger;

public abstract class VisualisedSimulationRunner<
    C extends TopologyConfiguration,
    R extends BoundedBroker,
    F extends AbstractTopologyFactory<C, R>> extends SimulationRunner<C, R, F> {

    private static final Logger logger = CustomLogger.getLogger(VisualisedSimulationRunner.class.getName());
    protected TopologyVisualiser visualiser;

    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        this.visualiser = TopologyVisualiser.getInstance();
    }

    @Override
    protected void setupSimulation() {
        super.setupSimulation();
        attachClientsAndVisualize();
    }

    protected void attachClientsAndVisualize() {
        if (this.rootNode != null) {
            visualizeTopology(this.rootNode);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (visualiser != null) {
            logger.info("\n--- Displaying Final Visualisation ---");
            logger.info("--- Right-click the window to toggle auto-layout and enable node dragging. ---");
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
