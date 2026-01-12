package simulator.simulations.performance.metrics;

public class ProximityPerformanceMetricsData extends PerformanceMetricsData {

    // Unique to Proximity: Brake Strategy Efficiency
    public long totalBrakeFilteredEvents = 0;

    @Override
    public String getAlgorithmLabel() {
        return "PROXIMITY (Closest Node)";
    }
}