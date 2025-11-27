package simulator.workload;

import java.util.List;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;

/**
 * Strategy interface for generating the content of subscriptions
 * (Workload Generation).
 */
public interface SubscriptionWorkloadGenerator {
    
    /**
     * Generates a subscription event for a specific subscriber.
     * @param subscriber The subscriber entity generating the event.
     * @param leafBrokers The list of all leaf brokers (used for calculating remote interests).
     * @return The subscription event to be sent.
     */
    SimulationSubscription generateSubscription(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers);
}