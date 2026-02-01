package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class RegionMetricsPrinter extends MetricsPrinter {

    public RegionMetricsPrinter(Logger logger) {
        super(logger);
    }

    @Override
    protected void printSubscriptionTrafficAndAggregation(PerformanceMetricsData rawData) {
        RegionPerformanceMetricsData data = (RegionPerformanceMetricsData) rawData;
        logger.info("1. SUBSCRIPTION TRAFFIC & AGGREGATION:");

        long totalInput = data.totalInputCovered + data.totalInputExpanded + data.totalInputAdded;
        long totalOutput = data.totalPropagatedExpanded + data.totalPropagatedAdded;
        long totalSaved = data.totalInputCovered + data.totalPropagatedCovered;

        double suppressionRate = (totalInput > 0) ? (double) totalSaved / totalInput : 0.0;
        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Events (Received)", format(totalInput));
        logItem("Total Output Events (Propagated)", format(totalOutput));
        
        if (totalSaved > 0) {
            logItem("Traffic Saved (Geometric Covering)", format(totalSaved));
            logItem(" -> Suppression Rate", String.format("%.2f%%", suppressionRate * 100.0));
        }
        
        logItem("Aggregation Factor (Compression)", (Double.isInfinite(aggFactor)) ? "Infinite" : String.format("%.2f", aggFactor));
        logger.info("");
    }

    @Override
    protected void printProcessingDetails(PerformanceMetricsData rawData) {
        RegionPerformanceMetricsData data = (RegionPerformanceMetricsData) rawData;
        logger.info("2. SUBSCRIPTION PROCESSING & OPTIMIZATION DETAILS:");
        
        long totalInput = data.totalInputCovered + data.totalInputExpanded + data.totalInputAdded;
        long effectiveInput = totalInput - data.totalInputCovered;
        long effectiveOutput = data.totalPropagatedExpanded + data.totalPropagatedAdded;

        logItem("Total Subscriptions Processed", format(totalInput));
        logItem("  -> Covered (Filtered/Suppressed)", format(data.totalInputCovered) + " / " + format(data.totalPropagatedCovered));
        logItem("  -> Effective Updates (Forwarded)", format(effectiveInput) + " / " + format(effectiveOutput));
        logItem("       -> Expansion Events (Total)", format(data.totalInputExpanded) + " / " + format(data.totalPropagatedExpanded));
        logItem("           -> Simple Expansions", format(data.totalInputSimpleExpanded) + " / " + format(data.totalPropagatedSimpleExpanded));
        logItem("           -> Complex Expansions", format(data.totalInputComplexExpanded) + " / " + format(data.totalPropagatedComplexExpanded));
        logItem("                -> Entries Merged", format(data.totalInputMerged) + " / " + format(data.totalPropagatedMerged)); 
        logItem("                -> Entries Absorbed", format(data.totalInputAbsorbed) + " / " + format(data.totalPropagatedAbsorbed));
        logItem("       -> New Entries Added", format(data.totalInputAdded) + " / " + format(data.totalPropagatedAdded));
        logger.info("");
    }

    @Override
    protected void printFlowControlDetails(PerformanceMetricsData rawData) { } // currently no flow control

    @Override
    protected void printAdditionalRoutingMetrics(PerformanceMetricsData rawData) {
        RegionPerformanceMetricsData data = (RegionPerformanceMetricsData) rawData;
        
        logItem("False Positive Deliveries (Unwanted)", format(data.totalFalsePositiveDeliveries));
        double fpDel = (data.totalDeliveriesReceived > 0) 
                ? ((double) data.totalFalsePositiveDeliveries / data.totalDeliveriesReceived) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Notifications)", String.format("%.2f%%", fpDel));
    }
}