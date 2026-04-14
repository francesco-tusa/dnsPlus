package marketplace.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import marketplace.config.MarketplaceConfig;
import marketplace.population.tiers.CloudProviderTier;
import marketplace.population.tiers.EdgeProviderTier;
import marketplace.population.tiers.FogProviderTier;
import marketplace.population.tiers.ProviderTierStrategy;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.population.SubscribersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;
import utils.SimulationRandom;

public abstract class AbstractProviderPlacementStrategy implements SubscribersPlacementStrategy {

    protected static final Logger logger = CustomLogger.getLogger(AbstractProviderPlacementStrategy.class.getName());
    
    protected final Random random = SimulationRandom.get();
    protected final MarketplaceConfig config = MarketplaceConfig.get();
    protected final ProviderProfileGenerator profileGenerator;

    public AbstractProviderPlacementStrategy(ProviderProfileGenerator profileGenerator) {
        this.profileGenerator = profileGenerator;
    }

    /**
     * The Template Method for Provider Generation.
     * Centralizes dynamic depth calculation and tier execution.
     */
    @Override
    public final void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        logger.info(">>> Generating " + getStrategyName() + "...");

        // DYNAMIC ROOT CALCULATION
        int maxTreeDepth = getStructuralTreeDepth(rootNode);
        int rootOffset = 4 - maxTreeDepth; 
        logger.info(">>> Topology Depth Detected: " + maxTreeDepth + " (Applying Depth Offset: " + rootOffset + ")");

        if (config.cloudProviderCount > 0) placeTier(rootNode, "COUNTRY", config.cloudProviderCount, new CloudProviderTier(), "AWS_Cloud", rootOffset);
        if (config.fogProviderCount > 0)   placeTier(rootNode, "ADMIN1", config.fogProviderCount, new FogProviderTier(), "Telco_Fog", rootOffset);
        if (config.edgeProviderCount > 0)  placeTier(rootNode, "CITY", config.edgeProviderCount, new EdgeProviderTier(), "Metro_Edge", rootOffset);
    }

    protected abstract String getStrategyName();

    protected abstract void placeTier(BoundedBroker root, String depthTag, int count, ProviderTierStrategy tierStrategy, String providerLabel, int rootOffset);

    // --- SHARED TOPOLOGY & PLACEMENT LOGIC ---

    protected Location extractLocation(BoundedBroker hostBroker) {
        if (hostBroker.getRegion() != null) {
            if (hostBroker.getRegion() instanceof Region trueRegion) return trueRegion.getRandomLocation(this.random);
            else return hostBroker.getRegion().getRandomLocation();
        }
        return new Location(0, 0, 0);
    }

    protected double calculateCoveringRange(BoundedBroker broker, ProviderTierStrategy tier) {
        double generatedRadius = tier.generateCoverageRadius(this.random);

        // Bypass check based on properties config
        if (!config.enableSpatialClipping) {
            return generatedRadius;
        }

        simulator.regions.SpatialRegion r = broker.getRegion();
        if (r == null) return generatedRadius; 

        double maxDimension = Math.max(r.getWidth(), r.getHeight());
        double halfSide = maxDimension / 2.0;

        // Clean polymorphic clamp using the Tier's specific multiplier
        return Math.min(halfSide * tier.getRegionClippingMultiplier(), generatedRadius);
    }

    private int getStructuralTreeDepth(TreeNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) return 0;
        int max = 0;
        for (TreeNode child : node.getChildren()) {
            if (child instanceof SimulationBroker) max = Math.max(max, getStructuralTreeDepth(child));
        }
        return max + 1;
    }

    protected List<TreeNode> findCandidatesByDepth(TreeNode node, String depthTag, int rootOffset) {
        List<TreeNode> results = new ArrayList<>();
        int targetDepth = switch (depthTag.toUpperCase()) {
            case "CONTINENT" -> 1;
            case "COUNTRY" -> 2;
            case "ADMIN1" -> 3;
            case "CITY" -> 4;
            default -> 4;
        };
        traverseAndCollect(node, rootOffset, targetDepth, results);
        return results;
    }

    private void traverseAndCollect(TreeNode node, int currentDepth, int targetDepth, List<TreeNode> results) {
        if (currentDepth == targetDepth) { results.add(node); return; }
        if (node instanceof SimulationBroker sb) {
            for (TreeNode child : sb.getChildren()) traverseAndCollect(child, currentDepth + 1, targetDepth, results);
        }
    }
}