package marketplace.population.traces;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import marketplace.agents.MarketplaceProvider;
import marketplace.population.AbstractProviderPlacementStrategy;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.tiers.CloudProviderTier;
import marketplace.population.tiers.EdgeProviderTier;
import marketplace.population.tiers.FogProviderTier;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.traces.AzureTraceFunctionDistribution;
import marketplace.workload.traces.AzureTraceRecord;
import marketplace.workload.traces.AzureTraceRepository;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;

public class AzureProviderPlacementStrategy extends AbstractProviderPlacementStrategy {
    
    private final AzureTraceFunctionDistribution functionDistribution;
    private final List<Long> cloudFallbackFunctions;

    public AzureProviderPlacementStrategy(ProviderProfileGenerator profileGenerator) {
        super(profileGenerator);
        this.functionDistribution = new AzureTraceFunctionDistribution();
        
        this.cloudFallbackFunctions = AzureTraceRepository.getInstance().getAllRecords().values().stream()
                .sorted((a, b) -> Long.compare(b.invocationCount(), a.invocationCount()))
                .limit(this.config.cloudTenantCapacity)
                .map(AzureTraceRecord::functionId)
                .toList();
    }

    @Override
    protected String getStrategyName() {
        return "Azure Multi-Tenant Trace Providers";
    }

    @Override
    protected void placeTier(BoundedBroker root, String depthTag, int count, ProviderTierStrategy tierStrategy, String providerLabel, int rootOffset) {
        List<TreeNode> candidates = findCandidatesByDepth(root, depthTag, rootOffset);
        if (candidates.isEmpty()) {
            logger.warning("No structural candidates found for tier: " + depthTag);
            return;
        }

        int totalDatasetSize = AzureTraceRepository.getInstance().getAllRecords().size();
        int tenantCapacity = 1;
        boolean isCloudFallback = false;
        
        // Note: This instanceof check is acceptable here because it explicitly dictates 
        // capacity limits based on the tier's role in the multi-tenant dataset configuration, 
        // rather than core physical metrics.
        if (tierStrategy instanceof CloudProviderTier) {
            tenantCapacity = Math.min(this.config.cloudTenantCapacity, cloudFallbackFunctions.size());
            isCloudFallback = true;
        } else if (tierStrategy instanceof FogProviderTier) {
            tenantCapacity = Math.min(this.config.fogTenantCapacity, totalDatasetSize);
        } else if (tierStrategy instanceof EdgeProviderTier) {
            tenantCapacity = Math.min(this.config.edgeTenantCapacity, totalDatasetSize);
        }

        for (int i = 0; i < count; i++) {
            TreeNode targetNode = candidates.get(random.nextInt(candidates.size()));
            if (!(targetNode instanceof BoundedBroker hostBroker)) continue;

            Location providerLoc = extractLocation(hostBroker);
            double finalAdaptiveRange = calculateCoveringRange(hostBroker, tierStrategy);

            MarketplaceProvider provider = new MarketplaceProvider(providerLabel + "_" + i + "_" + hostBroker.getName(), providerLoc);
            hostBroker.addChild(provider);
            
            ProviderProfileGenerator.ProviderPolicy policy = tierStrategy.generateProviderPolicy(this.random);
            Set<Long> localCache = new HashSet<>();
            
            for (int t = 0; t < tenantCapacity; t++) {
                long assignedFunctionId;
                if (isCloudFallback) {
                    assignedFunctionId = cloudFallbackFunctions.get(t);
                } else {
                    do { 
                        assignedFunctionId = this.functionDistribution.selectProviderFunction(); 
                    } 
                    while (localCache.contains(assignedFunctionId)); 
                }
                localCache.add(assignedFunctionId);
                
                Map<String, Double> qosProfile = profileGenerator.generateProfile(tierStrategy, policy, assignedFunctionId);
                provider.configureService(assignedFunctionId, qosProfile, finalAdaptiveRange);
            }
        }
    }
}