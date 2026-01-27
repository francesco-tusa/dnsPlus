package simulator.simulations.performance.metrics;

import java.util.*;
import simulator.core.TreeNode;
import simulator.entities.*;
import simulator.regions.BoundedBroker;
import simulator.regions.ProximityRoutingBroker;
import simulator.simulations.performance.SimulationType;

public class MetricsCollector {
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs) {

        SimulationType type = SimulationType.infer(root);
        PerformanceMetricsData data = type.createMetricsData();

        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null)
            queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();

            if (curr instanceof SimulationBroker b) {
                // Common Stats
                data.inputTableStats.accept(b.getInputSubscriptionCount());
                data.outputTableStats.accept(b.getOutputSubscriptionCount());

                data.totalSubscriptionInputEvents += b.getTotalSubscriptionProcessingEvents();
                data.totalPublicationProcessingEvents += b.getTotalPublicationProcessingEvents();
                data.totalMatchingComputations += b.getTotalMatchingComputations();
                data.totalFalsePositiveEvents += b.getTotalFalsePositiveEvents();

                if (data instanceof RegionPerformanceMetricsData rd && b instanceof BoundedBroker bb) {

                    // =========================================================
                    // 1. INPUT METRICS (Child -> Broker)
                    // =========================================================
                    rd.totalInputCovered += bb.getSubCoveredCount();
                    rd.totalInputExpanded += bb.getSubExpandedCount();

                    // Expansion Breakdown
                    rd.totalInputSimpleExpanded += bb.getSubSimpleExpandedCount();
                    rd.totalInputComplexExpanded += bb.getSubComplexExpandedCount();

                    rd.totalInputAdded += bb.getSubAddedCount();

                    // Optimization Side-effects
                    rd.totalInputMerged += bb.getSubMergedCount();
                    rd.totalInputAbsorbed += bb.getSubAbsorbedCount();

                    // =========================================================
                    // 2. UPSTREAM METRICS (Broker -> Parent)
                    // Mapping internal 'OutSub' counters to 'Upstream' metrics
                    // =========================================================
                    rd.totalPropagatedCovered += bb.getOutSubCoveredCount();
                    rd.totalPropagatedExpanded += bb.getOutSubExpandedCount();

                    // Expansion Breakdown
                    rd.totalPropagatedSimpleExpanded += bb.getOutSubSimpleExpandedCount();
                    rd.totalPropagatedComplexExpanded += bb.getOutSubComplexExpandedCount();

                    rd.totalPropagatedAdded += bb.getOutSubAddedCount();

                    // Optimization Side-effects
                    rd.totalPropagatedMerged += bb.getOutSubMergedCount();
                    rd.totalPropagatedAbsorbed += bb.getOutSubAbsorbedCount();
                } else if (data instanceof ProximityPerformanceMetricsData pd
                        && b instanceof ProximityRoutingBroker pb) {
                    pd.totalBrakeSuppressedEvents += pb.getBrakeFilteredCount();
                    pd.totalUpstreamSubscriptionUpdates += pb.getOutSubAddedCount();
                }
            }

            if (curr.getChildren() != null)
                queue.addAll(curr.getChildren());
        }

        // Subscriber & Publisher stats collection UNCHANGED
        for (SubscriberWithLocation s : subs) {
            data.totalDeliveriesReceived += s.getnPublications();
            data.totalFalsePositiveDeliveries += s.getFalsePositiveDeliveries();

            if (s.getHopCount() > 0) {
                data.totalHopSum += s.getHopSum();
                if (s.getHopMin() < data.globalMinHops)
                    data.globalMinHops = s.getHopMin();
                if (s.getHopMax() > data.globalMaxHops)
                    data.globalMaxHops = s.getHopMax();
            }
        }
        for (PublisherWithLocation p : pubs) {
            data.totalPublicationsSent += p.getnPublications();
        }

        return data;
    }
}