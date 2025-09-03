package simulator.regions;

import simulator.events.SimulationPublication;

/**
 * An interface that defines the contract for a leaf broker.
 * A leaf broker is responsible for the final delivery of publications
 * to its directly connected subscribers.
 */
public interface LeafBroker {

    /**
     * Processes a publication for delivery to the subscribers
     * directly connected to this leaf broker.
     *
     * @param p The publication to be processed.
     */
    void processPublicationForLocalDelivery(SimulationPublication p);
}