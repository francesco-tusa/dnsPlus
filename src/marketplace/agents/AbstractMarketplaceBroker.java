package marketplace.agents;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.policy.StrictPropagationPolicy;
import simulator.regions.store.RegionSubscriptionStore;
import utils.CsvMetricWriter;
import marketplace.events.ServiceRequest;
import marketplace.optimization.ServiceSelectionStrategy;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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

        // 1. Spatial Filter
        this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);

        if (this.matchBuffer.isEmpty()) {
            this.totalFalsePositiveEvents++;
            this.totalProactiveShieldedEvents++;
            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "DROP_NO_MATCH_SHIELDED", "No Spatial/Content Match");
            return null;
        }

        // 2. Strategy Execution (No longer requires passing the store)
        ServiceSelectionStrategy.SelectionResult selection = this.selectionStrategy.selectBestProvider(
                req, this.matchBuffer);
        TreeNode bestNode = selection.bestNode();

        boolean tracingEnabled = SimConfiguration.get().paths.enableEventTracing;

        // 3. Forwarding & Observability (INTEGRATION POINT)
        if (bestNode != null) {
            String details = "Selected " + bestNode.getName();

            if (tracingEnabled && selection.bestScore() < PENALTY_SCORE) {
                details += String.format(" (Score: %.3f, Dist: %.1f)", selection.bestScore(), selection.distance());
            }

            // COMMIT: The request is now moving DOWN toward a provider
            req.markAsRoutingDown();

            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "FORWARDED", details);
            forwardPublicationToNode(p, bestNode);
        } else {
            // Drop Logic
            this.totalFalsePositiveEvents++;

            String logType;
            if (req.isRoutingDown()) {
                // TRUE DEAD END: An upper broker promised a match that this child cannot fulfill.
                this.totalDownwardDeadEndEvents++;
                logType = "DROP_STRATEGY_DEAD_END";
            } else {
                // PROACTIVE SHIELDING: We blocked an invalid request during the search phase.
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

    // Abstract Factory Methods to be implemented by Concrete Brokers
    @Override
    protected abstract RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold);
}