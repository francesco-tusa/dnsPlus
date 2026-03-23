package marketplace.config.factories;

import java.util.Random;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.traces.AzureTraceProfileGenerator;
import marketplace.population.traces.AzureProviderPlacementStrategy;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.traces.AzureClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.traces.AzureTraceFunctionDistribution;
import simulator.population.SubscribersPlacementStrategy;
import utils.SimulationRandom;

public class AzureComponentFactory implements MarketplaceComponentFactory {
    private final Random random = SimulationRandom.get();

    @Override
    public FunctionDistributionStrategy createDistributionStrategy() {
        return new AzureTraceFunctionDistribution();
    }

    @Override
    public ProviderProfileGenerator createProviderGenerator() {
        return new AzureTraceProfileGenerator(random);
    }

    @Override
    public ClientDemandGenerator createDemandGenerator() {
        return new AzureClientDemandGenerator();
    }

    @Override
    public SubscribersPlacementStrategy createProviderPlacementStrategy() {
        return new AzureProviderPlacementStrategy(createProviderGenerator());
    }
}