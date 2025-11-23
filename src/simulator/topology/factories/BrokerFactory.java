package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BoundedBroker;

/**
 * An interface for factories that create broker nodes for a topology.
 * This allows a topology generator to be independent of the concrete broker implementation.
 */
public interface BrokerFactory {
    /**
     * Creates a non-leaf (internal) broker node.
     * @param name The name for the new broker.
     * @return A new broker instance as a BrokerWithRegion.
     */
    BoundedBroker createBroker(String name);

    /**
     * Creates a leaf broker node with a predefined region.
     * @param name The name for the new leaf broker.
     * @param p1 The first corner location defining the broker's initial region.
     * @param p2 The second corner location defining the broker's initial region.
     * @return A new leaf broker instance as a BrokerWithRegion.
     */
    BoundedBroker createLeafBroker(String name, Location p1, Location p2);

    /**
     * Creates a leaf broker node without a predefined region.
     * Its region will be calculated dynamically based on its children.
     * @param name The name for the new leaf broker.
     * @return A new leaf broker instance as a BrokerWithRegion.
     */
    BoundedBroker createLeafBroker(String name);
}