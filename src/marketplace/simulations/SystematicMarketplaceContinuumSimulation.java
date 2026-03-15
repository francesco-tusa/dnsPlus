package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;

import marketplace.agents.MarketplaceClient;

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
        logger.info(">>> Generating Marketplace Multi-Objective Workload...");
        
        List<PublicationWithLocation> requests = new ArrayList<>();
        ClientDemandGenerator demandGenerator = new ClientDemandGenerator(SimulationRandom.get());

        marketplace.workload.topics.FunctionDistributionStrategy distribution = 
            MarketplaceConfig.get().functionDistribution;

        for (int i = 0; i < publishers.size(); i++) {
            // Safely cast to our specialized agent
            MarketplaceClient client = (MarketplaceClient) publishers.get(i);
            
            // 1. Ask the Workload Distribution which function this client needs
            long oracleId = distribution.selectClientFunction();
            
            // 2. Fetch the stratified QoS profile
            ClientDemandProfile profile = demandGenerator.generateDemand(i);

            // 3. Delegate the cryptographic wrapping and object creation to the agent itself
            ServiceRequest req = client.createServiceRequest(oracleId, profile.constraints(), profile.weights());
            
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