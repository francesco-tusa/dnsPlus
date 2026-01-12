package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class MetricsPrinter {
    private final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS: " + data.getAlgorithmLabel());

        // 1. Common Subscription State (Polymorphic calculation)
        printSubscriptionState(data);

        // 2. Algorithm Specific Processing Logic
        if (data instanceof RegionPerformanceMetricsData rd) {
            printRegionProcessing(rd);
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            printProximityProcessing(pd);
        } else {
            // Fallback for unknown types
            printGenericProcessing(data);
        }

        // 3. Traffic (Common)
        printPublicationTraffic(data);

        // 4. Delivery (Common)
        printDeliveryPerformance(data);
        
        // 5. Routing Efficiency (Common)
        printRoutingEfficiency(data);

        // 6. Algorithm Specific Accuracy
        if (data instanceof RegionPerformanceMetricsData rd) {
            printRegionAccuracy(rd);
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            printProximityAccuracy(pd);
        }

        printSeparator();
    }

    // --- Specific Printers ---

    private void printSubscriptionState(PerformanceMetricsData data) { 
        logger.info("1. SUBSCRIPTION STATE (Memory & Aggregation):");
        
        long totalInput = 0;
        long totalOutput = 0;

        // Calculate Totals based on specific type
        if (data instanceof RegionPerformanceMetricsData rd) {
            totalInput = rd.totalSubCovered + rd.totalSubAbsorbed + rd.totalSubMerged + rd.totalSubExpanded + rd.totalSubAdded;
            totalOutput = rd.totalOutSubCovered + rd.totalOutSubExpanded + rd.totalOutSubAdded + rd.totalOutSubAbsorbed + rd.totalOutSubMerged;
        } else if (data instanceof ProximityPerformanceMetricsData pd) {
            // For Proximity: Input is total traffic, Output is traffic NOT suppressed by brakes
            totalInput = pd.totalSubscriptionTraffic;
            totalOutput = pd.totalSubscriptionTraffic - pd.totalBrakeFilteredEvents;
        } else {
            // Default/Fallback
            totalInput = data.totalSubscriptionTraffic;
            totalOutput = 0; 
        }

        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : 0.0;

        logItem("Total Input Entries (Received)", format(totalInput));
        logItem("Total Output Entries (Propagated)", format(totalOutput));
        logItem("Aggregation Factor (Input/Output)", String.format("%.2f", aggFactor));
        logItem("Avg Input Table Size", String.format("%.2f", data.inputTableStats.getAverage()));
        logItem("Max Input Table Size", format(data.inputTableStats.getMax()));
        logger.info("");
    }

    private void printRegionProcessing(RegionPerformanceMetricsData data) {
        logger.info("2. SUBSCRIPTION PROCESSING (Logic) [Input / Output]:");
        
        long totalInput = data.totalSubCovered + data.totalSubAbsorbed + data.totalSubMerged + data.totalSubExpanded + data.totalSubAdded;
        
        // Calculate Effective Input/Output (Forwarded events)
        long effectiveInput = totalInput - data.totalSubCovered;
        long effectiveOutput = data.totalOutSubExpanded + data.totalOutSubAdded + data.totalOutSubMerged + data.totalOutSubAbsorbed; 

        logItem("Total Subscriptions Processed", format(totalInput));
        
        // Format: "InputVal / OutputVal"
        logItem("  -> Covered (Filtered)", 
                format(data.totalSubCovered) + " / " + format(data.totalOutSubCovered));
        
        logItem("  -> Effective Updates (Forwarded)", 
                format(effectiveInput) + " / " + format(effectiveOutput));
        
        logItem("       -> Expansion Events", 
                format(data.totalSubExpanded + data.totalSubMerged + data.totalSubAbsorbed) + " / " + 
                format(data.totalOutSubExpanded + data.totalOutSubMerged + data.totalOutSubAbsorbed));
        
        logItem("           -> Entries Merged", 
                format(data.totalSubMerged) + " / " + format(data.totalOutSubMerged)); 

        logItem("           -> Entries Absorbed", 
                format(data.totalSubAbsorbed) + " / " + format(data.totalOutSubAbsorbed));
        
        logItem("       -> New Entries Added (Disjoint)", 
                format(data.totalSubAdded) + " / " + format(data.totalOutSubAdded));
        
        logger.info("");
    }

    private void printProximityProcessing(ProximityPerformanceMetricsData data) {
        logger.info("2. PROCESSING LOGIC (PROXIMITY):");
        logItem("Total Subscriptions Processed", format(data.totalSubscriptionTraffic));
        
        logger.info("   -> Propagation Control (Brake Strategy)");
        logItem("      Events Filtered (Suppressed)", format(data.totalBrakeFilteredEvents));
        
        long totalAttempts = data.totalSubscriptionTraffic; 
        // Note: In collector, totalSubscriptionTraffic is total events. Filtered are a subset.
        
        double filterRate = (totalAttempts > 0) 
            ? (double) data.totalBrakeFilteredEvents / totalAttempts * 100.0 
            : 0.0;
        
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

    private void printPublicationTraffic(PerformanceMetricsData data) {
        logger.info("3. PUBLICATION TRAFFIC & COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPubsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPubForwardingEvents));
        logItem("Total Matching Computations (Cost)", format(data.totalMatchingComputations));
        
        double avgComparisons = (data.totalPubForwardingEvents > 0) 
                ? (double) data.totalMatchingComputations / data.totalPubForwardingEvents 
                : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avgComparisons));
        logger.info("");
    }

    private void printDeliveryPerformance(PerformanceMetricsData data) {
        logger.info("4. DELIVERY PERFORMANCE (Quality):");
        logItem("Total Notifications Received", format(data.totalNotifications));
        
        if (data.totalNotifications > 0) {
            double avgHops = (double)data.totalHopSum / data.totalHopCount;
            logItem("Hop Count (Min / Avg / Max)", 
                    data.globalMinHops + " / " + String.format("%.2f", avgHops) + " / " + data.globalMaxHops);
        } else {
            logItem("Hop Count (Min / Avg / Max)", "0 / 0.00 / 0");
        }
        logger.info("");
    }

    private void printRoutingEfficiency(PerformanceMetricsData data) {
        logger.info("5. ROUTING EFFICIENCY:");
        
        logItem("False Positive Events (Dead Ends)", format(data.totalFalsePositiveEvents));
        double fpRateEvents = (data.totalPubForwardingEvents > 0) 
                ? ((double) data.totalFalsePositiveEvents / data.totalPubForwardingEvents) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Traffic)", String.format("%.2f%%", fpRateEvents));

        logItem("False Positive Deliveries (Unwanted)", format(data.totalFalsePositiveDeliveries));
        double fpRateDelivery = (data.totalNotifications > 0) 
                ? ((double) data.totalFalsePositiveDeliveries / data.totalNotifications) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Notifications)", String.format("%.2f%%", fpRateDelivery));

        double trafficRatio = (data.totalNotifications > 0) 
                ? (double) data.totalPubForwardingEvents / data.totalNotifications 
                : 0.0;
        logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", trafficRatio));
        logger.info("");
    }
    
    private void printRegionAccuracy(RegionPerformanceMetricsData data) {
        if (data.groundTruthMatches <= 0) return;
        logger.info("6. REGION ACCURACY:");
        logItem("Ground Truth Matches", format(data.groundTruthMatches));
        
        double accuracy = (data.groundTruthMatches > 0) 
                ? ((double) data.totalNotifications / data.groundTruthMatches) * 100.0 
                : 0.0;
        logItem("Delivery Accuracy (Total / GT)", String.format("%.2f%%", accuracy));
    }

    private void printProximityAccuracy(ProximityPerformanceMetricsData data) {
        if (data.groundTruthMatches <= 0) return;
        
        logger.info("6. PROXIMITY ALGORITHM ACCURACY:");
        logger.info("   (Ground Truth = 'Necessary Updates' strictly improving distance)");
        
        logItem("Necessary Updates", format(data.groundTruthMatches));
        logItem("Actual Notifications", format(data.totalNotifications));
        
        double recall = (double) Math.min(data.totalNotifications, data.groundTruthMatches) / data.groundTruthMatches * 100.0;
        logItem("Update Recall", String.format("%.2f%%", recall));

        if (data.totalNotifications > data.groundTruthMatches) {
            long noise = data.totalNotifications - data.groundTruthMatches;
            double noiseRate = (double) noise / data.totalNotifications * 100.0;
            logItem("Inefficient Updates (Noise)", format(noise));
            logItem(" -> Noise Rate", String.format("%.2f%%", noiseRate));
        }
    }

    // --- Helpers ---
    private void printBanner(String t) { logger.info("==================================================================================\n  " + t + "\n=================================================================================="); }
    private void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    private void logItem(String k, String v) { logger.info(String.format("%-45s : %s", k, v)); }
    private String format(long v) { return String.format("%,d", v); }
}