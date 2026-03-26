package marketplace.agents;

import simulator.entities.SubscriberWithLocation;
import simulator.core.Location;
import simulator.events.SimulationPublication;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;

import marketplace.common.identifiers.RoutingIdentifierFactory;
import marketplace.common.identifiers.ServiceIdentifier;
import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class MarketplaceProvider extends SubscriberWithLocation {

    private final RoutingIdentifierFactory cryptographyFactory;

    private ServiceOffer lastAdvertisedOffer;
    
    private final Map<Long, ServiceOffer> activeOffers = new HashMap<>();

    // State for deferred offer creation must support multiple functions
    private static class DeferredConfig {
        final long serviceId;
        final Map<String, Double> metrics;
        final double range;

        DeferredConfig(long serviceId, Map<String, Double> metrics, double range) {
            this.serviceId = serviceId;
            this.metrics = metrics;
            this.range = range;
        }
    }
    
    private final Queue<DeferredConfig> pendingConfigs = new LinkedList<>();
    private final List<ServiceRequest> deliveredRequests = new ArrayList<>();

    public MarketplaceProvider(String name, Location location) {
        super(name, location);
        this.cryptographyFactory = MarketplaceConfig.get().routingCryptography;
    }

    public void configureService(long serviceId, Map<String, Double> performanceMetrics, double spatialRange) {
        this.pendingConfigs.add(new DeferredConfig(serviceId, performanceMetrics, spatialRange));
    }

    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics, double spatialRange) {
        configureService(serviceId, performanceMetrics, spatialRange);
        
        // Process the entire generated list
        List<ServiceOffer> offers = createServiceOffers();
        for (ServiceOffer offer : offers) {
            this.send(offer);
        }
    }

    public void advertiseService(long serviceId, Map<String, Double> performanceMetrics) {
        this.advertiseService(serviceId, performanceMetrics, 0.0);
    }
    
    /**
     * Generates and returns all deferred configurations as discrete offers.
     */
    public List<ServiceOffer> createServiceOffers() {
        List<ServiceOffer> generatedOffers = new ArrayList<>();

        while (!pendingConfigs.isEmpty()) {
            DeferredConfig config = pendingConfigs.poll();
            
            ServiceIdentifier routingId = this.cryptographyFactory.createIdentifier(config.serviceId);

            ServiceOffer offer = new ServiceOffer(
                config.serviceId, 
                routingId, 
                config.metrics, 
                this.getLocation(), 
                getName(), 
                config.range
            );

            // Map the Oracle ID to the Offer for Ground Truth validation later
            this.activeOffers.put(config.serviceId, offer);
            this.lastAdvertisedOffer = offer;
            
            generatedOffers.add(offer);
        }

        return generatedOffers;
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
        // Wrap the map values in an ArrayList to satisfy the legacy interface contract
        return new ArrayList<>(activeOffers.values());
    }

    /**
     * Retrieval of specific ServiceOffer for Ground Truth Oracle evaluations.
     */
    public ServiceOffer getSpecificOffer(long oracleServiceId) {
        return activeOffers.get(oracleServiceId);
    }

    @Override
    public void receive(SimulationPublication p) {
        super.receive(p); 
        
        if (p instanceof ServiceRequest req) {
            deliveredRequests.add(req);
        }
    }

    public List<ServiceRequest> getDeliveredRequests() {
        return deliveredRequests;
    }
}