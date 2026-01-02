package simulator.tests.fixtures;

import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;

public class FixedTopologyFixture implements TopologyFixture {
    private BoundedBroker root; 

    @Override
    public void setup(SpatialMatchBrokerFactory factory) {
        FixedTestTopologyGenerator generator = new FixedTestTopologyGenerator(factory);
        
        // 1. Build Brokers
        this.root = generator.generateTopology(new FixedTestTopologyConfiguration());
        
        // 2. Attach Clients
        generator.attachSubscribers(this.root);
        generator.attachPublishers(this.root);
    }

    @Override
    public BoundedBroker getRoot() { 
        return this.root; 
    }

    @Override
    public <T extends TreeNode> T findNode(String name, Class<T> clazz) {
        return TopologyAnalyser.findNodeByName(this.root, name, clazz);
    }
    
    @Override
    public <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz) {
        return TopologyAnalyser.findNodeByNameContains(this.root, partialName, clazz);
    }

    @Override
    public String getName() { return "Fixed Manual Topology"; }
}