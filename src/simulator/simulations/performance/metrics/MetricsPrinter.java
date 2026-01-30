package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public abstract class MetricsPrinter {
    
    protected final Logger logger;
    
    public MetricsPrinter(Logger logger) {
        this.logger = logger;
    }
    
    public void print(PerformanceMetricsData data) {
        printBanner("SIMULATION RESULT METRICS: " + data.getAlgorithmLabel());
        printSubscriptionTrafficAndAggregation(data);
        printProcessingDetails(data);
        printTableStats(data);
        printPublicationTrafficAndCost(data);
        printDeliveryPathMetrics(data);
        printRoutingEfficiency(data);
        printAccuracy(data);
        printSeparator();
    }
    
    protected abstract void printSubscriptionTrafficAndAggregation(PerformanceMetricsData data);
    protected abstract void printProcessingDetails(PerformanceMetricsData data);
    protected abstract void printRoutingEfficiency(PerformanceMetricsData data);
    protected abstract void printAccuracy(PerformanceMetricsData data);

    protected void printTableStats(PerformanceMetricsData data) {
        logger.info("2a. BROKER STATE & MEMORY (TABLE SIZES):");
        
        logItem("Input Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", 
                data.inputTableStats.getMin(), 
                data.inputTableStats.getAverage(), 
                data.inputTableStats.getMax()));
            
        // Log Core Table Size with Min/Avg/Max alignment
        // Check count to handle cases with 0 core brokers gracefully
        long coreCount = data.coreInputTableStats.getCount();
        long minCore = coreCount > 0 ? data.coreInputTableStats.getMin() : 0;
        long maxCore = coreCount > 0 ? data.coreInputTableStats.getMax() : 0;
        double avgCore = coreCount > 0 ? data.coreInputTableStats.getAverage() : 0.0;

        logItem("Core Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", minCore, avgCore, maxCore));

        logItem("Output Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", 
                data.outputTableStats.getMin(), 
                data.outputTableStats.getAverage(), 
                data.outputTableStats.getMax()));
        logger.info("");
    }
    
    protected void printPublicationTrafficAndCost(PerformanceMetricsData data) {
        logger.info("3. PUBLICATION TRAFFIC & COMPUTATIONAL COST:");
        logItem("Total Publications Sent (Origins)", format(data.totalPublicationsSent));
        logItem("Total Forwarding Events (Traffic)", format(data.totalPublicationProcessingEvents));
        logItem("Total Matching Computations (CPU Cost)", format(data.totalMatchingComputations));
        double avg = (data.totalPublicationProcessingEvents > 0) ? (double) data.totalMatchingComputations / data.totalPublicationProcessingEvents : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avg));
        logger.info("");
    }

    protected void printDeliveryPathMetrics(PerformanceMetricsData data) {
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
    
    protected void printBanner(String t) { logger.info("==================================================================================\n  " + t + "\n=================================================================================="); }
    protected void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    protected void logItem(String k, String v) { logger.info(String.format("%-45s : %s", k, v)); }
    protected String format(long v) { return String.format("%,d", v); }
}