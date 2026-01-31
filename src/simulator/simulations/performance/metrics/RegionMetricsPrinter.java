package simulator.simulations.performance.metrics;

import java.util.logging.Logger;
import simulator.config.SimConfiguration;

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

        // Rate = Proportion of Input that was Saved
        double suppressionRate = (totalInput > 0) ? (double) totalSaved / totalInput : 0.0;
        
        // Ratio = Input / Output (How much we compressed the stream)
        double aggFactor = (totalOutput > 0) ? (double) totalInput / totalOutput : (totalInput > 0 ? Double.POSITIVE_INFINITY : 0.0);

        logItem("Total Input Events (Received)", format(totalInput));
        logItem("Total Output Events (Propagated)", format(totalOutput));
        
        if (totalSaved > 0) {
            logItem("Traffic Saved (Geometric Covering)", format(totalSaved));
            // MOVED: Placed contextually next to the absolute number
            // UPDATED: Formatted as % for clarity (e.g., "22.55%")
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
        
        logItem("  -> Covered (Filtered/Suppressed)", 
                format(data.totalInputCovered) + " / " + format(data.totalPropagatedCovered));
        
        logItem("  -> Effective Updates (Forwarded)", 
                format(effectiveInput) + " / " + format(effectiveOutput));
        
        logItem("       -> Expansion Events (Total)", 
                format(data.totalInputExpanded) + " / " + format(data.totalPropagatedExpanded));
        
        logItem("           -> Simple Expansions (Stable)", 
                format(data.totalInputSimpleExpanded) + " / " + format(data.totalPropagatedSimpleExpanded));
        
        logItem("           -> Complex Expansions (Optimizing)", 
                format(data.totalInputComplexExpanded) + " / " + format(data.totalPropagatedComplexExpanded));
        
        logItem("                -> Entries Merged (Count)", 
                format(data.totalInputMerged) + " / " + format(data.totalPropagatedMerged)); 
        
        logItem("                -> Entries Absorbed (Count)", 
                format(data.totalInputAbsorbed) + " / " + format(data.totalPropagatedAbsorbed));
        
        logItem("       -> New Entries Added (Disjoint)", 
                format(data.totalInputAdded) + " / " + format(data.totalPropagatedAdded));
        
        logger.info("");
    }

    @Override
    protected void printRoutingEfficiency(PerformanceMetricsData rawData) {
        RegionPerformanceMetricsData data = (RegionPerformanceMetricsData) rawData;
        logger.info("5. ROUTING OVERHEAD & EFFICIENCY (Regional/Spatial):");
        
        logItem("False Positive Events (Dead Ends)", format(data.totalFalsePositiveEvents));
        
        double fpRate = (data.totalPublicationProcessingEvents > 0) 
                ? ((double) data.totalFalsePositiveEvents / data.totalPublicationProcessingEvents) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Traffic)", String.format("%.2f%%", fpRate));
        
        logItem("False Positive Deliveries (Unwanted)", format(data.totalFalsePositiveDeliveries));
        
        double fpDel = (data.totalDeliveriesReceived > 0) 
                ? ((double) data.totalFalsePositiveDeliveries / data.totalDeliveriesReceived) * 100.0 
                : 0.0;
        logItem(" -> Rate (vs Notifications)", String.format("%.2f%%", fpDel));
        
        // In a matching algorithm, this measures the overhead of the distribution tree.
        double tr = (data.totalDeliveriesReceived > 0) 
                ? (double) data.totalPublicationProcessingEvents / data.totalDeliveriesReceived 
                : 0.0;
        logItem("Traffic Ratio (Multicast Efficiency)", String.format("%.2f", tr));
        logger.info("");
    }

    @Override
    protected void printAccuracy(PerformanceMetricsData rawData) {
        if (!SimConfiguration.get().workload.enableGroundTruth) {
            return;
        }

        RegionPerformanceMetricsData data = (RegionPerformanceMetricsData) rawData;
        if (data.groundTruthMatches <= 0) return;
        
        logger.info("6. ALGORITHM ACCURACY & CORRECTNESS:");
        logItem("Ground Truth Matches", format(data.groundTruthMatches));
        logItem("Total Notifications Received", format(data.totalDeliveriesReceived));
        
        long delta = data.totalDeliveriesReceived - data.groundTruthMatches;
        logItem("Delivery Delta (Raw Gap)", String.format("%+d", delta));

        double acc = (data.groundTruthMatches > 0) 
                ? ((double) data.totalDeliveriesReceived / data.groundTruthMatches) * 100.0 
                : 0.0;
        logItem("Delivery Accuracy (Total / GT)", String.format("%.6f%%", acc)); 
    }
}