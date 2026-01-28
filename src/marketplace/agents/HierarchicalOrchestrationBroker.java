package marketplace.agents;

import simulator.regions.ProximityRoutingBroker;
import simulator.regions.Region;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import marketplace.common.MultiMetricLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Broker that implements Hierarchical Aggregation and Multi-Metric Routing.
 * Designed for the Privacy-Preserving FaaS Marketplace.
 */
public class HierarchicalOrchestrationBroker extends ProximityRoutingBroker {

    public HierarchicalOrchestrationBroker(String name) {
        super(name);
    }

    public HierarchicalOrchestrationBroker(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * Override to use the Subscription's Location (which carries Metrics)
     * instead of the Entity's physical location.
     */
    @Override
    protected void updateTopologicalTargets(TreeNode child) {
        // 1. Check if we have active subscriptions for this child
        SimulationSubscription sub = inputStore.get(child);

        Location[] targets = null;

        if (sub != null) {
            // In BasicSubscriptionStore, there is only one subscription per child.
            // So we target that one location.
            targets = new Location[1];
            boolean foundMetricLocation = false;

            if (sub instanceof SubscriptionWithLocation) {
                targets[0] = ((SubscriptionWithLocation) sub).getLocation();
                if (targets[0] instanceof MultiMetricLocation) {
                    foundMetricLocation = true;
                }
            } else {
                // Fallback if subscription has no location
                targets[0] = child.getMetricLocation();
            }

            // If we found valid subscription locations, use them.
            if (foundMetricLocation) {
                childTopologicalTargets.put(child, targets);

                // We must also reset the best distances array because the size might have
                // changed
                // or the semantics are different.
                // ProximityRoutingBroker.initializeDistances is private, so we implement it
                // here manually.
                double[] dists = new double[targets.length];
                for (int k = 0; k < targets.length; k++)
                    dists[k] = Double.MAX_VALUE;
                childBestDistances.put(child, dists);
                return;
            }
        }

        // 2. Fallback to standard behavior if no specific subscription info found
        super.updateTopologicalTargets(child);
    }

    /**
     * Override to Aggregates metrics from children instead of just sending the
     * center.
     */
    @Override
    protected void propagateSubscriptionUpward(SimulationSubscription originalSub) {
        // We do strictly hierarchical aggregation.
        // We only send ONE proxy subscription representing the "Best" offer in this
        // region.

        if (getParentBroker() == null)
            return;

        Region r = getRegion();
        Location myCenter = (r != null) ? r.getCenter() : null;
        if (myCenter == null)
            return;

        // 1. Calculate Aggregated Metrics (Min Latency, Min Cost, etc.)
        Map<String, Double> bestMetrics = new HashMap<>();
        boolean hasMetrics = false;

        // Iterate over ALL children contributions in the input store
        // inputStore.getAllSubscriptions() returns Map<TreeNode,
        // List<SimulationSubscription>>
        for (List<SimulationSubscription> subList : inputStore.getAllSubscriptions().values()) {
            for (SimulationSubscription s : subList) {
                if (s instanceof SubscriptionWithLocation) {
                    Location loc = ((SubscriptionWithLocation) s).getLocation();
                    if (loc instanceof MultiMetricLocation) {
                        Map<String, Double> m = ((MultiMetricLocation) loc).getMetrics();
                        if (m != null) {
                            hasMetrics = true;
                            // For each metric, we take the MIN
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

        // 2. Create the Proxy Subscription
        Location proxyLoc;
        if (hasMetrics) {
            proxyLoc = new MultiMetricLocation(myCenter, bestMetrics);
        } else {
            proxyLoc = myCenter;
        }

        SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(proxyLoc);
        proxySubscription.setSource(this);
        proxySubscription.setHops(originalSub.getHops());

        // 3. Send Upward
        getParentBroker().processSubscription(proxySubscription);
    }
}
