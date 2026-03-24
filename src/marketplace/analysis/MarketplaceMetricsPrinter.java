package marketplace.analysis;

import java.util.logging.Logger;
import simulator.simulations.performance.metrics.RegionMetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;

public class MarketplaceMetricsPrinter extends RegionMetricsPrinter {

    public MarketplaceMetricsPrinter(Logger logger) {
        super(logger);
    }

    @Override
    protected void printExtendedTableStats(PerformanceMetricsData rawData) {
        if (rawData instanceof MarketplacePerformanceMetricsData data) {
            logger.info("2b. TIER-BY-TIER BROKER STATE (Marketplace Continuum):");
            logger.info("Level | Brokers | Function Directory Size (Min/Avg/Max) | Spatial Index Size (Min/Avg/Max)");
            logger.info("---------------------------------------------------------------------------------------------");

            for (Integer level : data.tierSpatialIndexStats.keySet()) {
                var spatialStats = data.tierSpatialIndexStats.get(level);
                var functionStats = data.tierFunctionDirectoryStats.get(level);
                
                if (spatialStats.getCount() == 0) continue;

                String funcStr = String.format("%d / %.1f / %d", 
                    functionStats.getMin(), functionStats.getAverage(), functionStats.getMax());
                String spatStr = String.format("%d / %.1f / %d", 
                    spatialStats.getMin(), spatialStats.getAverage(), spatialStats.getMax());
                
                logger.info(String.format("  %-3d |   %-5d | %-33s | %s", 
                    level, spatialStats.getCount(), funcStr, spatStr));
            }
            logger.info(""); // Add a blank line for visual spacing
        }
    }

    @Override
    protected void printAccuracy(PerformanceMetricsData rawData) {
        // Print Level 3 base routing accuracy
        super.printAccuracy(rawData); 
        
        if (rawData instanceof MarketplacePerformanceMetricsData data) {
            if (data.groundTruthMatches <= 0) return;
            
            // Inject blank line for visual separation
            logger.info(""); 
            logger.info("7. MULTI-OBJECTIVE UTILITY ACCURACY (Marketplace):");
            logItem("Optimal Deliveries (Hit Oracle)", format(data.optimalDeliveries));
            logItem("Suboptimal Deliveries (Feasible but Missed)", format(data.suboptimalDeliveries));
            
            logItem("Feasible SLA Violations (Bad Routing)", format(data.feasibleSlaViolations));
            logItem("Unfeasible Dead Ends (No Global Match)", format(data.unfeasibleSlaViolations));
            
            double optimalPercentage = (data.totalDeliveriesReceived > 0) 
                ? ((double) data.optimalDeliveries / data.totalDeliveriesReceived) * 100.0 : 0.0;
            
            logItem("Optimal Match Rate (vs Deliveries)", String.format("%.2f%%", optimalPercentage));
            
            double gapPercentage = data.getAverageUtilityDegradation() * 100.0;
            logItem("Average Optimality Gap (Valid)", String.format("%.2f%% worse than optimal", gapPercentage));
            
            double slaGapPercentage = data.getAverageSlaViolationDegradation() * 100.0;
            logItem("Average SLA Violation Gap (Magnitude)", String.format("%.2f%% worse than optimal", slaGapPercentage));
        }
    }
}