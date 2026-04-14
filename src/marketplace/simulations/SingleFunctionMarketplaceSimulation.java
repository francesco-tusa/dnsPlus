package marketplace.simulations;

import marketplace.config.MarketplaceConfig;
import marketplace.config.factories.SingleFunctionComponentFactory; // New Polymorphic Factory
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import simulator.config.SimConfiguration;

public class SingleFunctionMarketplaceSimulation extends AbstractMarketplaceContinuumSimulation {

    // Define explicit constructor to pass the factory to the superclass
    public SingleFunctionMarketplaceSimulation() {
        super(new SingleFunctionComponentFactory());
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
    //  SINGLE-FUNCTION MAIN ENTRY POINT
    // ==================================================================================
    public static void main(String[] args) {
        try {
            logger.info(">>> Initializing SingleFunction Stratified Marketplace Simulation...");

            SimConfiguration.get(); 
            MarketplaceConfig.get();

            MarketplaceTopologyConfiguration topoConfig = new MarketplaceTopologyConfiguration();
            MarketplaceBrokerFactory brokerFactory = new MarketplaceBrokerFactory();
            MarketplaceTopologyLoader loader = new MarketplaceTopologyLoader(topoConfig, brokerFactory);
            
            // Now correctly uses the constructor that injects the SingleFunctionComponentFactory
            SingleFunctionMarketplaceSimulation simulation = new SingleFunctionMarketplaceSimulation();
            simulation.run(loader, topoConfig);
            
            logger.info(">>> SingleFunction Simulation Complete.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}