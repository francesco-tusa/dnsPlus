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
     * Generates a list of subscriptions for a subscriber.
     * Implementing classes can override this to ensure diversity (e.g., preventing duplicate local subscriptions).
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