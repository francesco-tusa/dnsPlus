package marketplace.simulations;

import marketplace.config.factories.AzureComponentFactory;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.traces.AzureTraceRepository;
import marketplace.config.MarketplaceConfig;
import simulator.config.SimConfiguration;

public class AzureMarketplaceSimulation extends AbstractMarketplaceContinuumSimulation {

    public AzureMarketplaceSimulation() {
        super(new AzureComponentFactory());
    }

    @Override
    protected FunctionDistributionStrategy createDistributionStrategy() {
        return componentFactory.createDistributionStrategy();
    }

    @Override
    protected ClientDemandGenerator createDemandGenerator() {
        return componentFactory.createDemandGenerator();
    }

    public static void main(String[] args) {
        try {
            logger.info(">>> Initializing Generic Azure Trace Marketplace Simulation...");
            AzureTraceRepository.getInstance().loadTraces(MarketplaceConfig.get().azureTraceFilePath);
            
            SimConfiguration.get(); 
            MarketplaceConfig.get();

            MarketplaceTopologyConfiguration topoConfig = new MarketplaceTopologyConfiguration();
            MarketplaceTopologyLoader loader = new MarketplaceTopologyLoader(topoConfig, new MarketplaceBrokerFactory());
            
            AzureMarketplaceSimulation simulation = new AzureMarketplaceSimulation();
            simulation.run(loader, topoConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}