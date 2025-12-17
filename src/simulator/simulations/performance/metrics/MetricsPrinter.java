package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class MetricsPrinter {

    private final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS");

        logger.info("1. SUBSCRIPTION STATE (Memory & Aggregation):");
        
        long totalInput = data.inputTableStats.getSum();
        long totalOutput = data.outputTableStats.getSum();
        
        logItem("Total Input Entries (Received)", format(totalInput));
        logItem("Total Output Entries (Propagated)", format(totalOutput));
        
        // Calculate Aggregation Ratio
        double aggregationRatio = (totalOutput > 0) 
            ? (double) totalInput / totalOutput 
            : 0.0;
            
        logItem("Aggregation Factor (Input/Output)", String.format("%.2f", aggregationRatio));
        
        logItem("Avg Input Table Size", String.format("%.2f", data.inputTableStats.getAverage()));
        logItem("Max Input Table Size", format(data.inputTableStats.getMax()));

        logger.info(""); 
        logger.info("2. SUBSCRIPTION PROCESSING (Logic):");
        logItem("Total Subscriptions Processed", format(data.totalSubscriptionTraffic));
        
        long effectiveUpdates = data.totalSubExpanded + data.totalSubAdded;
        logItem("  -> Covered (Filtered)", format(data.totalSubCovered));
        logItem("   -> Effective Updates (Forwarded)", format(effectiveUpdates));
        logItem("       -> Expansion Events", format(data.totalSubExpanded));
        logger.info("           [Entries Affected by Expansions]:");
        logItem("           -> Entries Merged (contributed to growth)", format(data.totalSubMerged));
        logItem("           -> Entries Absorbed (removed as subset)", format(data.totalSubAbsorbed));
        logItem("       -> New Entries Added (Disjoint)", format(data.totalSubAdded));

        logger.info("");
        logger.info("3. PUBLICATION TRAFFIC & COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPubsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPubForwardingEvents));
        logItem("Total Matching Computations (Cost)", format(data.totalMatchingComputations));

        double avgCostPerMessage = (data.totalPubForwardingEvents > 0) 
            ? (double) data.totalMatchingComputations / data.totalPubForwardingEvents 
            : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avgCostPerMessage));

        logger.info("");
        logger.info("4. DELIVERY PERFORMANCE (Quality):");
        logItem("Total Notifications Received", format(data.totalNotifications));
        
        if (data.hopStats.getCount() > 0) {
            logItem("Hop Count (Min / Avg / Max)", 
                String.format("%d / %.2f / %d", data.hopStats.getMin(), data.hopStats.getAverage(), data.hopStats.getMax()));
        } else {
            logItem("Hop Count", "N/A");
        }

        if (data.totalPubForwardingEvents > 0) {
            logger.info("");
            logger.info("5. ROUTING EFFICIENCY:");
            
            long deadEnds = data.totalFalsePositiveEvents;
            double fpRate = (double) deadEnds / data.totalPubForwardingEvents * 100.0;
            
            double trafficRatio = (data.totalNotifications > 0) 
                ? (double) data.totalPubForwardingEvents / data.totalNotifications 
                : 0.0;

            logItem("False Positive Events (Dead Ends)", format(deadEnds));
            logItem("False Positive Rate", String.format("%.2f%%", fpRate));
            logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", trafficRatio));
        }
        
        if (data.groundTruthMatches > 0) {
            logger.info("");
            logger.info("6. REGION ACCURACY:");
            logItem("Ground Truth Matches", format(data.groundTruthMatches));
            double accuracy = (double) data.totalNotifications / data.groundTruthMatches * 100.0;
            logItem("Delivery Accuracy", String.format("%.2f%%", accuracy));
        }

        printSeparator();
    }

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
        logger.info(String.format("%-40s : %s", key, value));
    }
    
    private String format(long number) {
        return String.format("%,d", number);
    }
    
    private String format(int number) {
        return String.format("%,d", number);
    }
}