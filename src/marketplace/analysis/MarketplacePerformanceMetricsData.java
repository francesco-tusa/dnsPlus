package marketplace.analysis;

import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;

public class MarketplacePerformanceMetricsData extends RegionPerformanceMetricsData {
    public long optimalDeliveries = 0;
    public long suboptimalDeliveries = 0;
    
    // The accumulated error (Actual Delivered Score - Theoretical Optimal Score)
    public double cumulativeUtilityDegradation = 0.0;
    
    public double getAverageUtilityDegradation() {
        if (suboptimalDeliveries == 0) return 0.0;
        return cumulativeUtilityDegradation / suboptimalDeliveries;
    }

    @Override
    public String getAlgorithmLabel() {
        return "Marketplace (Multi-Objective Optimization)";
    }
}