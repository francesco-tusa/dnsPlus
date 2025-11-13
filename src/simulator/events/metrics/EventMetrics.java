package simulator.events.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * A composite class that holds the shared, mutable metric data (hops and path)
 * for a simulation event. An instance of this class is created once by the 
 * original event and its reference is passed to all subsequent copies.
 */
public class EventMetrics {
    
    // 1-element array to act as a mutable integer reference
    private final int[] hopMetric;
    
    // The lists to be shared by all copies
    private final List<String> brokerPath;
    private final List<String> brokerRegionPath;

    public EventMetrics() {
        this.hopMetric = new int[1];
        this.hopMetric[0] = 0;
        this.brokerPath = new ArrayList<>();
        this.brokerRegionPath = new ArrayList<>();
    }

    /**
     * Increments the shared hop count.
     */
    public void incrementHops() {
        this.hopMetric[0]++;
    }

    /**
     * Gets the current shared hop count.
     * @return The hop count.
     */
    public int getHops() {
        return this.hopMetric[0];
    }

    /**
     * Adds a broker's name to the shared path.
     * @param brokerName The name of the broker.
     */
    public void addBrokerToPath(String brokerName) {
        this.brokerPath.add(brokerName);
    }

    /**
     * Gets the shared path.
     * @return The list of broker names.
     */
    public List<String> getBrokerPath() {
        return this.brokerPath;
    }

    /**
     * Adds a broker's region string to the shared path.
     * @param regionString The string representation of the region.
     */
    public void addBrokerRegionToPath(String regionString) {
        this.brokerRegionPath.add(regionString);
    }

    /**
     * Gets the shared region path.
     * @return The list of broker regions.
     */
    public List<String> getBrokerRegionPath() {
        return this.brokerRegionPath;
    }
}