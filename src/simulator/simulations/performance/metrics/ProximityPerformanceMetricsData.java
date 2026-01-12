package simulator.simulations.performance.metrics;

public class ProximityPerformanceMetricsData extends PerformanceMetricsData {

    // Unique to Proximity: Brake Strategy Efficiency
    public long totalBrakeFilteredEvents = 0;

    // Track actual upstream subscriptions to verify aggregation
    public long totalPropagatedSubscriptions = 0;

    // Track actual downstream message volume (Fan-out)
    public long totalMessagesForwarded = 0;

    @Override
    public String getAlgorithmLabel() {
        return "PROXIMITY (Closest Node)";
    }
}