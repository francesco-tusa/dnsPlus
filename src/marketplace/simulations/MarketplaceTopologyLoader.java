package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import marketplace.config.MarketplaceConfig;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.factories.BoundedBrokerFactory;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import utils.CustomLogger;

public class MarketplaceTopologyLoader extends GeoNamesTopologyLoader {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceTopologyLoader.class.getName());
    private final MarketplaceTopologyConfiguration marketConfig;

    public MarketplaceTopologyLoader(MarketplaceTopologyConfiguration config, BrokerFactory factory) {
        super((BoundedBrokerFactory) factory);
        this.marketConfig = config;
    }

    @Override
    protected BoundedBroker buildCoreTopology() {
        BoundedBroker worldRoot = super.buildCoreTopology();

        if (marketConfig != null && worldRoot != null) {
            logger.info(">>> Pruning Topology based on Slice: " + MarketplaceConfig.get().allowedCountries);
            pruneTopology(worldRoot);
            
            logger.info(">>> Recalculating Population Statistics for Sliced Topology...");
            recalculatePopulation(worldRoot);
            logger.info(">>> New Root Population: " + worldRoot.getInternetPopulation());
        }

        return worldRoot;
    }

    private void pruneTopology(BoundedBroker root) {
        List<TreeNode> continents = root.getChildren();
        if (continents == null || continents.isEmpty()) return;

        int keptCountries = 0;
        int removedCountries = 0;
        List<TreeNode> emptyContinents = new ArrayList<>();

        for (TreeNode continent : continents) {
            List<TreeNode> countries = continent.getChildren();
            
            // If a continent has no countries loaded, mark it for removal
            if (countries == null || countries.isEmpty()) {
                emptyContinents.add(continent);
                continue;
            }

            List<TreeNode> toRemove = new ArrayList<>();

            for (TreeNode countryNode : countries) {
                String name = countryNode.getName();
                
                // Normalise check
                if (!marketConfig.isCountryAllowed(name)) {
                    toRemove.add(countryNode);
                    removedCountries++;
                } else {
                    keptCountries++;
                    logger.info("  >>> KEEPING Country: " + name + " (Children: " + countryNode.getChildren().size() + ")");
                }
            }

            // 1. Remove the forbidden countries from the continent
            countries.removeAll(toRemove);

            // 2. If the continent is now empty, mark it for removal from the World
            if (countries.isEmpty()) {
                emptyContinents.add(continent);
            }
        }
        
        // 3. Remove all empty continents from the Root
        if (!emptyContinents.isEmpty()) {
            root.getChildren().removeAll(emptyContinents);
            logger.info(">>> Removed " + emptyContinents.size() + " empty Continents (e.g., Europe, Asia) to fix population stats.");
        }
        
        logger.info(String.format(">>> Pruning Complete. Kept %d Countries. Removed %d Countries.", keptCountries, removedCountries));
    }

    /**
     * Recursively updates the internet population of parent nodes 
     * based on the sum of their remaining children.
     */
    private long recalculatePopulation(TreeNode node) {
        // Base case: If it's a leaf (City or bottom-most node), we trust its current population.
        // Because we removed empty continents, any node with 0 children is a TRUE leaf (with valid data).
        if (node.getChildren().isEmpty()) {
            if (node instanceof BoundedBroker) {
                return ((BoundedBroker) node).getInternetPopulation();
            }
            return 0;
        }

        long newTotal = 0;
        for (TreeNode child : node.getChildren()) {
            newTotal += recalculatePopulation(child);
        }

        if (node instanceof BoundedBroker) {
            ((BoundedBroker) node).setInternetPopulation(newTotal);
        }

        return newTotal;
    }
}