package simulator.tests.fixtures;

import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.regions.BoundedBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.BoundedBrokerFactory;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

public class GridTopologyFixture implements TopologyFixture {
    private BoundedBroker root; 

    @Override
    public void setup(BrokerFactory factory) {
        if (!(factory instanceof BoundedBrokerFactory boundedFactory)) {
            throw new IllegalArgumentException("GridTopologyFixture requires BoundedBrokerFactory (Region-based), but received: " + factory.getClass().getSimpleName());
        }

        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator generator = new GridTopologyGenerator(boundedFactory);
        
        // 1. Build Brokers
        this.root = generator.generateTopology(config);
        
        // 2. Attach Standard Clients
        generator.attachSubscribers(this.root);
        generator.attachPublishers(this.root);
        
        // 3. Add extra subscribers for Aggregation Tests
        ensureSufficientSubscribers(this.root);
    }

    private void ensureSufficientSubscribers(BoundedBroker root) {
        // Find a leaf to populate
        var leaves = TopologyAnalyser.findLeafBrokers(root);
        if (!leaves.isEmpty()) {
            BoundedBroker target = leaves.get(0);
            simulator.core.Location loc = target.getRegion().getCenter();
            
            // Add 2 extra subscribers named specifically for tracking or just generically
            target.addChild(new simulator.entities.SubscriberWithLocation("Extra-Sub-1", loc));
            target.addChild(new simulator.entities.SubscriberWithLocation("Extra-Sub-2", loc));
            
            // Update region to include new children
            if (!target.getChildren().isEmpty()) {
                target.updateRegion(target.getChildren().get(target.getChildren().size()-1));
            }
        }
    }

    @Override
    public SimulationBroker getRoot() { 
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