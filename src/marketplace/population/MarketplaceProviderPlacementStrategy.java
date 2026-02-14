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

/**
 * STRATEGY UPDATE:
 * 1. Uses SimulationRandom for global reproducibility.
 * 2. Generates UNIQUE QoS profiles for every single provider.
 * 3. Models "Processing Latency" (Cloud=Fast, Edge=Slow).
 * 4. Uses Region class methods for dimension and center calculations.
 */
public class MarketplaceProviderPlacementStrategy implements SubscribersPlacementStrategy {
    
    private static final Logger logger = CustomLogger.getLogger(MarketplaceProviderPlacementStrategy.class.getName());
    
    private final Random random = SimulationRandom.get();

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        logger.info(">>> Generating Marketplace Providers (Continuum Placement)...");
        
        MarketplaceConfig config = MarketplaceConfig.get();
        long serviceID = 9999;

        // 1. CLOUD TIER
        if (config.cloudProviderCount > 0) {
            placeTier(rootNode, "COUNTRY", config.cloudProviderCount, serviceID, "CLOUD", "AWS_Cloud");
        }

        // 2. FOG TIER
        if (config.fogProviderCount > 0) {
            placeTier(rootNode, "ADMIN1", config.fogProviderCount, serviceID, "FOG", "Telco_Fog");
        }

        // 3. EDGE TIER
        if (config.edgeProviderCount > 0) {
            placeTier(rootNode, "CITY", config.edgeProviderCount, serviceID, "EDGE", "Metro_Edge");
        }
    }

    private void placeTier(BoundedBroker root, String depthTag, int count, long serviceId, String tierType, String providerLabel) {
        List<TreeNode> candidates = findCandidatesByDepth(root, depthTag);
        
        if (candidates.isEmpty()) {
            logger.warning("No candidates found for level " + depthTag);
            return;
        }

        logger.info(String.format("Placing %d '%s' Providers on %s nodes.", count, providerLabel, depthTag));

        for (int i = 0; i < count; i++) {
            TreeNode targetNode = candidates.get(random.nextInt(candidates.size()));
            
            if (!(targetNode instanceof BoundedBroker)) continue;
            BoundedBroker hostBroker = (BoundedBroker) targetNode;

            // [FIX] Use Region.getCenter()
            Location providerLoc = getBrokerCenter(hostBroker);
            
            // [FIX] Use Region.getWidth()/getHeight()
            double adaptiveRange = calculateCoveringRange(hostBroker, depthTag);

            String name = String.format("%s_%d_%s", providerLabel, i, hostBroker.getName());
            MarketplaceProvider provider = new MarketplaceProvider(name, providerLoc);
            
            hostBroker.addChild(provider);
            
            Map<String, Double> qosProfile = generateRandomProfile(tierType);

            provider.configureService(serviceId, qosProfile, adaptiveRange);
            
            logger.fine(String.format("Attached %s to %s (Range: %.2f) [Lat: %.1fms]", 
                name, hostBroker.getName(), adaptiveRange, qosProfile.get(MarketplaceMetricSchema.METRIC_LATENCY)));
        }
    }

    private Map<String, Double> generateRandomProfile(String tierType) {
        double lat, cost, rel;

        switch (tierType) {
            case "CLOUD":
                lat = 10.0 + (random.nextDouble() * 10.0);
                cost = 2.0 + (random.nextDouble() * 3.0);
                rel = 0.999 + (random.nextDouble() * 0.0009);
                break;
            case "FOG":
                lat = 30.0 + (random.nextDouble() * 30.0);
                cost = 15.0 + (random.nextDouble() * 10.0);
                rel = 0.99 + (random.nextDouble() * 0.009);
                break;
            case "EDGE":
            default:
                lat = 80.0 + (random.nextDouble() * 60.0);
                cost = 50.0 + (random.nextDouble() * 30.0);
                rel = 0.95 + (random.nextDouble() * 0.04);
                break;
        }

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, lat,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, rel
        );
    }

    private double calculateCoveringRange(BoundedBroker broker, String depthTag) {
        Region r = broker.getRegion();
        if (r == null) return 5.0; 

        // [FIX] Delegate to Region class to handle coordinate wrapping correctly
        double width = r.getWidth();
        double height = r.getHeight();
        
        // Use Max Dimension (Square Logic) to ensure coverage
        double maxDimension = Math.max(width, height);
        double halfSide = maxDimension / 2.0;

        switch (depthTag.toUpperCase()) {
            case "COUNTRY": return Math.max(halfSide * 3.0, 500.0); 
            case "ADMIN1": return halfSide * 1.5;
            case "CITY": return 5.0; 
            default: return halfSide;
        }
    }

    private Location getBrokerCenter(BoundedBroker broker) {
        Region r = broker.getRegion();
        if (r == null) return new Location(0,0,0);
        
        // [FIX] Delegate to Region.getCenter()
        return r.getCenter();
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
        switch (tag.toUpperCase()) {
            case "CONTINENT": return 1;
            case "COUNTRY": return 2;
            case "ADMIN1": return 3;
            case "CITY": return 4;
            default: return 4;
        }
    }
}