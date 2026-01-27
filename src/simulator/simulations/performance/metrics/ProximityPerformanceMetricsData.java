package simulator.simulations.performance.metrics;

public class ProximityPerformanceMetricsData extends PerformanceMetricsData {

    // --- Upstream Metrics (Network Load) ---
    
    // The actual count of subscription messages sent to the parent (Proxy Subscriptions).
    // This acts as the "Output" for calculating the Aggregation Factor.
    public long totalUpstreamSubscriptionUpdates = 0; 

    // --- Processing Metrics (Brake Strategy) ---
    
    // Valid updates (publisher moved closer) that were suppressed because the 
    // improvement was too small. Measures system stability.
    public long totalBrakeSuppressedEvents = 0; 

    @Override
    public String getAlgorithmLabel() {
        return "Proximity (Light / Closest Node)";
    }
}