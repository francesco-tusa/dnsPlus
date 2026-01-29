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
 * Selects the single best provider based on a "Dominant Metric" (e.g.,
 * Latency).
 * Advertises the exact profile of that provider. prevents "Frankenstein"
 * proxies.
 */
public class DominantMetricPolicy implements AggregationPolicy {

    private final String metricName;

    public DominantMetricPolicy(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public SubscriptionWithLocation aggregate(Map<TreeNode, List<SimulationSubscription>> subscriptions,
            Location regionCenter) {
        Map<String, Double> bestMetrics = null;
        double minVal = Double.MAX_VALUE;
        boolean hasMetrics = false;

        for (List<SimulationSubscription> subList : subscriptions.values()) {
            for (SimulationSubscription s : subList) {
                if (s instanceof SubscriptionWithLocation) {
                    Location loc = ((SubscriptionWithLocation) s).getLocation();
                    if (loc instanceof MultiMetricLocation) {
                        Map<String, Double> m = ((MultiMetricLocation) loc).getMetrics();
                        if (m != null && m.containsKey(metricName)) {
                            hasMetrics = true;
                            double currentVal = m.get(metricName);
                            if (currentVal < minVal) {
                                minVal = currentVal;
                                bestMetrics = new HashMap<>(m); // Copy FULL profile
                            }
                        }
                    }
                }
            }
        }

        Location proxyLoc;
        if (hasMetrics && bestMetrics != null) {
            proxyLoc = new MultiMetricLocation(regionCenter, bestMetrics);
        } else {
            proxyLoc = regionCenter;
        }

        return new SubscriptionWithLocation(proxyLoc);
    }
}
