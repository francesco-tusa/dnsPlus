package marketplace.agents;

import simulator.entities.SubscriberWithLocation;
import simulator.core.Location;
import simulator.events.SubscriptionWithLocation;
import marketplace.common.MultiMetricLocation;
import simulator.events.SimulationSubscription;
import java.util.Map;

/**
 * Represents a Service Provider in the Marketplace.
 * Extends the Simulator's SubscriberWithLocation to participate in the standard
 * topology.
 * Uses "Subscriptions" to advertise availability (Availability = Subscription).
 */
public class MarketplaceProvider extends SubscriberWithLocation {

    public MarketplaceProvider(String name, Location location) {
        super(name, location);
    }

    private Map<String, Double> lastAdvertisedMetrics;

    /**
     * Advertises a service to the network with specific QoS metrics.
     * 
     * @param serviceId          The unique ID of the function/service.
     * @param performanceMetrics Map of metrics (e.g., "latency": 10.0, "cost": 5.0)
     */
    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics) {
        this.lastAdvertisedMetrics = performanceMetrics; // Store for Oracle/Debug

        // 1. Create a MultiMetricLocation that represents THIS provider's performance
        // profile
        // (We base it on our physical location, but attach performance stats)
        Location physicalLoc = this.getLocation();
        MultiMetricLocation performanceLoc = new MultiMetricLocation(physicalLoc, performanceMetrics);

        // 2. Create the Subscription payload
        SubscriptionWithLocation sub = new SubscriptionWithLocation(performanceLoc);

        // 3. Set the "Topic" (Service ID) - In this simulator, topics are often
        // implicit or part of the metadata. For exact matching, we might use
        // a specific field, but here we assume the simulator routes based on
        // Spatial/Metric "interest".
        //
        // However, to be compatible with typical topic-based pub/sub, let's assume
        // we might use the ID in a custom field if the base class supported it.
        // For now, we rely on the Location/Metrics being the primary matching criteria.

        // 4. Inject into the simulator
        // The 'send' method propagates it to the parent Broker.
        this.send(sub);

        // System.out.println("[Provider " + getName() + "] Advertised service " +
        // serviceId + " with metrics " + performanceMetrics);
    }

    public Map<String, Double> getLastAdvertisedMetrics() {
        return lastAdvertisedMetrics;
    }

    @Override
    public void receive(simulator.events.SimulationPublication p) {
        super.receive(p);
        // Debugging Receive
        System.out.println("[DEBUG] Provider " + getName() + " received msg ID=" + p.getId() + " Type="
                + p.getClass().getSimpleName());

        if (p instanceof simulator.events.PublicationWithLocation pub) {
            String clientName = "UNKNOWN";
            if (pub.getMetrics() != null) {
                if (pub.getMetrics().getOriginalSourceName() != null) {
                    clientName = pub.getMetrics().getOriginalSourceName();
                } else {
                    System.out.println(
                            "[DEBUG] Metrics present but SourceName is NULL. TraceID=" + pub.getMetrics().getTraceId());
                }
            } else {
                System.out.println("[DEBUG] Metrics object is NULL for msg ID=" + pub.getId());
            }

            System.out.println("[DEBUG] RECORDING MATCH: " + clientName + " -> " + getName());
            marketplace.simulations.MarketplaceContinuumSimulation.recordMatch(clientName, getName());
        } else {
            System.out.println("[DEBUG] IGNORING msg type: " + p.getClass().getName());
        }
    }
}
