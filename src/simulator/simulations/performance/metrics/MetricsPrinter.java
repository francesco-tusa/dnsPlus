package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class MetricsPrinter {
    private final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS: " + data.getAlgorithmLabel());
        printSubscriptionTrafficAndAggregation(data);

        if (data instanceof RegionPerformanceMetricsData rd) {
            printRegionProcessingDetails(rd);
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            printProximitySuppressionDetails(pd);
        } else {
            printGenericProcessing(data);
        }

        printPublicationTrafficAndCost(data);
        printDeliveryPathMetrics(data);
        
        if (data instanceof RegionPerformanceMetricsData rd) {
            printRegionRoutingEfficiency(rd);
            printRegionAccuracy(rd);
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            printProximityRoutingEfficiency(pd);
            printProximityAccuracy(pd);
        }
        printSeparator();
    }

    private void printSubscriptionTrafficAndAggregation(PerformanceMetricsData data) { 
        logger.info("1. SUBSCRIPTION TRAFFIC & AGGREGATION:");
        long totalInput = 0, totalOutput = 0, totalFiltered = 0;

        if (data instanceof RegionPerformanceMetricsData rd) {
            totalInput = rd.totalSubCovered + rd.totalSubExpanded + rd.totalSubAdded; 
            // NOTE: Absorbed/Merged are subsets of Expanded, so we don't sum them into TotalInput again.
            // Wait, previous logic summed Absorbed/Merged. 
            // If those were counts of events, it was fine. Now they are entries.
            // The Correct Total Input is: Covered + Expanded + Added.
            
            totalOutput = rd.totalOutSubExpanded + rd.totalOutSubAdded;
            totalFiltered = rd.totalOutSubCovered;
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            totalInput = pd.totalSubscriptionTraffic;
            totalOutput = pd.totalPropagatedSubscriptions;
            totalFiltered = (totalInput > totalOutput) ? (totalInput - totalOutput) : 0;
        } else {
            totalInput = data.totalSubscriptionTraffic;
        }

        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Entries (Received)", format(totalInput));
        logItem("Total Output Entries (Propagated)", format(totalOutput));
        if (totalFiltered > 0) logItem("Traffic Saved (Filtered/Absorbed)", format(totalFiltered));
        logItem("Aggregation Factor (Input/Output)", (Double.isInfinite(aggFactor)) ? "Infinite" : String.format("%.2f", aggFactor));
        logItem("Avg Input Table Size (State)", String.format("%.2f", data.inputTableStats.getAverage()));
        logItem("Max Input Table Size (State)", format(data.inputTableStats.getMax()));
        logger.info("");
    }

    private void printRegionProcessingDetails(RegionPerformanceMetricsData data) {
        logger.info("2. SUBSCRIPTION PROCESSING & OPTIMIZATION DETAILS:");
        
        long totalInput = data.totalSubCovered + data.totalSubExpanded + data.totalSubAdded;
        long effectiveInput = totalInput - data.totalSubCovered;
        long effectiveOutput = data.totalOutSubExpanded + data.totalOutSubAdded;

        logItem("Total Subscriptions Processed", format(totalInput));
        logItem("  -> Covered (Filtered/Suppressed)", format(data.totalSubCovered) + " / " + format(data.totalOutSubCovered));
        logItem("  -> Effective Updates (Forwarded)", format(effectiveInput) + " / " + format(effectiveOutput));
        
        // --- EXPANSION BREAKDOWN (EVENTS) ---
        // Sum check: Simple + Complex = Total Expanded
        logItem("       -> Expansion Events (Total)", 
                format(data.totalSubExpanded) + " / " + format(data.totalOutSubExpanded));
        
        logItem("           -> Simple Expansions (Stable)", 
                format(data.totalSubSimpleExpanded) + " / " + format(data.totalOutSubSimpleExpanded));
        
        logItem("           -> Complex Expansions (Optimizing)", 
                format(data.totalSubComplexExpanded) + " / " + format(data.totalOutSubComplexExpanded));
        
        // --- ENTRY IMPACT (VICTIMS) ---
        // Indented further to show they belong to Complex Expansions
        logItem("                -> Entries Merged (Count)", 
                format(data.totalSubMerged) + " / " + format(data.totalOutSubMerged)); 
        logItem("                -> Entries Absorbed (Count)", 
                format(data.totalSubAbsorbed) + " / " + format(data.totalOutSubAbsorbed));
        
        logItem("       -> New Entries Added (Disjoint)", format(data.totalSubAdded) + " / " + format(data.totalOutSubAdded));
        logger.info("");
    }

    
    private void printProximitySuppressionDetails(ProximityPerformanceMetricsData data) {
        logger.info("2. PROXIMITY UPDATE SUPPRESSION (BRAKE STRATEGY):");
        logItem("Total Subscriptions Processed", format(data.totalSubscriptionTraffic));
        logger.info("   -> Propagation Control (Brake Strategy)");
        logItem("      Events Filtered (Suppressed)", format(data.totalBrakeFilteredEvents));
        double filterRate = (data.totalSubscriptionTraffic > 0) ? (double) data.totalBrakeFilteredEvents / data.totalSubscriptionTraffic * 100.0 : 0.0;
        logItem("      Suppression Rate", String.format("%.2f%%", filterRate));
        logger.info("");
    }
    
    private void printGenericProcessing(PerformanceMetricsData data) {
         if (data.totalSubscriptionTraffic > 0) {
            logger.info("2. PROCESSING LOGIC (GENERIC):");
            logItem("Total Subscriptions Processed", format(data.totalSubscriptionTraffic));
            logger.info("");
        }
    }
    
    private void printPublicationTrafficAndCost(PerformanceMetricsData data) {
        logger.info("3. PUBLICATION TRAFFIC & COMPUTATIONAL COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPubsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPubForwardingEvents));
        logItem("Total Matching Computations (CPU Cost)", format(data.totalMatchingComputations));
        double avg = (data.totalPubForwardingEvents > 0) ? (double) data.totalMatchingComputations / data.totalPubForwardingEvents : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avg));
        logger.info("");
    }
    
    private void printDeliveryPathMetrics(PerformanceMetricsData data) {
        logger.info("4. PUBLICATION DELIVERY & PATH METRICS:");
        logItem("Total Notifications Received", format(data.totalNotifications));
        if (data.totalNotifications > 0) {
            double avgHops = (double)data.totalHopSum / data.totalHopCount;
            logItem("Hop Count (Min / Avg / Max)", data.globalMinHops + " / " + String.format("%.2f", avgHops) + " / " + data.globalMaxHops);
        } else {
            logItem("Hop Count (Min / Avg / Max)", "0 / 0.00 / 0");
        }
        logger.info("");
    }
    
    private void printRegionRoutingEfficiency(RegionPerformanceMetricsData data) {
        logger.info("5. ROUTING OVERHEAD & EFFICIENCY (Regional/Spatial):");
        logItem("False Positive Events (Dead Ends)", format(data.totalFalsePositiveEvents));
        double fpRate = (data.totalPubForwardingEvents > 0) ? ((double) data.totalFalsePositiveEvents / data.totalPubForwardingEvents) * 100.0 : 0.0;
        logItem(" -> Rate (vs Traffic)", String.format("%.2f%%", fpRate));
        logItem("False Positive Deliveries (Unwanted)", format(data.totalFalsePositiveDeliveries));
        double fpDel = (data.totalNotifications > 0) ? ((double) data.totalFalsePositiveDeliveries / data.totalNotifications) * 100.0 : 0.0;
        logItem(" -> Rate (vs Notifications)", String.format("%.2f%%", fpDel));
        double tr = (data.totalNotifications > 0) ? (double) data.totalPubForwardingEvents / data.totalNotifications : 0.0;
        logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", tr));
        logger.info("");
    }
    
    private void printProximityRoutingEfficiency(ProximityPerformanceMetricsData data) {
         logger.info("5. ROUTING OVERHEAD & EFFICIENCY (Proximity/Closest):");
         double tr = (data.totalNotifications > 0) ? (double) data.totalPubForwardingEvents / data.totalNotifications : 0.0;
         logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", tr));
         logger.info("");
    }
    
    private void printRegionAccuracy(RegionPerformanceMetricsData data) {
        if (data.groundTruthMatches <= 0) return;
        logger.info("6. ALGORITHM ACCURACY & CORRECTNESS:");
        logItem("Ground Truth Matches", format(data.groundTruthMatches));
        double acc = (data.groundTruthMatches > 0) ? ((double) data.totalNotifications / data.groundTruthMatches) * 100.0 : 0.0;
        logItem("Delivery Accuracy (Total / GT)", String.format("%.2f%%", acc));
    }

    private void printProximityAccuracy(ProximityPerformanceMetricsData data) {
        if (data.groundTruthMatches <= 0) return;
        logger.info("6. PROXIMITY ALGORITHM ACCURACY:");
        logItem("Necessary Updates", format(data.groundTruthMatches));
        logItem("Actual Notifications", format(data.totalNotifications));
        double recall = (double) Math.min(data.totalNotifications, data.groundTruthMatches) / data.groundTruthMatches * 100.0;
        logItem("Update Recall", String.format("%.2f%%", recall));
    }

    private void printBanner(String t) { logger.info("==================================================================================\n  " + t + "\n=================================================================================="); }
    private void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    private void logItem(String k, String v) { logger.info(String.format("%-45s : %s", k, v)); }
    private String format(long v) { return String.format("%,d", v); }
}