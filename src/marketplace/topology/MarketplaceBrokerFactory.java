package marketplace.topology;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.topology.factories.BoundedBrokerFactory;
import simulator.config.SimConfiguration;
import marketplace.agents.HypercubeMarketplaceBroker;

/**
 * A factory for creating brokers that use the Hierarchical Orchestration strategy.
 * Retrieves core thresholds from configuration and dynamically instantiates the 
 * correct concrete Broker architecture (HyperCube vs Skyline) based on properties.
 */
public class MarketplaceBrokerFactory implements BoundedBrokerFactory {
    
    @Override
    public BoundedBroker createBroker(String name) {
        double threshold = SimConfiguration.get().broker.getSmartThreshold();
        
        return new HypercubeMarketplaceBroker(name, threshold);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        double threshold = SimConfiguration.get().broker.getSmartThreshold();
        
        return new HypercubeMarketplaceBroker(name, p1, p2, threshold);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        // Defer to the main parameterless creation method
        return createBroker(name);
    }
}