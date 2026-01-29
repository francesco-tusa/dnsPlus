package marketplace.agents;

import simulator.regions.ProximityRoutingBroker;
import simulator.regions.Region;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;

import java.util.Map;

import marketplace.common.MultiMetricLocation;
import marketplace.topology.aggregation.AggregationPolicy;
import marketplace.topology.aggregation.OptimisticPolicy;

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

    private AggregationPolicy aggregationPolicy = new OptimisticPolicy(); // Default to Optimistic (Frankenstein)

    public void setAggregationPolicy(AggregationPolicy policy) {
        this.aggregationPolicy = policy;
    }

    /**
     * Override to ensure we refresh topological targets when a subscription
     * arrives.
     * The base ProximityRoutingBroker only updates if the child is missing,
     * but we need to upgrade the target from "Plain Location" to
     * "MultiMetricLocation".
     */
    @Override
    protected void handleSubscriptionProcessing(SimulationSubscription s) {
        super.handleSubscriptionProcessing(s);
        // Force update to pick up metrics from the subscription
        updateTopologicalTargets(s.getSource());
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

        // Delegate aggregation to the policy
        SubscriptionWithLocation proxySubscription = aggregationPolicy.aggregate(inputStore.getAllSubscriptions(),
                myCenter);

        proxySubscription.setSource(this);
        proxySubscription.setHops(originalSub.getHops());

        // 3. Send Upward
        getParentBroker().processSubscription(proxySubscription);
    }

    /**
     * Override to implement SINGLE-WINNER Routing (Unicast/RPC style).
     * Instead of propagating to ALL better options, we pick ONLY the absolute best.
     */
    @Override
    public simulator.events.SimulationSubscription matchPublication(simulator.events.SimulationPublication p) {
        // 1. Try to resolve LOCALLY (Downward)
        boolean resolvedLocally = false;
        if (p instanceof simulator.events.PublicationWithLocation pub) {
            resolvedLocally = routeToBestChild(pub);
        }

        // 2. ONLY propagate UPWARD if NOT resolved locally
        if (!resolvedLocally && p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        return null;
    }

    /**
     * Override to redirect to our new logic.
     * Kept for compatibility if base class calls it, but matchPublication is the
     * main entry.
     */
    @Override
    protected void processPublicationDownward(simulator.events.PublicationWithLocation pub) {
        routeToBestChild(pub);
    }

    /**
     * Tries to find the Single Winner among children.
     * 
     * @return true if a winner was found and accepted (Score < Threshold).
     */
    private boolean routeToBestChild(simulator.events.PublicationWithLocation pub) {
        // 1. Identification Phase: Calculate scores for all candidates
        TreeNode bestChild = null;
        double minScore = Double.MAX_VALUE;

        for (Map.Entry<TreeNode, Location[]> entry : childTopologicalTargets.entrySet()) {
            TreeNode neighbor = entry.getKey();

            // Loop prevention
            if (neighbor == pub.getSource())
                continue;
            if (neighbor == getParentBroker())
                continue;
            if (inputStore.get(neighbor) == null)
                continue; // No interest

            Location[] targets = entry.getValue();
            if (targets == null)
                continue;

            // Find the best distance for THIS neighbor against the request
            for (Location target : targets) {
                double score = pub.getLocation().distanceSquared(target);
                if (score < minScore) {
                    minScore = score;
                    bestChild = neighbor;
                }
            }
        }

        // 2. Forwarding Phase: Send ONLY to the winner
        if (bestChild != null) {
            // STRICT REQUIREMENT CHECK
            if (minScore > MultiMetricLocation.MAX_ACCEPTABLE_DISTANCE) {
                if (simulator.config.SimConfiguration.get().paths.enableEventTracing && pub.getMetrics() != null) {
                    System.out.println(
                            "  [Broker " + getName() + "] REJECTED Winner: " + bestChild.getName() + " (Score: "
                                    + String.format("%.2f", minScore) + " > Threshold)");
                }
                return false; // Found, but REJECTED -> Treated as "Not Resolved"
            }

            if (simulator.config.SimConfiguration.get().paths.enableEventTracing && pub.getMetrics() != null) {
                System.out.println("  [Broker " + getName() + "] Routed to Winner: " + bestChild.getName() + " (Score: "
                        + String.format("%.2f", minScore) + ")");
            }
            sendToChild(bestChild, pub, minScore);
            return true;
        }

        return false;
    }

    private void sendToChild(TreeNode child, simulator.events.PublicationWithLocation pub, double score) {
        simulator.events.SimulationPublication abstractCopy = pub.getPublication();
        if (abstractCopy instanceof simulator.events.PublicationWithLocation) {
            simulator.events.PublicationWithLocation copy = (simulator.events.PublicationWithLocation) abstractCopy;
            copy.setSource(this);
            copy.copyStateFrom(pub);
            copy.incrementHops();
            if (score >= 0)
                copy.setCachedDistanceSquared(score);

            if (child instanceof simulator.regions.BoundedBroker) {
                ((simulator.regions.BoundedBroker) child).processPublication(copy);
            } else if (child instanceof simulator.entities.SubscriberWithLocation) {
                ((simulator.entities.SubscriberWithLocation) child).receive(copy);
            }
        }
    }
}
