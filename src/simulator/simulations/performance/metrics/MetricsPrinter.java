package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public abstract class MetricsPrinter {
    protected final Logger logger;

    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }

    // --- TEMPLATE METHOD: Defines the Report Structure ---
    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS: " + data.getAlgorithmLabel());
        
        // Hook 1: Input/Output logic is different for every algo
        printSubscriptionTrafficAndAggregation(data);

        // Hook 2: Processing logic (Optimization vs Brake)
        printProcessingDetails(data);

        // Shared: Memory/State Stats
        printTableStats(data);

        // Shared: Pub Traffic & Cost
        printPublicationTrafficAndCost(data);
        
        // Shared: Delivery Hops
        printDeliveryPathMetrics(data);
        
        // Hook 3: Efficiency (Multicast vs Search)
        printRoutingEfficiency(data);
        
        // Hook 4: Accuracy (Ground Truth)
        printAccuracy(data);
        
        printSeparator();
    }

    // --- ABSTRACT HOOKS ---
    protected abstract void printSubscriptionTrafficAndAggregation(PerformanceMetricsData data);
    protected abstract void printProcessingDetails(PerformanceMetricsData data);
    protected abstract void printRoutingEfficiency(PerformanceMetricsData data);
    protected abstract void printAccuracy(PerformanceMetricsData data);

    // --- SHARED METHODS ---
    private void printTableStats(PerformanceMetricsData data) {
        logger.info("2a. BROKER STATE & MEMORY (TABLE SIZES):");
        logItem("Input Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", data.inputTableStats.getMin(), data.inputTableStats.getAverage(), data.inputTableStats.getMax()));
        logItem("Output Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", data.outputTableStats.getMin(), data.outputTableStats.getAverage(), data.outputTableStats.getMax()));
        logger.info("");
    }
    
    private void printPublicationTrafficAndCost(PerformanceMetricsData data) {
        logger.info("3. PUBLICATION TRAFFIC & COMPUTATIONAL COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPublicationsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPublicationProcessingEvents));
        logItem("Total Matching Computations (CPU Cost)", format(data.totalMatchingComputations));
        double avg = (data.totalPublicationProcessingEvents > 0) ? (double) data.totalMatchingComputations / data.totalPublicationProcessingEvents : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avg));
        logger.info("");
    }
    
    private void printDeliveryPathMetrics(PerformanceMetricsData data) {
        logger.info("4. PUBLICATION DELIVERY & PATH METRICS:");
        logItem("Total Notifications Received", format(data.totalDeliveriesReceived));
        if (data.totalDeliveriesReceived > 0) {
            double avgHops = (double)data.totalHopSum / data.totalDeliveriesReceived;
            logItem("Hop Count (Min / Avg / Max)", data.globalMinHops + " / " + String.format("%.2f", avgHops) + " / " + data.globalMaxHops);
        } else {
            logItem("Hop Count (Min / Avg / Max)", "0 / 0.00 / 0");
        }
        logger.info("");
    }

    // --- UTILS ---
    protected void printBanner(String t) { logger.info("==================================================================================\n  " + t + "\n=================================================================================="); }
    protected void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    protected void logItem(String k, String v) { logger.info(String.format("%-45s : %s", k, v)); }
    protected String format(long v) { return String.format("%,d", v); }
}