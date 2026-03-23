package marketplace.simulations;

import marketplace.config.MarketplaceConfig;
import marketplace.config.factories.AzureComponentFactory; // Azure Polymorphic Factory
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.traces.AzureTraceRepository; // Required for trace bootstrapping
import simulator.config.SimConfiguration;

public class AzureMarketplaceSimulation extends MarketplaceContinuumSimulation {

    // Define explicit constructor to pass the Azure factory to the superclass
    public AzureMarketplaceSimulation() {
        super(new AzureComponentFactory());
    }

    // Delegate logic directly to the injected factory for clean polymorphism
    @Override
    protected FunctionDistributionStrategy createDistributionStrategy() {
        return componentFactory.createDistributionStrategy();
    }

    @Override
    protected ClientDemandGenerator createDemandGenerator() {
        return componentFactory.createDemandGenerator();
    }

    // ==================================================================================
    //  AZURE TRACE MAIN ENTRY POINT
    // ==================================================================================
    public static void main(String[] args) {
        try {
            logger.info(">>> Initializing Azure Trace Marketplace Simulation...");

            // 1. MANDATORY: Bootstrap the Empirical Dataset BEFORE starting the simulation
            AzureTraceRepository.getInstance().loadTraces("resources/azure_marketplace_top1000.csv");

            // 2. Initialize Global Configurations
            SimConfiguration.get(); 
            MarketplaceConfig.get();

            // 3. Setup Topology Components
            MarketplaceTopologyConfiguration topoConfig = new MarketplaceTopologyConfiguration();
            MarketplaceBrokerFactory brokerFactory = new MarketplaceBrokerFactory();
            MarketplaceTopologyLoader loader = new MarketplaceTopologyLoader(topoConfig, brokerFactory);
            
            // 4. Instantiate and Run the Azure Engine
            AzureMarketplaceSimulation simulation = new AzureMarketplaceSimulation();
            simulation.run(loader, topoConfig);
            
            logger.info(">>> Azure Trace Simulation Complete.");

        } catch (Exception e) {
            logger.severe("Fatal error during Azure simulation execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}