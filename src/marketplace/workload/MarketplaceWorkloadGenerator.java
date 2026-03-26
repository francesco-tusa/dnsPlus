package marketplace.workload;

import java.util.ArrayList;
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
        // Legacy Support: Yields the first item if the legacy singular method is invoked
        if (subscriber instanceof MarketplaceProvider provider) {
            List<ServiceOffer> offers = provider.createServiceOffers();
            if (!offers.isEmpty()) {
                logger.fine("Generated singular legacy ServiceOffer for " + provider.getName());
                return offers.get(0);
            }
        }
        return null;
    }

    @Override
    public List<SimulationSubscription> generateSubscriptionBatch(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers, int count) {
        // Multi-function support
        if (subscriber instanceof MarketplaceProvider provider) {
            List<ServiceOffer> offers = provider.createServiceOffers();
            if (!offers.isEmpty()) {
                logger.fine("Generated batch of " + offers.size() + " ServiceOffers for " + provider.getName());
            }
            // Return the full array directly into the Orchestrator's WorkloadRepository
            return new ArrayList<>(offers);
        }
        
        // Return empty list for regular clients or if unconfigured
        return Collections.emptyList();
    }

    @Override
    public void setHotspots(List<BoundedBroker> hotspots) {
        // No-op for Marketplace
    }
}