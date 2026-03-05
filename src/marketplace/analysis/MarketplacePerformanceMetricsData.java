package marketplace.analysis;

import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;

public class MarketplacePerformanceMetricsData extends RegionPerformanceMetricsData {
    public long optimalDeliveries = 0;
    public long suboptimalDeliveries = 0;
    public long slaViolations = 0;
    
    // Absolute Delivered Utility
    public double cumulativeDeliveredUtility = 0.0;
    // The accumulated error (Actual Delivered Score - Theoretical Optimal Score)
    public double cumulativeUtilityDegradation = 0.0;
    
    public double getAverageUtilityDegradation() {
        if (suboptimalDeliveries == 0) return 0.0;
        return cumulativeUtilityDegradation / suboptimalDeliveries;
    }

    public double getAverageDeliveredUtility() {
        long totalDeliveries = optimalDeliveries + suboptimalDeliveries;
        if (totalDeliveries == 0) return 0.0;
        return cumulativeDeliveredUtility / totalDeliveries;
    }

    @Override
    public String getAlgorithmLabel() {
        return "Marketplace (Multi-Objective Optimization)";
    }
}