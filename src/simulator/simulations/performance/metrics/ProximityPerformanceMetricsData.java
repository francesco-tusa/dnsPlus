package simulator.simulations.performance.metrics;

import java.util.DoubleSummaryStatistics;

public class ProximityPerformanceMetricsData extends PerformanceMetricsData {

    // --- Upstream Metrics (Network Load) ---
    public long totalUpstreamSubscriptionUpdates = 0; 

    // --- Processing Metrics (Brake Strategy) ---
    public long totalBrakeSuppressedEvents = 0; 

    // --- Stretch Metrics (Delta between Actual and Ideal Distance) ---
    public final DoubleSummaryStatistics stretchStats = new DoubleSummaryStatistics();

    @Override
    public String getAlgorithmLabel() {
        return "Proximity (Light / Closest Node)";
    }
}