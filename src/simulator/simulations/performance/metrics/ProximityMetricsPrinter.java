package simulator.simulations.performance.metrics;

import java.util.logging.Logger;

public class ProximityMetricsPrinter extends MetricsPrinter {

    public ProximityMetricsPrinter(Logger logger) {
        super(logger);
    }

    @Override
    protected void printSubscriptionTrafficAndAggregation(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        logger.info("1. SUBSCRIPTION TRAFFIC & AGGREGATION:");
        
        long totalInput = data.totalSubscriptionInputEvents;
        long totalOutput = data.totalUpstreamSubscriptionUpdates;
        long totalSuppressed = (totalInput > totalOutput) ? (totalInput - totalOutput) : 0;
        
        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Events (Received)", format(totalInput));
        logItem("Total Output Events (Propagated)", format(totalOutput));
        
        if (totalSuppressed > 0) {
            logItem("Traffic Saved (State Deduplication)", format(totalSuppressed));
        }
        
        logItem("Aggregation Factor", (Double.isInfinite(aggFactor)) ? "Infinite" : String.format("%.2f", aggFactor));
        logger.info("");
    }

    @Override
    protected void printFlowControlDetails(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;

        long processed = data.totalPublicationProcessingEvents;
        long suppressed = data.totalBrakeSuppressedEvents;

        if (suppressed > 0 || processed > 0) {
            logger.info("   --- Flow Control (Brake Strategy) ---");
            logItem("Stopped by Brake (Filtered)", format(suppressed));
            
            double filterRate = (processed > 0) 
                ? (double) suppressed / processed * 100.0 
                : 0.0;
                
            // Indented arrow to show relationship
            logItem(" -> Brake Rejection Rate", String.format("%.2f%%", filterRate));
        }
    }

    @Override
    protected void printAdditionalAccuracyMetrics(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        
        logger.info("   --- Stretch Metrics (Delta to Ideal) ---");
        if (data.stretchStats.getCount() > 0) {
            // Assuming 111.1 conversion factor from degrees to km
            logItem("Min Stretch", String.format("%.2f km", data.stretchStats.getMin() * 111.1)); 
            logItem("Max Stretch", String.format("%.2f km", data.stretchStats.getMax() * 111.1));
            logItem("Avg Stretch", String.format("%.2f km", data.stretchStats.getAverage() * 111.1));
        } else {
            logItem("Stretch Metrics", "N/A (No data)");
        }
    }
}