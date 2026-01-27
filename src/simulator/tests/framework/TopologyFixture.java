package simulator.tests.framework;

import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.topology.factories.BrokerFactory;

public interface TopologyFixture {
    /**
     * Sets up the topology using the provided factory.
     * Implementations may validate the factory type if necessary (e.g., Grid requires BoundedBrokerFactory).
     */
    void setup(BrokerFactory factory);

    SimulationBroker getRoot();

    <T extends TreeNode> T findNode(String name, Class<T> clazz);

    <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz);

    String getName();
}