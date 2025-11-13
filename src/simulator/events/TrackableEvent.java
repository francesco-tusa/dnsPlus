package simulator.events;

import java.util.List;

/**
 * Defines the contract for a simulation event (like a Subscription or Publication)
 * that needs to have its path and hop count tracked as it traverses the network.
 */
public interface TrackableEvent {

    /**
     * Increments the event's hop count.
     */
    void incrementHops();

    /**
     * Adds a broker's name to the event's path history.
     * @param brokerName The name of the broker.
     */
    void addBrokerToPath(String brokerName);

    /**
     * Gets the event's current hop count.
     * @return The hop count.
     */
    int getHops();

    /**
     * Gets the list of broker names in the event's path.
     * @return The path history.
     */
    List<String> getBrokerPath();

    /**
     * Adds a broker's region info to the event's path history.
     * @param regionInfo The string representation of the region.
     */
    void addBrokerRegionToPath(String regionInfo);

    /**
     * Gets the list of broker regions in the event's path.
     * @return The region path history.
     */
    List<String> getBrokerRegionPath();
}