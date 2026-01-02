package simulator.tests.fixtures;

import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

public class GridTopologyFixture implements TopologyFixture {
    private BoundedBroker root; 

    @Override
    public void setup(SpatialMatchBrokerFactory factory) {
        // 3x3 Grid Configuration
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator generator = new GridTopologyGenerator(factory);
        
        // 1. Build Brokers
        this.root = generator.generateTopology(config);
        
        // 2. Attach Clients
        // Grid generator requires these calls to populate leaves with "sub-x-y-z"
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
    public String getName() { return "Grid (3x3) Topology"; }
}