package marketplace.population;

import java.util.Map;
import marketplace.population.tiers.ProviderTierStrategy;

public interface ProviderProfileGenerator {
    enum ProviderPolicy { WARM_OPTIMIZED, COST_OPTIMIZED, BALANCED }
    Map<String, Double> generateProfile(ProviderTierStrategy tier, ProviderPolicy policy, long functionId);
}