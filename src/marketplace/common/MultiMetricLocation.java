package marketplace.common;

import simulator.core.Location;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Location that also carries QoS metrics for Multi-Metric Routing.
 * Used by MarketplaceProvider (Availability) and MarketplaceClient
 * (Requirements).
 */
public class MultiMetricLocation extends Location {

    private final Map<String, Double> metrics;

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

    /**
     * Calculates a Weighted Euclidean Distance combining spatial distance and
     * metric differences.
     * 
     * Formula:
     * Distance = (spatial_dist_norm)^2 + Sum((metric_diff_norm)^2 * weight)
     * 
     * For complexity's sake in this iteration, we treat the other location's
     * metrics
     * as the "target" or "ideal" and this location's metrics as the "actual" (or
     * vice versa).
     * 
     * NOTE: The standard Location.distanceSquared only does spatial.
     * We override this to include metric "distance" (penalty).
     */
    @Override
    public double distanceSquared(Location other) {
        double spatialDistSq = super.distanceSquared(other);

        // If the other location isn't MultiMetric, we retreat to pure spatial distance.
        if (!(other instanceof MultiMetricLocation)) {
            return spatialDistSq;
        }

        MultiMetricLocation otherMML = (MultiMetricLocation) other;
        double metricPenaltySq = 0.0;

        // Compare metrics that exist in both
        for (Map.Entry<String, Double> entry : this.metrics.entrySet()) {
            String key = entry.getKey();
            if (otherMML.metrics.containsKey(key)) {
                double v1 = entry.getValue();
                double v2 = otherMML.metrics.get(key);
                // Simple difference squared. In a real system, we'd normalize these.
                // e.g. latency diff of 10ms vs cost diff of $10.
                double diff = v1 - v2;
                metricPenaltySq += (diff * diff);
            }
        }

        // We weight spatial vs metric. Let's assume 1 unit of spatial distance
        // is roughly equivalent to 1 unit of metric difference for now.
        return spatialDistSq + metricPenaltySq;
    }

    @Override
    public String toString() {
        return super.toString() + " | metrics=" + metrics;
    }
}
