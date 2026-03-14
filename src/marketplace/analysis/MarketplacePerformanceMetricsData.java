package marketplace.analysis;

import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;

public class MarketplacePerformanceMetricsData extends RegionPerformanceMetricsData {
    public long optimalDeliveries = 0;
    public long suboptimalDeliveries = 0;
    public long slaViolations = 0;
    
    public long feasibleSlaViolations = 0;   // The Oracle found a match, but the router picked a bad node
    public long unfeasibleSlaViolations = 0; // The Oracle found NO match in the entire continuum

    // Absolute Delivered Utility
    public double cumulativeDeliveredUtility = 0.0;

    // The accumulated error (Actual Delivered Score - Theoretical Optimal Score)
    public double cumulativeUtilityDegradation = 0.0;

    // The accumulated error for deliveries that breached the SLA contract
    public double cumulativeSlaViolationDegradation = 0.0;
    
    public double getAverageUtilityDegradation() {
        if (suboptimalDeliveries == 0) return 0.0;
        return cumulativeUtilityDegradation / suboptimalDeliveries;
    }

    public double getAverageDeliveredUtility() {
        long totalDeliveries = optimalDeliveries + suboptimalDeliveries;
        if (totalDeliveries == 0) return 0.0;
        return cumulativeDeliveredUtility / totalDeliveries;
    }

    public double getAverageSlaViolationDegradation() {
        if (feasibleSlaViolations == 0) return 0.0;
        return cumulativeSlaViolationDegradation / feasibleSlaViolations;
    }

    public double getUnifiedSystemDegradation() {
        // Total requests that had a globally valid solution
        long totalSolvableRequests = optimalDeliveries + suboptimalDeliveries + feasibleSlaViolations;
        if (totalSolvableRequests == 0)
            return 0.0;
        double totalDegradation = cumulativeUtilityDegradation + cumulativeSlaViolationDegradation;
        return totalDegradation / totalSolvableRequests;
    }

    @Override
    public String getAlgorithmLabel() {
        return "Marketplace (Multi-Objective Optimization)";
    }
}