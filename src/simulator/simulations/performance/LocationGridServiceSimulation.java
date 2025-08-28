package simulator.simulations.performance;

import java.util.List;
import simulator.Location;
import simulator.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

/**
 * A concrete simulation that runs a service replication scenario on a
 * dynamically generated, grid-based world topology using location-processing brokers.
 */
public class LocationGridServiceSimulation extends AbstractServiceSimulation<
    GridTopologyConfiguration,
    GridTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 1000;
    private static final int NUMBER_OF_REPLICAS = 5;
    private static final double SUBSCRIPTION_REGION_SIZE = 10.0;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.2;
    
    // --- Topology Parameters ---
    private static final int GRID_DIMENSION = 10;
    private static final int TREE_DEPTH = 4;
    private static final double OVERLAP_FACTOR = 0.25;

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
        System.out.println("--- Starting Location-Based Service Simulation on Grid Topology ---");
        
        GridTopologyConfiguration config = new GridTopologyConfiguration(
            GRID_DIMENSION, 
            OVERLAP_FACTOR, 
            TREE_DEPTH
        );
        
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());
        
        LocationGridServiceSimulation simulation = new LocationGridServiceSimulation();
        simulation.run(factory, config);
    }
}