package marketplace.config.factories;

import java.util.Random;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.StandardProviderProfileGenerator;
import marketplace.population.MarketplaceProviderPlacementStrategy;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.StandardClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.topics.SingleFunctionDistribution;
import simulator.population.SubscribersPlacementStrategy;
import utils.SimulationRandom;

public class SingleFunctionComponentFactory implements MarketplaceComponentFactory {
    private final Random random = SimulationRandom.get();

    @Override
    public FunctionDistributionStrategy createDistributionStrategy() {
        return new SingleFunctionDistribution();
    }

    @Override
    public ProviderProfileGenerator createProviderGenerator() {
        return new StandardProviderProfileGenerator(random);
    }

    @Override
    public ClientDemandGenerator createDemandGenerator() {
        return new StandardClientDemandGenerator(random);
    }

    @Override
    public SubscribersPlacementStrategy createProviderPlacementStrategy() {
        // Inject BOTH the profile generator and the function distribution strategy
        return new MarketplaceProviderPlacementStrategy(
            createProviderGenerator(), 
            createDistributionStrategy()
        );
    }
}