package marketplace.workload;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import marketplace.agents.MarketplaceProvider;
import marketplace.events.ServiceOffer;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.workload.SubscriptionWorkloadGenerator;
import utils.CustomLogger;

public class MarketplaceWorkloadGenerator implements SubscriptionWorkloadGenerator {
    
    private static final Logger logger = CustomLogger.getLogger(MarketplaceWorkloadGenerator.class.getName());

    @Override
    public SimulationSubscription generateSubscription(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers) {
        // 1. If it is a Marketplace Provider, generate its specific Service Offer
        if (subscriber instanceof MarketplaceProvider provider) {
            ServiceOffer offer = provider.createServiceOffer();
            if (offer != null) {
                logger.fine("Generated ServiceOffer for " + provider.getName());
                return offer;
            }
        }
        
        // Return null for regular clients or if not configured (Simulator handles null gracefully)
        return null;
    }

    @Override
    public void setHotspots(List<BoundedBroker> hotspots) {
        // No-op for Marketplace
    }
}