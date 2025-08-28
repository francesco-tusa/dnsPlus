package simulator.topology.factories;

import simulator.Location;
import simulator.regions.BrokerWithRegion;

/**
 * An interface for factories that create broker nodes for a topology.
 * This allows a topology generator to be independent of the concrete broker implementation by using a common superclass.
 */
public interface BrokerFactory {
    /**
     * Creates a non-leaf (internal) broker node.
     * @param name The name for the new broker.
     * @return A new broker instance as a BrokerWithRegion.
     */
    BrokerWithRegion createBroker(String name);

    /**
     * Creates a leaf broker node.
     * @param name The name for the new leaf broker.
     * @param p1 The first corner location defining the broker's initial region.
     * @param p2 The second corner location defining the broker's initial region.
     * @return A new leaf broker instance as a BrokerWithRegion.
     */
    BrokerWithRegion createLeafBroker(String name, Location p1, Location p2);
}