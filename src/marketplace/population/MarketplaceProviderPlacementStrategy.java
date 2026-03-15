package marketplace.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import marketplace.agents.MarketplaceProvider;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.population.SubscribersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;
import utils.SimulationRandom;

public class MarketplaceProviderPlacementStrategy implements SubscribersPlacementStrategy {
    
    private static final Logger logger = CustomLogger.getLogger(MarketplaceProviderPlacementStrategy.class.getName());
    
    private final Random random = SimulationRandom.get();
    private final ProviderProfileGenerator profileGenerator = new ProviderProfileGenerator(random);

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        logger.info(">>> Generating Marketplace Providers (Continuum Placement)...");
        
        MarketplaceConfig config = MarketplaceConfig.get();

        // 1. CLOUD TIER
        if (config.cloudProviderCount > 0) {
            placeTier(rootNode, "COUNTRY", config.cloudProviderCount, "CLOUD", "AWS_Cloud");
        }

        // 2. FOG TIER
        if (config.fogProviderCount > 0) {
            placeTier(rootNode, "ADMIN1", config.fogProviderCount, "FOG", "Telco_Fog");
        }

        // 3. EDGE TIER
        if (config.edgeProviderCount > 0) {
            placeTier(rootNode, "CITY", config.edgeProviderCount, "EDGE", "Metro_Edge");
        }
    }

    private void placeTier(BoundedBroker root, String depthTag, int count, String tierType, String providerLabel) {
        List<TreeNode> candidates = findCandidatesByDepth(root, depthTag);
        
        if (candidates.isEmpty()) {
            logger.warning("No candidates found for level " + depthTag);
            return;
        }

        logger.info(String.format("Placing %d '%s' Providers on %s nodes.", count, providerLabel, depthTag));

        for (int i = 0; i < count; i++) {
            TreeNode targetNode = candidates.get(random.nextInt(candidates.size()));
            
            if (!(targetNode instanceof BoundedBroker hostBroker)) continue;

            // --- NATIVE GEOGRAPHIC DISPERSION ---
            // Utilizing the native Region class to deterministically scatter 
            // providers across the broker's bounding box.
            Location providerLoc = new Location(0, 0, 0); // Safe default
            if (hostBroker.getRegion() != null) {
                if (hostBroker.getRegion() instanceof Region trueRegion) {
                    providerLoc = trueRegion.getRandomLocation(this.random);
                } else {
                    providerLoc = hostBroker.getRegion().getRandomLocation();
                }
            }
            
            // --- HETEROGENEOUS RADII ---
            // Calculate base range, then apply a +/- 25% stochastic jitter 
            double baseRange = calculateCoveringRange(hostBroker, depthTag);
            double jitterMultiplier = 0.75 + (random.nextDouble() * 0.50); 
            double finalAdaptiveRange = baseRange * jitterMultiplier;

            String regionName = hostBroker.getName();
            String name = String.format("%s_%d_%s", providerLabel, i, regionName);
            
            MarketplaceProvider provider = new MarketplaceProvider(name, providerLoc);
            hostBroker.addChild(provider);
            
            ProviderProfileGenerator.ProviderPolicy policy = assignPolicyForTier(tierType);
            Map<String, Double> qosProfile = profileGenerator.generateProfile(tierType, policy);

            long assignedOracleId = MarketplaceConfig.get().functionDistribution.selectProviderFunction();
            provider.configureService(assignedOracleId, qosProfile, finalAdaptiveRange);
            
            logger.fine(String.format("Attached %s to %s (Range: %.2f) [Lat: %.1fms, Policy: %s]", 
                name, hostBroker.getName(), finalAdaptiveRange, qosProfile.get(MarketplaceMetricSchema.METRIC_LATENCY), policy.name()));
        }
    }
    
    private ProviderProfileGenerator.ProviderPolicy assignPolicyForTier(String tierType) {
        double probability = this.random.nextDouble(); 

        return switch (tierType.toUpperCase()) {
            case "CLOUD" -> (probability < 0.80) ? 
                    ProviderProfileGenerator.ProviderPolicy.WARM_OPTIMIZED : 
                    ProviderProfileGenerator.ProviderPolicy.BALANCED;
            case "FOG" -> {
                if (probability < 0.50) yield ProviderProfileGenerator.ProviderPolicy.BALANCED;
                if (probability < 0.80) yield ProviderProfileGenerator.ProviderPolicy.WARM_OPTIMIZED;
                yield ProviderProfileGenerator.ProviderPolicy.COST_OPTIMIZED;
            }
            case "EDGE" -> (probability < 0.70) ? 
                    ProviderProfileGenerator.ProviderPolicy.COST_OPTIMIZED : 
                    ProviderProfileGenerator.ProviderPolicy.BALANCED;
            default -> ProviderProfileGenerator.ProviderPolicy.BALANCED;
        };
    }

    private double calculateCoveringRange(BoundedBroker broker, String depthTag) {
        simulator.regions.SpatialRegion r = broker.getRegion();
        if (r == null) return ProviderProfileGenerator.RADIUS_EDGE; 

        double width = r.getWidth();
        double height = r.getHeight();
        double maxDimension = Math.max(width, height);
        double halfSide = maxDimension / 2.0;

        return switch (depthTag.toUpperCase()) {
            case "COUNTRY" -> Math.min(halfSide * 1.5, ProviderProfileGenerator.RADIUS_CLOUD_MAX);
            case "ADMIN1" -> Math.min(halfSide * 1.2, ProviderProfileGenerator.RADIUS_FOG_MAX);
            case "CITY" -> ProviderProfileGenerator.RADIUS_EDGE;
            default -> halfSide;
        };
    }

    private List<TreeNode> findCandidatesByDepth(TreeNode node, String depthTag) {
        List<TreeNode> results = new ArrayList<>();
        int targetDepth = parseDepth(depthTag);
        traverseAndCollect(node, 0, targetDepth, results);
        return results;
    }

    private void traverseAndCollect(TreeNode node, int currentDepth, int targetDepth, List<TreeNode> results) {
        if (currentDepth == targetDepth) {
            results.add(node);
            return;
        }
        if (node instanceof SimulationBroker sb) {
            for (TreeNode child : sb.getChildren()) {
                traverseAndCollect(child, currentDepth + 1, targetDepth, results);
            }
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