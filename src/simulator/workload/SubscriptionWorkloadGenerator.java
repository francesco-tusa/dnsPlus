package simulator.workload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;

public interface SubscriptionWorkloadGenerator {
    
    SimulationSubscription generateSubscription(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers);

    /**
     * Optional method to configure Interest Hotspots.
     * Default implementation does nothing, so classes that don't use hotspots (like Location/Marketplace) don't break.
     */
    default void setHotspots(List<BoundedBroker> hotspots) {
        // No-op by default
    }

    /**
     * Generates a list of subscriptions for a subscriber.
     */
    default List<SimulationSubscription> generateSubscriptionBatch(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers, int count) {
        if (count <= 0) return Collections.emptyList();
        List<SimulationSubscription> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(generateSubscription(subscriber, leafBrokers));
        }
        return batch;
    }
}