package marketplace.agents;

import simulator.entities.SubscriberWithLocation;
import simulator.core.Location;
import simulator.events.SimulationPublication;
import java.util.Map;
import java.util.Collections;

import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class MarketplaceProvider extends SubscriberWithLocation {

    private ServiceOffer lastAdvertisedOffer;

    public MarketplaceProvider(String name, Location location) {
        super(name, location);
    }

    /**
     * Publishes a ServiceOffer (Subscription) to the Marketplace.
     */
    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics) {
        // Create the specialized ServiceOffer event
        ServiceOffer offer = new ServiceOffer(serviceId, performanceMetrics, this.getLocation(), getName());

        // Store the authoritative object
        this.lastAdvertisedOffer = offer;

        // Propagate it
        this.send(offer);
    }

    /**
     * Returns the full last offer object.
     */
    public ServiceOffer getLastAdvertisedOffer() {
        return lastAdvertisedOffer;
    }

    /**
     * Helper to get just the metrics (delegates to the stored offer).
     */
    public Map<String, Double> getLastAdvertisedMetrics() {
        if (lastAdvertisedOffer != null) {
            return lastAdvertisedOffer.getQosMetrics();
        }
        return Collections.emptyMap();
    }

    /**
     * Handles incoming ServiceRequests (Publications) routed by the Broker.
     */
    @Override
    public void receive(SimulationPublication p) {
        super.receive(p);

        if (p instanceof ServiceRequest req) {
            long requestedId = req.getServiceId();
            String clientName = "Unknown";

            if (req.getMetrics() != null && req.getMetrics().getOriginalSourceName() != null) {
                clientName = req.getMetrics().getOriginalSourceName();
            }

            System.out.println(String.format(
                    "[DEBUG] Provider '%s' received Request for ServiceID=%d from Client '%s' | Prefs=%s",
                    getName(), requestedId, clientName, req.getPreferences()));

        } else if (p instanceof simulator.events.PublicationWithLocation pub) {
            // Fallback for generic publications
            String clientName = (pub.getMetrics() != null) ? pub.getMetrics().getOriginalSourceName() : "Unknown";
            System.out.println("[DEBUG] Provider " + getName() + " received generic publication from " + clientName);
        }
    }
}