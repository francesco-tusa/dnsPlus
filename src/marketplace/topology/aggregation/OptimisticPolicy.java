package marketplace.topology.aggregation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import marketplace.common.MultiMetricLocation;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;

/**
 * Aggregates the BEST of EACH metric independently (Min Latency AND Min Cost).
 * Creates a "Frankenstein" proxy representing the region's Potential.
 * Useful for maximizing discovery in multi-objective searches.
 */
public class OptimisticPolicy implements AggregationPolicy {

    @Override
    public SubscriptionWithLocation aggregate(Map<TreeNode, List<SimulationSubscription>> subscriptions,
            Location regionCenter) {
        Map<String, Double> bestMetrics = new HashMap<>();
        boolean hasMetrics = false;

        for (List<SimulationSubscription> subList : subscriptions.values()) {
            for (SimulationSubscription s : subList) {
                if (s instanceof SubscriptionWithLocation) {
                    Location loc = ((SubscriptionWithLocation) s).getLocation();
                    if (loc instanceof MultiMetricLocation) {
                        Map<String, Double> m = ((MultiMetricLocation) loc).getMetrics();
                        if (m != null) {
                            hasMetrics = true;
                            for (Map.Entry<String, Double> entry : m.entrySet()) {
                                String key = entry.getKey();
                                double val = entry.getValue();
                                if (!bestMetrics.containsKey(key) || val < bestMetrics.get(key)) {
                                    bestMetrics.put(key, val);
                                }
                            }
                        }
                    }
                }
            }
        }

        Location proxyLoc;
        if (hasMetrics) {
            proxyLoc = new MultiMetricLocation(regionCenter, bestMetrics);
        } else {
            proxyLoc = regionCenter;
        }

        return new SubscriptionWithLocation(proxyLoc);
    }
}
