package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceRequest;
import marketplace.topology.MarketplaceBrokerFactory;
import simulator.config.SimConfiguration;
import simulator.entities.PublisherWithLocation;
import simulator.events.PublicationWithLocation;

public class MarketplaceContinuumSimulation extends AbstractMarketplaceContinuumSimulation {

    @Override
    protected List<PublicationWithLocation> generatePublications(List<PublisherWithLocation> publishers) {
        logger.info(">>> Pre-generating Multi-Objective ServiceRequests for " + publishers.size() + " Clients...");
        
        List<PublicationWithLocation> requests = new ArrayList<>();
        long serviceId = 9999; 

        for (PublisherWithLocation p : publishers) {
            
            // 1. Define Constraints (The "Must Haves")
            // - Latency: Relaxed to 150ms so Cloud (100ms) is a valid candidate.
            // - Cost: Set to 100.0 so Edge (80.0) is a valid candidate.
            Map<String, Double> constraints = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 50.0, // 150.0
                MarketplaceMetricSchema.METRIC_COST,    10.0  // 100.0
            );

            // 2. Define Weights (The "Preferences" for Scoring)
            // - This fixes the 0.000 log issue.
            // - 50% preference for speed, 50% preference for low price.
            Map<String, Double> weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.3,
                MarketplaceMetricSchema.METRIC_COST,    0.7
            );

            // 3. Create Request with WEIGHTS
            ServiceRequest req = new ServiceRequest(serviceId, constraints, weights, p.getLocation());
            req.setSource(p); 
            requests.add(req);
        }
        
        return requests;
    }


    // ==================================================================================
    //  MAIN ENTRY POINT
    // ==================================================================================

    public static void main(String[] args) {
        try {
            logger.info(">>> Initializing Homogeneous Marketplace Simulation...");

            // 1. Initialize Configuration
            SimConfiguration.get(); 
            MarketplaceConfig.get(); 

            // 2. Setup Topology Components
            MarketplaceTopologyConfiguration topoConfig = new MarketplaceTopologyConfiguration();
            MarketplaceBrokerFactory brokerFactory = new MarketplaceBrokerFactory();
            MarketplaceTopologyLoader loader = new MarketplaceTopologyLoader(topoConfig, brokerFactory);
            
            // 3. Create Self
            MarketplaceContinuumSimulation simulation = new MarketplaceContinuumSimulation();
            
            // 4. Run It
            simulation.run(loader, topoConfig);
            
            logger.info(">>> Simulation Complete.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}