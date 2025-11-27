package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class MetricsPrinter {

    private final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS");

        logger.info("1. SUBSCRIPTION STATE (Memory):");
        logItem("Total Table Entries (Network)", data.tableSizeStats.getSum());
        logItem("Max Table Size (Single Broker)", data.tableSizeStats.getMax());
        logItem("Avg Table Size", String.format("%.2f", data.tableSizeStats.getAverage()));

        logger.info(""); 
        logger.info("2. SUBSCRIPTION PROCESSING (Logic):");
        logItem("Total Subscriptions Processed", data.totalSubscriptionTraffic);
        logItem("  -> Covered (Filtered)", data.totalSubCovered);
        logItem("  -> Expanded (Merged)", data.totalSubExpanded);
        logItem("  -> Added (Disjoint)", data.totalSubAdded);

        logger.info("");
        logger.info("3. PUBLICATION TRAFFIC & COST:");
        logItem("Total Publications Sent (Origins)", data.totalPubsSent);
        logItem("Total Forwarding Events (Traffic)", data.totalPubForwardingEvents);
        logItem("Total Matching Computations (Cost)", data.totalMatchingComputations);

        double avgCostPerMessage = (data.totalPubForwardingEvents > 0) 
            ? (double) data.totalMatchingComputations / data.totalPubForwardingEvents 
            : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avgCostPerMessage));

        logger.info("");
        logger.info("4. DELIVERY PERFORMANCE (Quality):");
        logItem("Total Notifications Received", data.totalNotifications);
        
        if (data.hopStats.getCount() > 0) {
            logItem("Hop Count (Min / Avg / Max)", 
                String.format("%d / %.2f / %d", data.hopStats.getMin(), data.hopStats.getAverage(), data.hopStats.getMax()));
        } else {
            logItem("Hop Count", "N/A");
        }

        if (data.totalPubForwardingEvents > 0) {
            logger.info("");
            logger.info("5. ROUTING EFFICIENCY:");
            
            // Count of messages that hit a dead end (aggregated region was too big)
            long deadEnds = data.totalFalsePositiveEvents;
            double fpRate = (double) deadEnds / data.totalPubForwardingEvents * 100.0;
            
            // Events per Delivery: Lower is better. 1.0 is ideal unicast. Multicast can be >1 or <1 depending on fanout.
            // Ideally compared against the "Perfect" routing scenario.
            double trafficRatio = (data.totalNotifications > 0) 
                ? (double) data.totalPubForwardingEvents / data.totalNotifications 
                : 0.0;

            logItem("False Positive Events (Dead Ends)", deadEnds);
            logItem("False Positive Rate", String.format("%.2f%%", fpRate));
            logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", trafficRatio));
        }
        
        if (data.groundTruthMatches > 0) {
            logger.info("");
            logger.info("6. REGION ACCURACY:");
            logItem("Ground Truth Matches", data.groundTruthMatches);
            double accuracy = (double) data.totalNotifications / data.groundTruthMatches * 100.0;
            logItem("Delivery Accuracy", String.format("%.2f%%", accuracy));
        }

        printSeparator();
    }

    private void printBanner(String title) {
        String line = "==================================================================================";
        logger.info(line);
        logger.info(String.format("  %s", title));
        logger.info(line);
    }

    private void printSeparator() {
        logger.info("----------------------------------------------------------------------------------");
    }

    private void logItem(String key, Object value) {
        logger.info(String.format("%-40s : %s", key, value));
    }
}