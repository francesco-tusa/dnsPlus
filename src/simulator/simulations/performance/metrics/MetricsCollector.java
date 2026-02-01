package simulator.simulations.performance.metrics;

import java.util.*;
import simulator.core.TreeNode;
import simulator.entities.*;
import simulator.regions.LeafBroker;
import simulator.simulations.performance.SimulationType;
import simulator.topology.analysis.TopologyAnalyser.TopologyStats;

public class MetricsCollector {
    
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs, TopologyStats topologyStats) {
        SimulationType type = SimulationType.infer(root);
        PerformanceMetricsData data = type.createMetricsData();
        return collect(root, subs, pubs, topologyStats, data);
    }

    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs, TopologyStats topologyStats, PerformanceMetricsData data) {

        if (topologyStats != null) {
            data.totalBrokers = topologyStats.totalBrokers;
            data.totalLeafBrokers = topologyStats.totalLeafBrokers;
        }

        // --- 1. Broker Stats (Tree Traversal) ---
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();

            if (curr instanceof SimulationBroker b) {
                visitBroker(b, data);
            }
            if (curr.getChildren() != null) queue.addAll(curr.getChildren());
        }

        // --- 2. Subscriber Stats ---
        for (SubscriberWithLocation s : subs) {
            visitSubscriber(s, data);
        }

        // --- 3. Publisher Stats ---
        for (PublisherWithLocation p : pubs) {
            data.totalPublicationsSent += p.getnPublications();
        }

        return data;
    }

    /**
     * Hook method for visiting a broker. Base implementation collects common stats.
     * Subclasses should override this (and call super) to collect specific stats.
     */
    protected void visitBroker(SimulationBroker b, PerformanceMetricsData data) {
        // Common Broker Stats
        data.inputTableStats.accept(b.getInputSubscriptionCount());
        data.outputTableStats.accept(b.getOutputSubscriptionCount());
        
        if (!(b instanceof LeafBroker)) {
            data.coreInputTableStats.accept(b.getInputSubscriptionCount());
        }

        data.totalSubscriptionInputEvents += b.getTotalSubscriptionProcessingEvents();
        data.totalPublicationProcessingEvents += b.getTotalPublicationProcessingEvents();
        data.totalMatchingComputations += b.getTotalMatchingComputations();
        data.totalFalsePositiveEvents += b.getTotalFalsePositiveEvents();
        data.totalPublicationsForwarded += b.getTotalPublicationForwardingEvents();
    }

    /**
     * Hook method for visiting a subscriber. Base implementation collects common stats.
     * Subclasses should override this (and call super) to collect specific stats (like Stretch).
     */
    protected void visitSubscriber(SubscriberWithLocation s, PerformanceMetricsData data) {
        data.totalDeliveriesReceived += s.getnPublications();
        data.totalFalsePositiveDeliveries += s.getFalsePositiveDeliveries();

        if (s.getHopCount() > 0) {
            data.totalHopSum += s.getHopSum();
            if (s.getHopMin() < data.globalMinHops) data.globalMinHops = s.getHopMin();
            if (s.getHopMax() > data.globalMaxHops) data.globalMaxHops = s.getHopMax();
        }
    }
}