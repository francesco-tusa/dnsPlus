package marketplace.config.factories;

import marketplace.population.ProviderProfileGenerator;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import simulator.population.SubscribersPlacementStrategy;

public interface MarketplaceComponentFactory {
    FunctionDistributionStrategy createDistributionStrategy();
    ProviderProfileGenerator createProviderGenerator();
    ClientDemandGenerator createDemandGenerator();
    SubscribersPlacementStrategy createProviderPlacementStrategy();
}