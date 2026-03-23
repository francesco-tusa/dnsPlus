package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;

import marketplace.agents.MarketplaceClient;
import marketplace.config.factories.MarketplaceComponentFactory;
import marketplace.events.ServiceRequest;
import marketplace.workload.ClientDemandProfile;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import simulator.entities.PublisherWithLocation;
import simulator.events.PublicationWithLocation;

public abstract class MarketplaceContinuumSimulation extends AbstractMarketplaceContinuumSimulation {

    public MarketplaceContinuumSimulation(MarketplaceComponentFactory factory) {
        super(factory);
    }

    // ==================================================================================
    //  ABSTRACT FACTORY METHODS (To be implemented by Subclasses)
    // ==================================================================================
    
    protected abstract FunctionDistributionStrategy createDistributionStrategy();
    protected abstract ClientDemandGenerator createDemandGenerator();

    // ==================================================================================
    //  CORE WORKLOAD GENERATION LOOP
    // ==================================================================================

    @Override
    protected List<PublicationWithLocation> generatePublications(List<PublisherWithLocation> publishers) {
        logger.info(">>> Generating Marketplace Multi-Objective Workload...");
        
        List<PublicationWithLocation> requests = new ArrayList<>();
        
        ClientDemandGenerator demandGenerator = createDemandGenerator();
        FunctionDistributionStrategy distribution = createDistributionStrategy();

        for (int i = 0; i < publishers.size(); i++) {
            MarketplaceClient client = (MarketplaceClient) publishers.get(i);
            
            long oracleId = distribution.selectClientFunction();
            ClientDemandProfile profile = demandGenerator.generateDemand(i, oracleId);

            ServiceRequest req = client.createServiceRequest(oracleId, profile.constraints(), profile.weights());
            requests.add(req);
        }
        
        return requests;
    }
}