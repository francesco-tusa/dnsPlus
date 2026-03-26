package marketplace.population;

import java.util.List;
import java.util.Map;

import marketplace.agents.MarketplaceProvider;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.topics.SingleFunctionDistribution;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;

public class MarketplaceProviderPlacementStrategy extends AbstractProviderPlacementStrategy {
    
    private final FunctionDistributionStrategy functionDistribution;

    public MarketplaceProviderPlacementStrategy() {
        this(new StandardProviderProfileGenerator(utils.SimulationRandom.get()), new SingleFunctionDistribution());
    }

    public MarketplaceProviderPlacementStrategy(ProviderProfileGenerator profileGenerator, FunctionDistributionStrategy functionDistribution) {
        super(profileGenerator);
        this.functionDistribution = functionDistribution;
    }

    @Override
    protected String getStrategyName() {
        return "Legacy Marketplace Providers (Single-Function Uniform)";
    }

    @Override
    protected void placeTier(BoundedBroker root, String depthTag, int count, ProviderTierStrategy tierStrategy, String providerLabel, int rootOffset) {
        List<TreeNode> candidates = findCandidatesByDepth(root, depthTag, rootOffset);
        if (candidates.isEmpty()) {
            logger.warning("No structural candidates found for tier: " + depthTag);
            return;
        }

        for (int i = 0; i < count; i++) {
            TreeNode targetNode = candidates.get(random.nextInt(candidates.size()));
            if (!(targetNode instanceof BoundedBroker hostBroker)) continue;

            Location providerLoc = extractLocation(hostBroker);
            double baseRange = calculateCoveringRange(hostBroker, tierStrategy);
            double finalAdaptiveRange = baseRange * (0.75 + (random.nextDouble() * 0.50));

            MarketplaceProvider provider = new MarketplaceProvider(providerLabel + "_" + i + "_" + hostBroker.getName(), providerLoc);
            hostBroker.addChild(provider);
            
            ProviderProfileGenerator.ProviderPolicy policy = assignPolicyForTier(tierStrategy);
            long assignedOracleId = this.functionDistribution.selectProviderFunction();
            Map<String, Double> qosProfile = profileGenerator.generateProfile(tierStrategy, policy, assignedOracleId);
            
            provider.configureService(assignedOracleId, qosProfile, finalAdaptiveRange);
        }
    }
}