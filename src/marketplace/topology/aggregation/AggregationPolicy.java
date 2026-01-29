package marketplace.topology.aggregation;

import java.util.List;
import java.util.Map;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;

/**
 * Strategy interface for aggregating child subscriptions into a single proxy
 * subscription.
 */
public interface AggregationPolicy {

    /**
     * Calculates the proxy subscription location (with metrics) based on child
     * subscriptions.
     * 
     * @param subscriptions Map of all child subscriptions (from inputStore).
     * @param regionCenter  The center of the current region (fallback location).
     * @param broker        The broker instance (for context/logging if needed).
     * @return A new SubscriptionWithLocation containing the aggregated
     *         MultiMetricLocation.
     */
    SubscriptionWithLocation aggregate(
            Map<TreeNode, List<SimulationSubscription>> subscriptions,
            Location regionCenter);
}
