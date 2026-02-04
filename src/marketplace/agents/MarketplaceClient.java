package marketplace.agents;

import simulator.entities.PublisherWithLocation;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import java.util.Map;
import marketplace.events.ServiceRequest; // Import the new event

public class MarketplaceClient extends PublisherWithLocation {

    public MarketplaceClient(String name, Location location) {
        super(name, location);
    }

    public void requestService(long serviceId, Map<String, Double> constraints) {
        ServiceRequest req = new ServiceRequest(serviceId, constraints, this.getLocation());

        // --- METRICS & TRACING ---
        simulator.events.metrics.EventMetrics metrics = new simulator.events.metrics.EventMetrics(System.nanoTime());
        Location physicalLoc = this.getLocation();
        metrics.setOriginalSourceInfo(getName(), "Marketplace", physicalLoc.getX(), physicalLoc.getY());
        req.setMetrics(metrics);

        // Inject
        this.send(req);
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