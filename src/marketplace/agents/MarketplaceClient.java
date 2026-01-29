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

        // --- PREPARE METRICS FOR TRACING ---
        // Even if caching/tracing is disabled globally, we inject metrics for Logic
        // Verification
        simulator.events.metrics.EventMetrics metrics = new simulator.events.metrics.EventMetrics(System.nanoTime());
        metrics.setOriginalSourceInfo(getName(), "Marketplace", physicalLoc.getX(), physicalLoc.getY());
        pub.setMetrics(metrics);

        // DEBUG
        // System.out.println("[DEBUG] Client " + getName() + " sending req. TraceID=" +
        // metrics.getTraceId() + " Source="
        // + metrics.getOriginalSourceName());

        // 3. Inject into the simulator
        // The 'send' method propagates it to the parent Broker.
        this.send(pub);
    }

    /**
     * Override send to bypass the default PublisherWithLocation logic which
     * forcibly overwrites metrics based on SimConfiguration.
     * We want to guarantee our TraceID/SourceInfo is preserved.
     */

    @Override
    public void send(PublicationWithLocation pub) {
        pub.setSource(this);

        // Ensure the metrics we set in requestService are kept.
        // If not set, we create them here.
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
