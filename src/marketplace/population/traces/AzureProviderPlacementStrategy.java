package marketplace.population.traces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import marketplace.agents.MarketplaceProvider;
import marketplace.config.MarketplaceConfig;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.tiers.CloudProviderTier;
import marketplace.population.tiers.EdgeProviderTier;
import marketplace.population.tiers.FogProviderTier;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.traces.AzureTraceRecord;
import marketplace.workload.traces.AzureTraceRepository;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.population.SubscribersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;
import utils.SimulationRandom;

public class AzureProviderPlacementStrategy implements SubscribersPlacementStrategy {
    
    private static final Logger logger = CustomLogger.getLogger(AzureProviderPlacementStrategy.class.getName());
    
    private final Random random = SimulationRandom.get();
    private final ProviderProfileGenerator profileGenerator;
    private final AzureTraceRepository traceRepo = AzureTraceRepository.getInstance();

    public AzureProviderPlacementStrategy(ProviderProfileGenerator profileGenerator) {
        this.profileGenerator = profileGenerator;
    }

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        logger.info(">>> Generating Azure Trace-Driven Providers (Knapsack Constrained)...");
        MarketplaceConfig config = MarketplaceConfig.get();

        if (config.cloudProviderCount > 0) placeTier(rootNode, "COUNTRY", config.cloudProviderCount, new CloudProviderTier(), "AWS_Cloud");
        if (config.fogProviderCount > 0)   placeTier(rootNode, "ADMIN1", config.fogProviderCount, new FogProviderTier(), "Telco_Fog");
        if (config.edgeProviderCount > 0)  placeTier(rootNode, "CITY", config.edgeProviderCount, new EdgeProviderTier(), "Metro_Edge");
    }

    private void placeTier(BoundedBroker root, String depthTag, int count, ProviderTierStrategy tierStrategy, String providerLabel) {
        List<TreeNode> candidates = findCandidatesByDepth(root, depthTag);
        if (candidates.isEmpty()) return;

        // 1. Memory-Aware FaaS Hardware Constraint Filtering
        List<AzureTraceRecord> eligibleFunctions = traceRepo.getAllRecords().values().stream()
            .filter(rec -> isFunctionEligibleForTier(rec, tierStrategy))
            .collect(Collectors.toList());

        if (eligibleFunctions.isEmpty()) {
            logger.warning("No eligible Azure functions found for tier " + providerLabel + " based on memory constraints!");
            return;
        }

        for (int i = 0; i < count; i++) {
            TreeNode targetNode = candidates.get(random.nextInt(candidates.size()));
            if (!(targetNode instanceof BoundedBroker hostBroker)) continue;

            Location providerLoc = extractLocation(hostBroker);
            double finalAdaptiveRange = tierStrategy.getCoverageRadius() * (0.75 + (random.nextDouble() * 0.50));

            MarketplaceProvider provider = new MarketplaceProvider(providerLabel + "_" + i + "_" + hostBroker.getName(), providerLoc);
            hostBroker.addChild(provider);

            // 2. Assign a strictly eligible empirical Azure Function
            AzureTraceRecord assignedRecord = eligibleFunctions.get(random.nextInt(eligibleFunctions.size()));
            
            // 3. Generate Profile (AzureTraceProfileGenerator will automatically scale cost by memory)
            ProviderProfileGenerator.ProviderPolicy policy = assignPolicyForTier(tierStrategy);
            Map<String, Double> qosProfile = profileGenerator.generateProfile(tierStrategy, policy, assignedRecord.functionId());
            
            provider.configureService(assignedRecord.functionId(), qosProfile, finalAdaptiveRange);
        }
    }

    private boolean isFunctionEligibleForTier(AzureTraceRecord record, ProviderTierStrategy tier) {
        // Deep Edge: Highly constrained (Max 128MB). Only hosts the most heavily invoked Top 10%.
        if (tier instanceof EdgeProviderTier) return record.memory() <= 128.0 && record.functionId() <= 100;
        
        // Fog: Moderately constrained (Max 512MB).
        if (tier instanceof FogProviderTier) return record.memory() <= 512.0;
        
        // Cloud: Unconstrained. Hosts the universal fallback state.
        return true; 
    }

    private ProviderProfileGenerator.ProviderPolicy assignPolicyForTier(ProviderTierStrategy tier) {
        double p = this.random.nextDouble(); 
        if (tier instanceof CloudProviderTier) return (p < 0.80) ? ProviderProfileGenerator.ProviderPolicy.WARM_OPTIMIZED : ProviderProfileGenerator.ProviderPolicy.BALANCED;
        if (tier instanceof EdgeProviderTier)  return (p < 0.70) ? ProviderProfileGenerator.ProviderPolicy.COST_OPTIMIZED : ProviderProfileGenerator.ProviderPolicy.BALANCED;
        
        if (p < 0.50) return ProviderProfileGenerator.ProviderPolicy.BALANCED;
        if (p < 0.80) return ProviderProfileGenerator.ProviderPolicy.WARM_OPTIMIZED;
        return ProviderProfileGenerator.ProviderPolicy.COST_OPTIMIZED;
    }

    private Location extractLocation(BoundedBroker hostBroker) {
        if (hostBroker.getRegion() != null) {
            if (hostBroker.getRegion() instanceof Region trueRegion) return trueRegion.getRandomLocation(this.random);
            else return hostBroker.getRegion().getRandomLocation();
        }
        return new Location(0, 0, 0);
    }

    private List<TreeNode> findCandidatesByDepth(TreeNode node, String depthTag) {
        List<TreeNode> results = new ArrayList<>();
        int targetDepth = parseDepth(depthTag);
        traverseAndCollect(node, 0, targetDepth, results);
        return results;
    }

    private void traverseAndCollect(TreeNode node, int currentDepth, int targetDepth, List<TreeNode> results) {
        if (currentDepth == targetDepth) { results.add(node); return; }
        if (node instanceof SimulationBroker sb) {
            for (TreeNode child : sb.getChildren()) traverseAndCollect(child, currentDepth + 1, targetDepth, results);
        }
    }

    private int parseDepth(String tag) {
        return switch (tag.toUpperCase()) {
            case "CONTINENT" -> 1;
            case "COUNTRY" -> 2;
            case "ADMIN1" -> 3;
            case "CITY" -> 4;
            default -> 4;
        };
    }
}