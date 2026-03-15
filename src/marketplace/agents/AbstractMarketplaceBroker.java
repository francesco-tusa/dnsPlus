package marketplace.agents;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.policy.StrictPropagationPolicy;
import simulator.regions.store.RegionSubscriptionStore;
import utils.CsvMetricWriter;
import marketplace.events.ServiceRequest;
import marketplace.optimization.ServiceSelectionStrategy;
import marketplace.topology.store.MarketplaceRegionStore;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractMarketplaceBroker extends SpatialMatchBroker {

    protected final Map<TreeNode, List<SimulationSubscription>> matchBuffer = new HashMap<>();
    protected ServiceSelectionStrategy selectionStrategy;

    protected static final double PENALTY_SCORE = 999.0;

    public AbstractMarketplaceBroker(String name, double threshold) {
        super(name, false, threshold, new StrictPropagationPolicy());
    }

    public AbstractMarketplaceBroker(String name, Location p1, Location p2, double threshold) {
        super(name, p1, p2, false, threshold, new StrictPropagationPolicy());
    }

    public void setSelectionStrategy(ServiceSelectionStrategy strategy) {
        this.selectionStrategy = strategy;
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (!(p instanceof ServiceRequest req)) {
            return super.matchPublication(p);
        }

        CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "RECEIVED", "Level " + this.getNodeLevel());

        this.matchBuffer.clear();

        // 1. Dual-Key Spatial & Topic Filter
        if (this.getInputStore() instanceof MarketplaceRegionStore store) {
            // Triggers the polymorphic directory resolution
            store.findMatchesForRequest(req, this.matchBuffer);
        } else {
            // Legacy fallback
            this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);
        }

        int evaluatedCandidates = 0;
        for (List<SimulationSubscription> candidates : this.matchBuffer.values()) {
            evaluatedCandidates += candidates.size();
        }
        this.totalMatchingComputations += evaluatedCandidates;

        if (this.matchBuffer.isEmpty()) {
            this.totalFalsePositiveEvents++;
            this.totalProactiveShieldedEvents++;
            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "DROP_NO_MATCH_SHIELDED", "No Spatial/Content Match");
            return null;
        }

        // 2. Strategy Execution 
        ServiceSelectionStrategy.SelectionResult selection = this.selectionStrategy.selectBestProvider(
                req, this.matchBuffer);
        TreeNode bestNode = selection.bestNode();

        boolean tracingEnabled = SimConfiguration.get().paths.enableEventTracing;

        // 3. Forwarding & Observability 
        if (bestNode != null) {
            String details = "Selected " + bestNode.getName();

            if (tracingEnabled && selection.bestScore() < PENALTY_SCORE) {
                details += String.format(" (Score: %.3f, Dist: %.1f)", selection.bestScore(), selection.distance());
            }
            
            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "FORWARDED", details);
            forwardPublicationToNode(p, bestNode);
        } else {
            // Drop Logic
            this.totalFalsePositiveEvents++;
            String logType;

            // STATELESS PROVENANCE CHECK: 
            // Is this broker the topological entry point (Ingress) for this request?
            if (!isIngressNode(req)) {
                // TRUE DEAD END: An upstream broker promised a match via a Hypercube that this child cannot fulfill.
                this.totalDownwardDeadEndEvents++;
                logType = "DROP_STRATEGY_DEAD_END";
            } else {
                // PROACTIVE SHIELDING: We blocked an unresolvable intent right at the edge/entry point.
                this.totalProactiveShieldedEvents++;
                logType = "DROP_STRATEGY_SHIELDED";
            }

            String reason = "No suitable provider found via Utility Strategy";
            if (tracingEnabled && selection.diagnosis() != null) {
                reason = selection.diagnosis();
            }

            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), logType, reason);
        }

        return null;
    }

    /**
     * Determines if this broker is the first topological hop for the publication.
     */
    private boolean isIngressNode(ServiceRequest req) {

        
        return req.getHops() == 0;
    }

    @Override
    protected abstract RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold);
}