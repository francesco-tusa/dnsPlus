// File: src/marketplace/analysis/metrics/MarketplaceMetricsPrinter.java
package marketplace.analysis;

import java.util.logging.Logger;
import simulator.simulations.performance.metrics.RegionMetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;

public class MarketplaceMetricsPrinter extends RegionMetricsPrinter {

    public MarketplaceMetricsPrinter(Logger logger) {
        super(logger);
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
            
            double optimalPercentage = (data.totalDeliveriesReceived > 0) 
                ? ((double) data.optimalDeliveries / data.totalDeliveriesReceived) * 100.0 : 0.0;
            
            logItem("Optimal Match Rate (vs Deliveries)", String.format("%.2f%%", optimalPercentage));
            
            double gapPercentage = data.getAverageUtilityDegradation() * 100.0;
            logItem("Average Optimality Gap (Stretch)", String.format("%.2f%% worse than optimal", gapPercentage));
        }
    }
}