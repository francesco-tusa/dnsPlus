package simulator.simulations.performance.metrics;

import java.util.logging.Logger;
import simulator.config.SimConfiguration;

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
    
    // --- Abstract Hooks ---
    
    protected abstract void printSubscriptionTrafficAndAggregation(PerformanceMetricsData data);
    protected abstract void printFlowControlDetails(PerformanceMetricsData data);

    protected void printProcessingDetails(PerformanceMetricsData data) {}
    protected void printAdditionalRoutingMetrics(PerformanceMetricsData data) {}
    protected void printAdditionalAccuracyMetrics(PerformanceMetricsData data) {}

    // --- Common Logic Implementations ---

    protected void printTableStats(PerformanceMetricsData data) {
        logger.info("2a. BROKER STATE & MEMORY (TABLE SIZES):");
        
        logItem("Input Table Size (Min / Avg / Max)", 
            String.format("%d / %.1f / %d", 
                data.inputTableStats.getMin(), 
                data.inputTableStats.getAverage(), 
                data.inputTableStats.getMax()));
            
        long coreCount = data.coreInputTableStats.getCount();
        long minCore = coreCount > 0 ? data.coreInputTableStats.getMin() : 0;
        long maxCore = coreCount > 0 ? data.coreInputTableStats.getMax() : 0;
        double avgCore = coreCount > 0 ? data.coreInputTableStats.getAverage() : 0.0;

        logItem("Core Input Table Size (Min / Avg / Max)", 
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
        logItem("Total Publications Processed (Compute Load)", format(data.totalPublicationProcessingEvents));

        printFlowControlDetails(data);

        logItem("Total Publications Forwarded (Network Load)", format(data.totalPublicationsForwarded));

        logger.info("   --- Computational Cost ---");
        logItem("Total Matching Computations (Compute Load)", format(data.totalMatchingComputations));
        
        double avg = (data.totalPublicationProcessingEvents > 0) 
            ? (double) data.totalMatchingComputations / data.totalPublicationProcessingEvents 
            : 0.0;
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
    
    protected void printRoutingEfficiency(PerformanceMetricsData data) {
        logger.info("5. ROUTING OVERHEAD & EFFICIENCY:");
        
        // 1. Common Metric: Dead Ends
        logItem("False Positive Events (Dead Ends)", format(data.totalFalsePositiveEvents));
        double fpRate = (data.totalPublicationProcessingEvents > 0) 
                ? ((double) data.totalFalsePositiveEvents / data.totalPublicationProcessingEvents) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Traffic)", String.format("%.2f%%", fpRate));
        
        // 2. Hook for specifics (e.g. Region FP Deliveries)
        printAdditionalRoutingMetrics(data);
        
        // 3. Common Metric: Traffic Ratio
        double tr = (data.totalDeliveriesReceived > 0) 
                ? (double) data.totalPublicationProcessingEvents / data.totalDeliveriesReceived 
                : 0.0;
        logItem("Traffic Ratio (Cost Efficiency)", String.format("%.2f", tr));
        logger.info("");
    }

    protected void printAccuracy(PerformanceMetricsData data) {
        if (!SimConfiguration.get().workload.enableGroundTruth) {
            return;
        }

        // Common Guard clause
        if (data.groundTruthMatches <= 0 && data.totalDeliveriesReceived <= 0) return;
        
        logger.info("6. ALGORITHM ACCURACY & CORRECTNESS:");
        logItem("Necessary Updates (Ground Truth)", format(data.groundTruthMatches));
        logItem("Actual Notifications Delivered", format(data.totalDeliveriesReceived));
        
        long delta = data.totalDeliveriesReceived - data.groundTruthMatches;
        logItem("Delivery Delta (Actual - GT)", String.format("%+d", delta));

        double acc = (data.groundTruthMatches > 0) 
                ? ((double) data.totalDeliveriesReceived / data.groundTruthMatches) * 100.0 
                : 0.0;
        
        // Label differs slightly in context (Accuracy vs Recall) but math is identical
        logItem("Delivery Accuracy (Recall)", String.format("%.6f%%", acc)); 

        // Hook for specifics (e.g. Proximity Stretch)
        printAdditionalAccuracyMetrics(data);
    }
    
    // --- Helpers ---
    protected void printBanner(String t) { logger.info("==================================================================================\n  " + t + "\n=================================================================================="); }
    protected void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    protected void logItem(String k, String v) { logger.info(String.format("%-45s : %s", k, v)); }
    protected String format(long v) { return String.format("%,d", v); }
}