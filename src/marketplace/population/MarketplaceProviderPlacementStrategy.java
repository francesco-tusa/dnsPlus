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

    private final ProviderProfileGenerator profileGenerator = new ProviderProfileGenerator(random);

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

            Location providerLoc = getBrokerCenter(hostBroker);
            double adaptiveRange = calculateCoveringRange(hostBroker, depthTag);

            // Sanitize the broker name to prevent log parsers from truncating at spaces
            String safeRegionName = hostBroker.getName().replaceAll(" ", "_");
            String name = String.format("%s_%d_%s", providerLabel, i, safeRegionName);
            
            MarketplaceProvider provider = new MarketplaceProvider(name, providerLoc);
            
            hostBroker.addChild(provider);
            
            Map<String, Double> qosProfile = profileGenerator.generateProfile(tierType);

            provider.configureService(serviceId, qosProfile, adaptiveRange);
            
            logger.fine(String.format("Attached %s to %s (Range: %.2f) [Intrinsic Lat: %.1fms]", 
                name, hostBroker.getName(), adaptiveRange, qosProfile.get(MarketplaceMetricSchema.METRIC_LATENCY)));
        }
    }
    

    private double calculateCoveringRange(BoundedBroker broker, String depthTag) {
        Region r = broker.getRegion();
        if (r == null) return 0.25; // Safe default for unmapped leaf (~27km)

        // Delegate to Region class to handle coordinate wrapping correctly
        double width = r.getWidth();
        double height = r.getHeight();
        
        // Use Max Dimension (Square Logic) to ensure coverage
        double maxDimension = Math.max(width, height);
        double halfSide = maxDimension / 2.0;

        switch (depthTag.toUpperCase()) {
            case "COUNTRY": 
                // CLOUD: Use a large multiplier to cover the country, 
                // but strictly CAP it at 20.0 degrees (~2200km) so it doesn't wrap the Earth.
                return Math.min(halfSide * 1.5, 20.0); 
                
            case "ADMIN1": 
                // FOG: Cover the state/region, CAP at 3.0 degrees (~330km)
                return Math.min(halfSide * 1.2, 3.0);
                
            case "CITY": 
                // EDGE: Hardcode to 0.25 degrees (~27km) for strict Metropolitan boundaries.
                // 5.0 was too large (~550km).
                return 0.25; 
                
            default: 
                return halfSide;
        }
    }

    private Location getBrokerCenter(BoundedBroker broker) {
        Region r = broker.getRegion();
        if (r == null) return new Location(0,0,0);
        
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