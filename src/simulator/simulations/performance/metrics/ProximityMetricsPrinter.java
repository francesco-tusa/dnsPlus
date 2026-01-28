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
        long totalFiltered = (totalInput > totalOutput) ? (totalInput - totalOutput) : 0;
        
        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Entries (Received)", format(totalInput));
        logItem("Total Output Entries (Propagated)", format(totalOutput));
        if (totalFiltered > 0) logItem("Traffic Saved (Filtered/Absorbed)", format(totalFiltered));
        logItem("Aggregation Factor (Input/Output)", (Double.isInfinite(aggFactor)) ? "Infinite" : String.format("%.2f", aggFactor));
        logger.info("");
    }

    @Override
    protected void printProcessingDetails(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        logger.info("2. PROXIMITY UPDATE SUPPRESSION (BRAKE STRATEGY):");
        logItem("Total Subscriptions Processed", format(data.totalSubscriptionInputEvents));
        
        logger.info("   -> Propagation Control (Brake Strategy)");
        logItem("      Events Filtered (Suppressed)", format(data.totalBrakeSuppressedEvents));
        
        double filterRate = (data.totalSubscriptionInputEvents > 0) ? (double) data.totalBrakeSuppressedEvents / data.totalSubscriptionInputEvents * 100.0 : 0.0;
        logItem("      Suppression Rate", String.format("%.2f%%", filterRate));
        logger.info("");
    }

    @Override
    protected void printRoutingEfficiency(PerformanceMetricsData rawData) {
        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        logger.info("5. ROUTING OVERHEAD & EFFICIENCY (Proximity/Closest):");
        
        double tr = (data.totalDeliveriesReceived > 0) ? (double) data.totalPublicationProcessingEvents / data.totalDeliveriesReceived : 0.0;
        logItem("Traffic Ratio (Events per Delivery)", String.format("%.2f", tr));
        logger.info("");
    }

    @Override
    protected void printAccuracy(PerformanceMetricsData rawData) {
        if (!SimConfiguration.get().workload.enableGroundTruth) {
            return;
        }

        ProximityPerformanceMetricsData data = (ProximityPerformanceMetricsData) rawData;
        if (data.groundTruthMatches <= 0) return;
        
        logger.info("6. PROXIMITY ALGORITHM ACCURACY:");
        logItem("Necessary Updates", format(data.groundTruthMatches));
        logItem("Actual Notifications", format(data.totalDeliveriesReceived));
        
        long delta = data.totalDeliveriesReceived - data.groundTruthMatches;
        logItem("Delivery Delta (Raw Gap)", String.format("%+d", delta));

        double recall = (data.groundTruthMatches > 0) ? (double) Math.min(data.totalDeliveriesReceived, data.groundTruthMatches) / data.groundTruthMatches * 100.0 : 0.0;
        logItem("Update Recall", String.format("%.6f%%", recall));
    }
}