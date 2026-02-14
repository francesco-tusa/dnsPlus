package marketplace.agents;

import simulator.entities.PublisherWithLocation;
import simulator.core.Location;
import java.util.Map;
import marketplace.events.ServiceRequest; 

public class MarketplaceClient extends PublisherWithLocation {

    public MarketplaceClient(String name, Location location) {
        super(name, location);
    }

    /**
     * Request a service with specific constraints and optimization weights.
     * * @param serviceId   The unique ID of the service.
     * @param constraints The hard limits (e.g., Latency < 50ms).
     * @param weights     The importance of each metric (e.g., Latency=0.9, Cost=0.1).
     * If null, defaults to equal importance.
     */
    
public void requestService(long serviceId, Map<String, Double> constraints, Map<String, Double> weights) {
    // 1. Create the request
    ServiceRequest req = new ServiceRequest(serviceId, constraints, weights, this.getLocation());

    // 3. Use the parent's send method
    super.send(req);
}

    /**
     * Overloaded convenience method for backward compatibility.
     * Uses default weights (1.0 for all metrics).
     */
    public void requestService(long serviceId, Map<String, Double> constraints) {
        this.requestService(serviceId, constraints, null);
    }
}