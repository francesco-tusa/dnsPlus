package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class MetricsPrinter {

    private final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS");

        printSubscriptionState(data);
        printSubscriptionProcessing(data);
        printPublicationTraffic(data);
        printDeliveryPerformance(data);
        printRoutingEfficiency(data);
        printRegionAccuracy(data);

        printSeparator();
    }

    // ==================================================================================
    // 1. SUBSCRIPTION STATE
    // ==================================================================================
    private void printSubscriptionState(PerformanceMetricsData data) {
        logger.info("1. SUBSCRIPTION STATE (Memory & Aggregation):");
        
        long totalInput = data.inputTableStats.getSum();
        long totalOutput = data.outputTableStats.getSum();
        
        logItem("Total Input Entries (Received)", format(totalInput));
        logItem("Total Output Entries (Propagated)", format(totalOutput));
        
        double aggregationRatio = (totalOutput > 0) 
            ? (double) totalInput / totalOutput 
            : 0.0;
            
        logItem("Aggregation Factor (Input/Output)", String.format("%.2f", aggregationRatio));
        logItem("Avg Input Table Size", String.format("%.2f", data.inputTableStats.getAverage()));
        logItem("Max Input Table Size", format(data.inputTableStats.getMax()));
        logger.info(""); 
    }

    // ==================================================================================
    // 2. SUBSCRIPTION PROCESSING
    // ==================================================================================
    private void printSubscriptionProcessing(PerformanceMetricsData data) {
        logger.info("2. SUBSCRIPTION PROCESSING (Logic) [Input / Output]:");
        logItem("Total Subscriptions Processed", format(data.totalSubscriptionTraffic));
        
        long effectiveUpdatesIn = data.totalSubExpanded + data.totalSubAdded;
        long effectiveUpdatesOut = data.totalOutSubExpanded + data.totalOutSubAdded;

        logSplitItem("  -> Covered (Filtered)", data.totalSubCovered, data.totalOutSubCovered);
        logSplitItem("  -> Effective Updates (Forwarded)", effectiveUpdatesIn, effectiveUpdatesOut);
        logSplitItem("       -> Expansion Events", data.totalSubExpanded, data.totalOutSubExpanded);
        //logger.info("           [Entries Affected by Expansions (Input / Output)]:");
        logSplitItem("           -> Entries Merged", data.totalSubMerged, data.totalOutSubMerged);
        logSplitItem("           -> Entries Absorbed", data.totalSubAbsorbed, data.totalOutSubAbsorbed);
        
        logSplitItem("       -> New Entries Added (Disjoint)", data.totalSubAdded, data.totalOutSubAdded);
        logger.info("");
    }

    // ==================================================================================
    // 3. PUBLICATION TRAFFIC & COST
    // ==================================================================================
    private void printPublicationTraffic(PerformanceMetricsData data) {
        logger.info("3. PUBLICATION TRAFFIC & COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPubsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPubForwardingEvents));
        logItem("Total Matching Computations (Cost)", format(data.totalMatchingComputations));

        double avgCostPerMessage = (data.totalPubForwardingEvents > 0) 
            ? (double) data.totalMatchingComputations / data.totalPubForwardingEvents 
            : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avgCostPerMessage));
        logger.info("");
    }

    // ==================================================================================
    // 4. DELIVERY PERFORMANCE
    // ==================================================================================
    private void printDeliveryPerformance(PerformanceMetricsData data) {
        logger.info("4. DELIVERY PERFORMANCE (Quality):");
        logItem("Total Notifications Received", format(data.totalNotifications));
        
        if (data.hopStats.getCount() > 0) {
            logItem("Hop Count (Min / Avg / Max)", 
                String.format("%d / %.2f / %d", 
                data.hopStats.getMin(), 
                data.hopStats.getAverage(), 
                data.hopStats.getMax()));
        } else {
            logItem("Hop Count", "N/A");
        }
    }

    // ==================================================================================
    // 5. ROUTING EFFICIENCY (Updated)
    // ==================================================================================
    private void printRoutingEfficiency(PerformanceMetricsData data) {
        if (data.totalPubForwardingEvents <= 0) return;

        logger.info("");
        logger.info("5. ROUTING EFFICIENCY:");
        
        // --- Metric A: False Positive Events (Dead Ends) ---
        // Measures Network Efficiency: Did we forward to a broker who had no children?
        long deadEnds = data.totalFalsePositiveEvents;
        double fpEventRate = (double) deadEnds / data.totalPubForwardingEvents * 100.0;

        logItem("False Positive Events (Dead Ends)", format(deadEnds));
        logItem(" -> Rate (vs Traffic)", String.format("%.2f%%", fpEventRate));

        // --- Metric B: False Positive Deliveries (Geometric Mismatch) ---
        // Measures Spatial Index Precision: Did we deliver to a subscriber who didn't want it?
        long fpDeliveries = data.totalFalsePositiveDeliveries;
        double fpDeliveryRate = (data.totalNotifications > 0) 
            ? (double) fpDeliveries / data.totalNotifications * 100.0 
            : 0.0;
        
        // Only show this block if we are actually tracking delivery FPs to avoid cluttering logs 
        // for simulations that don't use Subscriber verification.
        if (fpDeliveries > 0 || data.totalNotifications > 0) {
            logItem("False Positive Deliveries (Unwanted)", format(fpDeliveries));
            logItem(" -> Rate (vs Notifications)", String.format("%.2f%%", fpDeliveryRate));
        }

        // --- Metric C: Overall Traffic Ratio ---
        double trafficRatio = (data.totalNotifications > 0) 
            ? (double) data.totalPubForwardingEvents / data.totalNotifications 
            : 0.0;

        logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", trafficRatio));
    }

    // ==================================================================================
    // 6. REGION ACCURACY
    // ==================================================================================
    private void printRegionAccuracy(PerformanceMetricsData data) {
        if (data.groundTruthMatches > 0) {
            logger.info("");
            logger.info("6. REGION ACCURACY:");
            logItem("Ground Truth Matches", format(data.groundTruthMatches));
            
            // Note: If False Positive Deliveries are tracked correctly, this Accuracy 
            // should ideally align closer to 100% (minus the FPs).
            double accuracy = (double) data.totalNotifications / data.groundTruthMatches * 100.0;
            logItem("Delivery Accuracy (Total / GT)", String.format("%.2f%%", accuracy));
        }
    }

    // ==================================================================================
    // HELPERS
    // ==================================================================================

    private void printBanner(String title) {
        String line = "==================================================================================";
        logger.info("");
        logger.info(line);
        logger.info(String.format("  %s", title));
        logger.info(line);
    }

    private void printSeparator() {
        logger.info("----------------------------------------------------------------------------------");
    }

    private void logItem(String key, String value) {
        logger.info(String.format("%-45s : %s", key, value));
    }
    
    private void logSplitItem(String key, long valIn, long valOut) {
        String value = String.format("%s / %s", format(valIn), format(valOut));
        logger.info(String.format("%-45s : %s", key, value));
    }
    
    private String format(long number) {
        return String.format("%,d", number);
    }
    
    private String format(int number) {
        return String.format("%,d", number);
    }
}