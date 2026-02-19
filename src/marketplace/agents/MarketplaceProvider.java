package marketplace.agents;

import simulator.entities.SubscriberWithLocation;
import simulator.core.Location;
import simulator.events.SimulationPublication;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import marketplace.events.ServiceOffer;

public class MarketplaceProvider extends SubscriberWithLocation {

    private ServiceOffer lastAdvertisedOffer;
    private final List<ServiceOffer> activeOffers = new ArrayList<>();

    // State for deferred offer creation
    private long configuredServiceId;
    private Map<String, Double> configuredMetrics;
    
    private double configuredRange; 
    private boolean isConfigured = false;

    public MarketplaceProvider(String name, Location location) {
        super(name, location);
    }

    /**
     * Configures the service parameters without sending the offer yet.
     * @param spatialRange The half-width of the square region (from center to edge).
     */
    public void configureService(long serviceId, Map<String, Double> performanceMetrics, double spatialRange) {
        this.configuredServiceId = serviceId;
        this.configuredMetrics = performanceMetrics;
        this.configuredRange = spatialRange;
        this.isConfigured = true;
    }

    /**
     * 1. MAIN OVERLOAD (Explicit Range)
     */
    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics, double spatialRange) {
        configureService(serviceId, performanceMetrics, spatialRange);
        
        ServiceOffer offer = createServiceOffer();
        if (offer != null) {
            this.send(offer);
        }
    }

    /**
     * 2. BACKWARD-COMPATIBLE OVERLOAD (No Range)
     * Defaults to -1.0 (indicating "Standard/Broker Default").
     */
    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics) {
        this.advertiseService(serviceId, performanceMetrics, 0.0);
    }
    
    /**
     * Called by MarketplaceWorkloadGenerator to create the actual Subscription object.
     */
    public ServiceOffer createServiceOffer() {
        if (!isConfigured) {
            return null;
        }

        // Create the specialized ServiceOffer event
        // The ServiceOffer constructor will interpret 'configuredRange' as the delta for the HyperCube bounds
        ServiceOffer offer = new ServiceOffer(
            configuredServiceId, 
            configuredMetrics, 
            this.getLocation(), 
            getName(), 
            configuredRange
        );

        this.activeOffers.add(offer);
        this.lastAdvertisedOffer = offer;

        return offer;
    }

    public ServiceOffer getLastAdvertisedOffer() {
        return lastAdvertisedOffer;
    }

    public Map<String, Double> getLastAdvertisedMetrics() {
        if (lastAdvertisedOffer != null) {
            return lastAdvertisedOffer.getQosMetrics();
        }
        return Collections.emptyMap();
    }

    public List<ServiceOffer> getActiveOffers() {
        return new ArrayList<>(activeOffers);
    }

    @Override
    public void receive(SimulationPublication p) {
        super.receive(p);
    }
}