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
import java.util.ArrayList;

public abstract class AbstractMarketplaceBroker extends SpatialMatchBroker {

    protected final List<TreeNode> matchBuffer = new ArrayList<>();
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

        // 1. Spatial Filter (Physical Layer)
        this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);

        // --- DEBUG INJECTION: SPATIAL MATCH LAYER ---
        System.out.println("\n[DEBUG Broker: " + this.getName() + "] Request: " + req.getPreferences() + " at "
                + req.getLocation());
        System.out.println(
                "[DEBUG Broker: " + this.getName() + "] Found " + this.matchBuffer.size() + " spatial matches.");
        for (TreeNode node : this.matchBuffer) {
            System.out.println(
                    "   -> Match Candidate: " + node.getName() + " (Class: " + node.getClass().getSimpleName() + ")");
        }
        // ---------------------------------------------

        if (this.matchBuffer.isEmpty()) {
            this.totalFalsePositiveEvents++;
            System.out.println("[DEBUG Broker: " + this.getName() + "] DROP_NO_MATCH: No Spatial Matches.");
            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "DROP_NO_MATCH", "No Spatial/Content Match");
            return null;
        }

        // 2. Strategy Execution (Logic Layer)
        ServiceSelectionStrategy.SelectionResult selection = this.selectionStrategy.selectBestProvider(
                req, this.matchBuffer, this.getInputStore());
        TreeNode bestNode = selection.bestNode();

        // --- DEBUG INJECTION: STRATEGY LAYER ---
        System.out.println("[DEBUG Broker: " + this.getName() + "] Selection Strategy Best Node: " +
                (bestNode != null ? bestNode.getName() : "NONE") +
                " | Diagnosis: " + selection.diagnosis());
        // ---------------------------------------------

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

            String reason = "No suitable provider found via Utility Strategy";
            if (tracingEnabled && selection.diagnosis() != null) {
                reason = selection.diagnosis();
            }

            CsvMetricWriter.getInstance().logPublication(
                    p, this.getName(), "DROP_STRATEGY_REJECT", reason);
        }

        return null;
    }

    // Abstract Factory Methods to be implemented by Concrete Brokers
    @Override
    protected abstract RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold);
}