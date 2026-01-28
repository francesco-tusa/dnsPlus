package marketplace.agents;

import simulator.entities.PublisherWithLocation;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import marketplace.common.MultiMetricLocation;
import java.util.Map;

/**
 * Represents a Client in the Marketplace.
 * Extends the Simulator's PublisherWithLocation.
 * Uses "Publications" to request services (Request = Publication).
 */
public class MarketplaceClient extends PublisherWithLocation {

    public MarketplaceClient(String name, Location location) {
        super(name, location);
    }

    /**
     * Requests a service with specific requirements.
     * 
     * @param serviceId   The ID of the service to invoke.
     * @param constraints Preference metrics (e.g., "latency": 0.0 -> implies we
     *                    want minimal latency).
     *                    The distance function treats these as the "Target".
     */
    public void requestService(long serviceId, Map<String, Double> constraints) {
        // 1. Create a MultiMetricLocation that represents the IDEAL target.
        // (Physical location = Client's location, Metrics = Desired values)
        Location physicalLoc = this.getLocation();
        MultiMetricLocation requirementLoc = new MultiMetricLocation(physicalLoc, constraints);

        // 2. Create the Publication payload
        PublicationWithLocation pub = new PublicationWithLocation(requirementLoc);

        // 3. Inject into the simulator
        // The 'send' method propagates it to the parent Broker.
        this.send(pub);

        // System.out.println("[Client " + getName() + "] Requested service " +
        // serviceId + " with constraints " + constraints);
    }
}
