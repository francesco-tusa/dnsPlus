package marketplace.agents;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.PublicationWithLocation;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.policy.StrictPropagationPolicy;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import java.util.List;
import java.util.ArrayList;

public class MarketplaceBroker extends SpatialMatchBroker {

    private final List<TreeNode> matchBuffer = new ArrayList<>();

    public MarketplaceBroker(String name) {
        super(name, false, 0.5, new StrictPropagationPolicy());
    }

    public MarketplaceBroker(String name, Location p1, Location p2) {
        super(name, p1, p2, false, 0.5, new StrictPropagationPolicy());
    }

    /**
     * MARKETPLACE OVERRIDE:
     * Intercepts the creation of propagated subscriptions to ensure
     * MetricHyperCubes are preserved.
     */
    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState,
            Region newRegionPayload) {
        // Check if we are dealing with a MetricHyperCube
        if (aggregatedState.getRegion() instanceof MetricHyperCube mhc) {

            // We must create a NEW MetricHyperCube that inherits the properties of the
            // aggregated one
            // but uses the 'newRegionPayload' (which contains the aggregated physical
            // bounds)
            MetricHyperCube newCube = new MetricHyperCube(mhc);
            newCube.set(newRegionPayload);

            return new SubscriptionWithRegion(newCube);
        }

        return super.createCandidateSubscription(aggregatedState, newRegionPayload);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (!(p instanceof ServiceRequest)) {
            return super.matchPublication(p);
        }
        ServiceRequest req = (ServiceRequest) p;

        this.matchBuffer.clear();
        this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);

        if (this.matchBuffer.isEmpty())
            return null;

        TreeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;

        for (TreeNode candidate : this.matchBuffer) {
            if (candidate == p.getSource())
                continue;

            List<SimulationSubscription> subs = this.getInputStore().getAllSubscriptions()
                    .get(candidate);

            if (subs == null)
                continue;

            for (SimulationSubscription sub : subs) {
                // Check for ServiceOffer type to safely access service ID
                if (sub instanceof ServiceOffer offer) {
                    if (offer.getServiceId() == req.getServiceId()) {
                        if (offer.getRegion() instanceof MetricHyperCube cap) {
                            if (cap.contains(req.getLocation())) {
                                double score = cap.getMinValues()[0];
                                if (score < bestScore) {
                                    bestScore = score;
                                    bestNode = candidate;
                                }
                            }
                        }
                    }
                }
                // Fallback for testing or non-ServiceOffer wrappers
                else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube cap) {
                    if (cap.contains(req.getLocation())) {
                        double score = cap.getMinValues()[0];

                        if (score < bestScore) {
                            bestScore = score;
                            bestNode = candidate;
                        }
                    }
                }
            }
        }

        if (bestNode != null) {
            forwardPublicationToNode(p, bestNode);
        }

        return null;
    }
}