package simulator.simulations.performance;

import java.util.List;

import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * A concrete simulation that runs a service replication scenario on the
 * full, data-driven topology generated from GeoNames data using location-processing brokers.
 */
public class WorldTopologyLocationPerformanceSimulation extends AbstractPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 500_000;
    private static final int NUMBER_OF_REPLICAS = 20;
    private static final double SUBSCRIPTION_REGION_SIZE = 1.0;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.1;

    @Override
    protected long getTotalSubscribers() {
        return TOTAL_SUBSCRIBERS;
    }

    @Override
    protected int getNumberOfReplicas() {
        return NUMBER_OF_REPLICAS;
    }

    @Override
    protected double getSubscriptionRegionSize() {
        return SUBSCRIPTION_REGION_SIZE;
    }
    
    @Override
    protected double getRemoteInterestProbability() {
        return REMOTE_INTEREST_PROBABILITY;
    }

    @Override
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
        
        Location centerOfInterest;

        if (random.nextDouble() < getRemoteInterestProbability()) {
            BrokerWithRegion remoteBroker = allLeafBrokers.get(random.nextInt(allLeafBrokers.size()));
            centerOfInterest = getRandomLocationInRegion(remoteBroker.getRegion());
        } else {
            centerOfInterest = subscriber.getLocation();
        }

        Region subscriptionRegion = new Region(
            new Location(centerOfInterest.getX() - (getSubscriptionRegionSize() / 2), 
                         centerOfInterest.getY() - (getSubscriptionRegionSize() / 2), 0),
            new Location(centerOfInterest.getX() + (getSubscriptionRegionSize() / 2), 
                         centerOfInterest.getY() + (getSubscriptionRegionSize() / 2), 0)
        );
        
        return new SubscriptionWithRegion(subscriptionRegion);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Location-Based Service Simulation on GeoNames Topology ---");
        
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        
        // Correctly instantiate the generator with the factory
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        WorldTopologyLocationPerformanceSimulation simulation = new WorldTopologyLocationPerformanceSimulation();
        simulation.run(factory, config);
    }
}