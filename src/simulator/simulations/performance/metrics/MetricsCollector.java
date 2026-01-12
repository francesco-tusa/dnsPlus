package simulator.simulations.performance.metrics;

import java.util.*;
import simulator.core.TreeNode;
import simulator.entities.*;
import simulator.regions.BoundedBroker;
import simulator.regions.ProximityRoutingBroker; 

public class MetricsCollector {
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs,
            List<PublisherWithLocation> pubs) {
        
        // 1. Factory Logic: Create specific data object based on Root Node Type
        PerformanceMetricsData data;
        
        // Check for Proximity/Location Broker first
        if (root instanceof ProximityRoutingBroker) {
            data = new ProximityPerformanceMetricsData();
        } else {
            // Default to Region for SpatialMatchBroker (which extends BoundedBroker)
            data = new RegionPerformanceMetricsData();
        }

        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null)
            queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();
            
            if (curr instanceof SimulationBroker b) {
                // A. Common Stats (collected for ALL simulation types)
                data.inputTableStats.accept(b.getInputSubscriptionCount());
                data.outputTableStats.accept(b.getOutputSubscriptionCount());

                data.totalSubscriptionTraffic += b.getTotalSubscriptionProcessingEvents();
                data.totalPubForwardingEvents += b.getTotalPublicationProcessingEvents();
                data.totalMatchingComputations += b.getTotalMatchingComputations();
                data.totalFalsePositiveEvents += b.getTotalFalsePositiveEvents();

                // B. Polymorphic Stats Collection (Specific to Algorithm)
                if (data instanceof RegionPerformanceMetricsData rd && b instanceof BoundedBroker bb) {
                    // --- Region (Spatial Match) Metrics ---
                    // INPUT
                    rd.totalSubCovered += bb.getSubCoveredCount();
                    rd.totalSubExpanded += bb.getSubExpandedCount();
                    rd.totalSubAdded += bb.getSubAddedCount();
                    rd.totalSubAbsorbed += bb.getSubAbsorbedCount();
                    rd.totalSubMerged += bb.getSubMergedCount();
                    
                    // OUTPUT
                    rd.totalOutSubCovered += bb.getOutSubCoveredCount();
                    rd.totalOutSubExpanded += bb.getOutSubExpandedCount();
                    rd.totalOutSubAdded += bb.getOutSubAddedCount();
                    rd.totalOutSubAbsorbed += bb.getOutSubAbsorbedCount();
                    rd.totalOutSubMerged += bb.getOutSubMergedCount();
                } 
                else if (data instanceof ProximityPerformanceMetricsData pd && b instanceof ProximityRoutingBroker pb) {
                    // --- Proximity (Location) Metrics ---
                    // "Brake" strategy stats (suppressed updates)
                    pd.totalBrakeFilteredEvents += pb.getBrakeFilteredCount();
                }
            }
            
            // Traverse down the tree
            if (curr.getChildren() != null)
                queue.addAll(curr.getChildren());
        }

        // C. Subscriber Stats (Notifications & Hops)
        for (SubscriberWithLocation s : subs) {
            data.totalNotifications += s.getnPublications();
            data.totalFalsePositiveDeliveries += s.getFalsePositiveDeliveries();
            
            if (s.getHopCount() > 0) {
                data.totalHopSum += s.getHopSum();
                data.totalHopCount += s.getHopCount();
                
                // Track global min/max hops
                if (s.getHopMin() < data.globalMinHops) data.globalMinHops = s.getHopMin();
                if (s.getHopMax() > data.globalMaxHops) data.globalMaxHops = s.getHopMax();
            }
        }
        
        // D. Publisher Stats (Publications Sent)
        for (PublisherWithLocation p : pubs) {
            data.totalPubsSent += p.getnPublications();
        }
        
        return data;
    }
}