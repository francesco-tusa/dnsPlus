package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;

import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceRequest;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.ClientDemandProfile;
import simulator.config.SimConfiguration;
import simulator.entities.PublisherWithLocation;
import simulator.events.PublicationWithLocation;
import utils.SimulationRandom;

public class SystematicMarketplaceContinuumSimulation extends AbstractMarketplaceContinuumSimulation {

    @Override
    protected List<PublicationWithLocation> generatePublications(List<PublisherWithLocation> publishers) {
        logger.info(">>> Generating Systematic Multi-Objective Workload for " + publishers.size() + " Clients...");
        
        List<PublicationWithLocation> requests = new ArrayList<>();
        long serviceId = 9999; 

        // Instantiate the decoupled generator using the global simulation seed
        ClientDemandGenerator demandGenerator = new ClientDemandGenerator(SimulationRandom.get());

        int total = publishers.size();
        for (int i = 0; i < total; i++) {
            PublisherWithLocation p = publishers.get(i);
            
            // 1. Fetch the stratified profile
            ClientDemandProfile profile = demandGenerator.generateDemand(i);

            // 2. Build the exact ServiceRequest using the profile data
            ServiceRequest req = new ServiceRequest(
                serviceId, 
                profile.constraints(), 
                profile.weights(), 
                p.getLocation()
            );
            
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
            logger.info(">>> Initializing Systematic Stratified Marketplace Simulation...");

            // 1. Initialize Configuration
            SimConfiguration.get(); 
            MarketplaceConfig.get(); 

            // 2. Setup Topology Components
            MarketplaceTopologyConfiguration topoConfig = new MarketplaceTopologyConfiguration();
            MarketplaceBrokerFactory brokerFactory = new MarketplaceBrokerFactory();
            MarketplaceTopologyLoader loader = new MarketplaceTopologyLoader(topoConfig, brokerFactory);
            
            // 3. Create Self (Using the new Systematic implementation)
            SystematicMarketplaceContinuumSimulation simulation = new SystematicMarketplaceContinuumSimulation();
            
            // 4. Run It
            simulation.run(loader, topoConfig);
            
            logger.info(">>> Simulation Complete.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}