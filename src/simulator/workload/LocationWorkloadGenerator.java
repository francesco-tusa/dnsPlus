package simulator.workload;

import java.util.List;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BoundedBroker;

public class LocationWorkloadGenerator implements SubscriptionWorkloadGenerator {

    @Override
    public SimulationSubscription generateSubscription(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers) {
        // Location-Based Logic:
        // Subscribers simply subscribe to their current physical coordinates.
        SubscriptionWithLocation sub = new SubscriptionWithLocation(subscriber.getLocation());

        sub.setSource(subscriber);
        
        return sub;
    }
}