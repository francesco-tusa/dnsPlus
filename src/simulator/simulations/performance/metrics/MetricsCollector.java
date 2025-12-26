package simulator.simulations.performance.metrics;

import java.util.*;
import simulator.core.TreeNode;
import simulator.entities.*;
import simulator.regions.BoundedBroker;

public class MetricsCollector {
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs) {
        PerformanceMetricsData data = new PerformanceMetricsData();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null)
            queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();
            if (curr instanceof SimulationBroker b) {
                data.inputTableStats.accept(b.getInputSubscriptionCount());
                data.outputTableStats.accept(b.getOutputSubscriptionCount());

                data.totalSubscriptionTraffic += b.getTotalSubscriptionProcessingEvents();
                data.totalPubForwardingEvents += b.getTotalPublicationProcessingEvents();
                data.totalMatchingComputations += b.getTotalMatchingComputations();
                data.totalFalsePositiveEvents += b.getTotalFalsePositiveEvents();

                if (b instanceof BoundedBroker bb) {
                    // INPUT
                    data.totalSubCovered += bb.getSubCoveredCount();
                    data.totalSubExpanded += bb.getSubExpandedCount();
                    data.totalSubAdded += bb.getSubAddedCount();
                    data.totalSubAbsorbed += bb.getSubAbsorbedCount();
                    data.totalSubMerged += bb.getSubMergedCount();
                    
                    // OUTPUT
                    data.totalOutSubCovered += bb.getOutSubCoveredCount();
                    data.totalOutSubExpanded += bb.getOutSubExpandedCount();
                    data.totalOutSubAdded += bb.getOutSubAddedCount();
                    data.totalOutSubAbsorbed += bb.getOutSubAbsorbedCount();
                    data.totalOutSubMerged += bb.getOutSubMergedCount();
                }
            }
            if (curr.getChildren() != null)
                queue.addAll(curr.getChildren());
        }

        for (SubscriberWithLocation s : subs) {
            data.totalNotifications += s.getnPublications();
            data.totalFalsePositiveDeliveries += s.getFalsePositiveDeliveries();
            data.hopStats.combine(s.getHopStats());
        }
        for (PublisherWithLocation p : pubs) {
            data.totalPubsSent += p.getnPublications();
        }
        return data;
    }
}