package marketplace.agents;

import simulator.entities.PublisherWithLocation;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
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
        // Updated to use the new ServiceRequest constructor that accepts weights
        ServiceRequest req = new ServiceRequest(serviceId, constraints, weights, this.getLocation());

        // --- METRICS & TRACING ---
        simulator.events.metrics.EventMetrics metrics = new simulator.events.metrics.EventMetrics(System.nanoTime());
        Location physicalLoc = this.getLocation();
        metrics.setOriginalSourceInfo(getName(), "Marketplace", physicalLoc.getX(), physicalLoc.getY());
        req.setMetrics(metrics);

        // Inject
        this.send(req);
    }

    /**
     * Overloaded convenience method for backward compatibility.
     * Uses default weights (1.0 for all metrics).
     */
    public void requestService(long serviceId, Map<String, Double> constraints) {
        this.requestService(serviceId, constraints, null);
    }

    @Override
    public void send(PublicationWithLocation pub) {
        pub.setSource(this);

        if (pub.getMetrics() == null) {
            simulator.events.metrics.EventMetrics metrics = new simulator.events.metrics.EventMetrics(
                    System.nanoTime());
            metrics.setOriginalSourceInfo(getName(), "Marketplace", this.getLocation().getX(),
                    this.getLocation().getY());
            pub.setMetrics(metrics);
        }

        simulator.core.TreeNode parent = getParent();
        if (parent instanceof simulator.entities.SimulationBroker) {
            ((simulator.entities.SimulationBroker) parent).processPublication(pub);
        } else {
            System.err.println(getName() + ": parent is not a SimulationBroker");
        }
    }
}