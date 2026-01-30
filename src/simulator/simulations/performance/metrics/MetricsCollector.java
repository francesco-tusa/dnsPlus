package simulator.simulations.performance.metrics;

import java.util.*;
import simulator.core.TreeNode;
import simulator.entities.*;
import simulator.regions.BoundedBroker;
import simulator.regions.LeafBroker;
import simulator.regions.ProximityRoutingBroker;
import simulator.simulations.performance.SimulationType;
import simulator.topology.analysis.TopologyAnalyser.TopologyStats;

public class MetricsCollector {
    
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs, TopologyStats topologyStats) {

        SimulationType type = SimulationType.infer(root);
        PerformanceMetricsData data = type.createMetricsData();
        
        if (topologyStats != null) {
            data.totalBrokers = topologyStats.totalBrokers;
            data.totalLeafBrokers = topologyStats.totalLeafBrokers;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null)
            queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();

            if (curr instanceof SimulationBroker b) {
                // 1. Common Stats (All Brokers)
                data.inputTableStats.accept(b.getInputSubscriptionCount());
                data.outputTableStats.accept(b.getOutputSubscriptionCount());
                
                // 2. Core Stats (Non-Leaf Brokers Only)
                boolean isLeaf = b instanceof LeafBroker;
                
                if (!isLeaf) {
                    data.coreInputTableStats.accept(b.getInputSubscriptionCount());
                }

                data.totalSubscriptionInputEvents += b.getTotalSubscriptionProcessingEvents();
                data.totalPublicationProcessingEvents += b.getTotalPublicationProcessingEvents();
                data.totalMatchingComputations += b.getTotalMatchingComputations();
                data.totalFalsePositiveEvents += b.getTotalFalsePositiveEvents();

                if (data instanceof RegionPerformanceMetricsData rd && b instanceof BoundedBroker bb) {
                    rd.totalInputCovered += bb.getSubCoveredCount();
                    rd.totalInputExpanded += bb.getSubExpandedCount();
                    rd.totalInputSimpleExpanded += bb.getSubSimpleExpandedCount();
                    rd.totalInputComplexExpanded += bb.getSubComplexExpandedCount();
                    rd.totalInputAdded += bb.getSubAddedCount();
                    rd.totalInputMerged += bb.getSubMergedCount();
                    rd.totalInputAbsorbed += bb.getSubAbsorbedCount();
                    rd.totalPropagatedCovered += bb.getOutSubCoveredCount();
                    rd.totalPropagatedExpanded += bb.getOutSubExpandedCount();
                    rd.totalPropagatedSimpleExpanded += bb.getOutSubSimpleExpandedCount();
                    rd.totalPropagatedComplexExpanded += bb.getOutSubComplexExpandedCount();
                    rd.totalPropagatedAdded += bb.getOutSubAddedCount();
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