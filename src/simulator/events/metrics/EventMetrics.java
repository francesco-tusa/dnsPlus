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
    private final List<String> subscribersReached; 

    public EventMetrics() {
        this.hopMetric = new int[1];
        this.hopMetric[0] = 0;
        this.brokerPath = new ArrayList<>();
        this.brokerRegionPath = new ArrayList<>();
        this.subscribersReached = new ArrayList<>();
    }

    /**
     * Copy constructor for branching paths (Deep Copy).
     * Creates a new instance with the current state but independent lists.
     */
    public EventMetrics(EventMetrics other) {
        this.hopMetric = new int[1];
        this.hopMetric[0] = other.hopMetric[0];
        // Deep copy the lists to branch the history
        this.brokerPath = new ArrayList<>(other.brokerPath);
        this.brokerRegionPath = new ArrayList<>(other.brokerRegionPath);
        this.subscribersReached = new ArrayList<>(other.subscribersReached);
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
    
    /**
     * Adds a subscriber's name to the shared list.
     * @param subscriberName The name of the subscriber.
     */
    public void addSubscriberToPath(String subscriberName) {
        this.subscribersReached.add(subscriberName);
    }

    /**
     * Gets the shared list of reached subscribers.
     * @return The list of subscriber names.
     */
    public List<String> getSubscribersReached() {
        return this.subscribersReached;
    }
}