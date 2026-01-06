package simulator.tests.fixtures;

import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.fixed.SimpleFixedTopologyConfiguration;
import simulator.topology.fixed.SimpleFixedTopologyGenerator;

public class FixedTopologyFixture implements TopologyFixture {

    private SimulationBroker root;
    private final SimpleFixedTopologyConfiguration config;

    public FixedTopologyFixture() {
        this.config = new SimpleFixedTopologyConfiguration();
    }

    @Override
    public void setup(BrokerFactory factory) {
        SimpleFixedTopologyGenerator generator = new SimpleFixedTopologyGenerator(factory);
        this.root = generator.generateTopology(this.config);
        
        generator.attachSubscribers(this.root);
        generator.attachPublishers(this.root);
    }

    @Override
    public SimulationBroker getRoot() {
        return root;
    }

    @Override
    public String getName() {
        return "Simple Fixed Topology (Polymorphic)";
    }
    
    @Override
    public <T extends TreeNode> T findNode(String name, Class<T> clazz) {
        return findNodeRecursive(root, name, clazz);
    }
    
    @Override
    public <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz) {
         return findNodeContainsRecursive(root, partialName, clazz);
    }

    private <T extends TreeNode> T findNodeRecursive(TreeNode node, String name, Class<T> clazz) {
        if (node == null) return null;
        if (node.getName().equals(name) && clazz.isInstance(node)) {
            return clazz.cast(node);
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                T result = findNodeRecursive(child, name, clazz);
                if (result != null) return result;
            }
        }
        return null;
    }

    private <T extends TreeNode> T findNodeContainsRecursive(TreeNode node, String partialName, Class<T> clazz) {
        if (node == null) return null;
        if (node.getName().contains(partialName) && clazz.isInstance(node)) {
            return clazz.cast(node);
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                T result = findNodeContainsRecursive(child, partialName, clazz);
                if (result != null) return result;
            }
        }
        return null;
    }
}