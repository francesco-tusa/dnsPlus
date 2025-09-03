package simulator.simulations.performance;

import java.util.List;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * A concrete simulation that runs a performance scenario on a
 * dynamically generated, random topology using region-based brokers.
 */
public class RandomTopologyRegionPerformanceSimulation extends AbstractPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 1000;
    private static final int NUMBER_OF_REPLICAS = 10;
    private static final double SUBSCRIPTION_REGION_SIZE = 10.0;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.2;
    
    // --- Topology Parameters ---
    private static final int TREE_DEPTH = 4;
    private static final int MAX_BRANCHING = 3;
    private static final int NUM_REGIONS = 5;
    private static final int SUBS_PER_LEAF = 50;
    private static final int PUBS_PER_LEAF = 5;

    @Override
    protected long getTotalSubscribers() { return TOTAL_SUBSCRIBERS; }

    @Override
    protected int getNumberOfReplicas() { return NUMBER_OF_REPLICAS; }

    @Override
    protected double getSubscriptionRegionSize() { return SUBSCRIPTION_REGION_SIZE; }

    @Override
    protected double getRemoteInterestProbability() { return REMOTE_INTEREST_PROBABILITY; }

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
        System.out.println("--- Starting Random Topology Performance Simulation (Region-Based) ---");
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(TREE_DEPTH, MAX_BRANCHING, NUM_REGIONS, SUBS_PER_LEAF, PUBS_PER_LEAF);
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new RegionBrokerFactory());
        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation();
        simulation.run(factory, config);
    }
}