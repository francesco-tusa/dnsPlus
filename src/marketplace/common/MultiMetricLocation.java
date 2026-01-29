package marketplace.common;

import simulator.core.Location;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Location that also carries QoS metrics for Multi-Metric Routing.
 * Used by MarketplaceProvider (Availability) and MarketplaceClient
 * (Requirements).
 */
public class MultiMetricLocation extends Location {

    private final Map<String, Double> metrics;

    private static final double MAX_LATENCY = 200.0;
    private static final double MAX_COST = 100.0;
    private static final double W_LATENCY = 0.5;
    private static final double W_COST = 0.5;

    // Threshold: Large enough to cover world spatial distance (100x100 = 20,000
    // diag sq)
    // But smaller than the massive penalty for metric mismatch (500,000)
    public static final double MAX_ACCEPTABLE_DISTANCE = 50000.0;

    public MultiMetricLocation(double x, double y, double z, Map<String, Double> metrics) {
        super(x, y, z);
        this.metrics = (metrics != null) ? new HashMap<>(metrics) : Collections.emptyMap();
    }

    public MultiMetricLocation(Location loc, Map<String, Double> metrics) {
        super(loc.getX(), loc.getY(), loc.getZ());
        this.metrics = (metrics != null) ? new HashMap<>(metrics) : Collections.emptyMap();
    }

    public Map<String, Double> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    @Override
    public double distanceSquared(Location other) {
        double spatialDistSq = super.distanceSquared(other); // This represents Network Latency

        if (!(other instanceof MultiMetricLocation))
            return spatialDistSq;

        MultiMetricLocation otherMML = (MultiMetricLocation) other;
        double metricPenalty = 0.0;

        // Latency Component (Intrinsic)
        if (this.metrics.containsKey("latency") && otherMML.metrics.containsKey("latency")) {
            double val = this.metrics.get("latency") - otherMML.metrics.get("latency");
            // Normalize: (20ms / 200ms)^2 * 0.5
            double norm = val / MAX_LATENCY;
            metricPenalty += (norm * norm) * W_LATENCY;
        }

        // Cost Component
        if (this.metrics.containsKey("cost") && otherMML.metrics.containsKey("cost")) {
            double val = this.metrics.get("cost") - otherMML.metrics.get("cost");
            double norm = val / MAX_COST;
            metricPenalty += (norm * norm) * W_COST;
        }

        // Combine: Pure Distance + QoS Penalty
        // Scale factor INCREASED to 1,000,000 so Metrics dominate Spatial (Max spatial
        // ~20,000)
        return spatialDistSq + (metricPenalty * 1000000.0); // Scale factor to make metrics visible
    }

    @Override
    public String toString() {
        return super.toString() + " | metrics=" + metrics;
    }
}
