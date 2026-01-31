package simulator.simulations.performance.metrics;

import java.util.logging.Logger;
import simulator.config.SimConfiguration;

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
        // In Proximity, we don't "aggregate" regions, we "suppress" state updates (Deduplication)
        long totalSuppressed = (totalInput > totalOutput) ? (totalInput - totalOutput) : 0;
        
        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Events (Received)", format(totalInput));
        logItem("Total Output Events (Propagated)", format(totalOutput));
        
        if (totalSuppressed > 0) {
            logItem("Traffic Saved (State Deduplication)", format(totalSuppressed));
        }
        
        logItem("Aggregation Factor (Deduplication)", (Double.isInfinite(aggFactor)) ? "Infinite" : String.format("%.2f", aggFactor));
        logger.info("");
    }

    // Skipped Section 2 (merged into Section 3)
    @Override
    protected void printProcessingDetails(PerformanceMetricsData data) {
        // No-op
    }

    @Override
    protected void printPublicationTrafficAndCost(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        
        logger.info("3. PUBLICATION TRAFFIC & PROXIMITY LOGIC:");
        
        long processed = data.totalPublicationProcessingEvents;
        long suppressed = data.totalBrakeSuppressedEvents;
        long forwarded = processed - suppressed;

        // 1. Overview
        logItem("Total Publications Sent (Origins)", format(data.totalPublicationsSent));
        
        // 2. Brake Strategy Logic
        logger.info("   --- Flow Control (Brake Strategy) ---");
        logItem("Total Publications Processed (Input)", format(processed));
        logItem("Stopped by Brake (Filtered)", format(suppressed));
        logItem("Actual Forwarded (Upstream/Down)", format(forwarded)); 
        
        // 3. Ratios
        double filterRate = (processed > 0) 
            ? (double) suppressed / processed * 100.0 
            : 0.0;
        logItem("Suppression Rate (Brake)", String.format("%.2f%%", filterRate));
        
        // 4. Computational Cost
        logger.info("   --- Computational Cost ---");
        logItem("Total Matching Computations", format(data.totalMatchingComputations));
        double avg = (processed > 0) ? (double) data.totalMatchingComputations / processed : 0.0;
        logItem("Avg Comparisons per Message", String.format("%.2f", avg));
        logger.info("");
    }

    @Override
    protected void printRoutingEfficiency(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        logger.info("5. ROUTING OVERHEAD & EFFICIENCY:");
        
        logItem("Dead Ends (False Positives)", format(data.totalFalsePositiveEvents));

        double fpRate = (data.totalPublicationProcessingEvents > 0) 
            ? ((double) data.totalFalsePositiveEvents / data.totalPublicationProcessingEvents) * 100.0 
            : 0.0;
        logItem(" -> Rate (vs Total Traffic)", String.format("%.2f%%", fpRate));
        
        // In a search/routing algorithm, this measures "How many nodes do we visit to find 1 subscriber?"
        double tr = (data.totalDeliveriesReceived > 0) ? (double) data.totalPublicationProcessingEvents / data.totalDeliveriesReceived : 0.0;
        logItem("Traffic Ratio (Search Cost Efficiency)", String.format("%.2f", tr));
        logger.info("");
    }

    @Override
    protected void printAccuracy(PerformanceMetricsData rawData) {
        if (!SimConfiguration.get().workload.enableGroundTruth) {
            return;
        }

        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        if (data.groundTruthMatches <= 0 && data.totalDeliveriesReceived <= 0) return;
        
        logger.info("6. ALGORITHM ACCURACY (Recall & Stretch):");
        logItem("Necessary Updates (Ground Truth)", format(data.groundTruthMatches));
        logItem("Actual Notifications Delivered", format(data.totalDeliveriesReceived));
        
        long delta = data.totalDeliveriesReceived - data.groundTruthMatches;
        logItem("Delivery Delta (Actual - GT)", String.format("%+d", delta));

        double deliveryRatio = (data.groundTruthMatches > 0) 
            ? (double) data.totalDeliveriesReceived / data.groundTruthMatches * 100.0 
            : 0.0;
        logItem("Update Recall (Actual/Necessary)", String.format("%.2f%%", deliveryRatio));
        
        logger.info("   --- Stretch Metrics (Delta to Ideal) ---");
        
        if (data.stretchStats.getCount() > 0) {
            logItem("Min Stretch", String.format("%.2f km", data.stretchStats.getMin() * 111.1)); 
            logItem("Max Stretch", String.format("%.2f km", data.stretchStats.getMax() * 111.1));
            logItem("Avg Stretch", String.format("%.2f km", data.stretchStats.getAverage() * 111.1));
        } else {
            logItem("Stretch Metrics", "N/A (No data)");
        }
        logger.info("");
    }
}