package marketplace.simulations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import marketplace.agents.MarketplaceClient;
import marketplace.config.MarketplaceConfig;
import marketplace.config.factories.MarketplaceComponentFactory;
import marketplace.events.ServiceRequest;
import marketplace.optimization.TelemetryLoggingStrategy;
import marketplace.workload.ClientDemandProfile;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import simulator.entities.PublisherWithLocation;
import simulator.events.PublicationWithLocation;

public abstract class MarketplaceContinuumSimulation extends AbstractMarketplaceContinuumSimulation {

    //private static final Logger logger = CustomLogger.getLogger(MarketplaceContinuumSimulation.class.getName());

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

    @Override
    protected void setupSimulation() {
        super.setupSimulation();

        // Bind the RL Telemetry directory to the current execution's output folder
        if (MarketplaceConfig.get().collectFlTelemetry) {
            String runId = this.getSimulationId(); 
            String path = "output" + File.separator + runId + File.separator + "telemetry_dump";
            
            TelemetryLoggingStrategy.setDumpDirectory(path);
            
            logger.info("RL Telemetry dump directory bound to: " + path);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();

        if (MarketplaceConfig.get().collectFlTelemetry) {
            logger.info("--- Finalizing FaaS Marketplace HFL Data Generation ---");
            logger.info("Simulation routing complete. Commencing localized telemetry dump...");
            
            TelemetryLoggingStrategy.flushAndCloseAll();
        }
    }
}