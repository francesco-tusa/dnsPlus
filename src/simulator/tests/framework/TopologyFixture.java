package simulator.tests.framework;

import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.factories.SpatialMatchBrokerFactory;

public interface TopologyFixture {
    void setup(SpatialMatchBrokerFactory factory);

    BoundedBroker getRoot();

    <T extends TreeNode> T findNode(String name, Class<T> clazz);

    <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz);

    String getName();
}