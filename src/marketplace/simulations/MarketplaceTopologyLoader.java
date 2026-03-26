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
            
            // 1. Capture the potentially hoisted root returned by the pruner
            BoundedBroker effectiveRoot = pruneTopology(worldRoot);
            
            logger.info(">>> Recalculating Population Statistics for Sliced Topology...");
            recalculatePopulation(effectiveRoot);
            logger.info(">>> New Root Population: " + effectiveRoot.getInternetPopulation());
            
            logger.info(">>> Sanitizing broker names to use underscores for log consistency...");
            sanitizeBrokerNames(effectiveRoot);
            
            // 2. Return the new root (which will be the Country if only 1 was selected)
            return effectiveRoot;
        }

        return worldRoot;
    }

    /**
     * Prunes unselected countries and empty continents.
     * Returns the hoisted Country node if it is the only one in the slice, 
     * otherwise returns the original World root.
     */
    private BoundedBroker pruneTopology(BoundedBroker root) {
        List<TreeNode> continents = root.getChildren();
        if (continents == null || continents.isEmpty()) return root;

        int keptCountries = 0;
        int removedCountries = 0;
        List<TreeNode> emptyContinents = new ArrayList<>();
        BoundedBroker singleCountryNode = null; // Track the node to potentially hoist

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
                    singleCountryNode = (BoundedBroker) countryNode;
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

       // 4. If exactly one country remains in the slice, hoist it to the root
        if (keptCountries == 1 && singleCountryNode != null) {
            logger.info(">>> Single country slice detected. Hoisting '" + singleCountryNode.getName() + "' to be the topology root.");
            
            // --- Sever the upward network link ---
            TreeNode oldParent = singleCountryNode.getParent();
            if (oldParent != null) {
                oldParent.removeChild(singleCountryNode); 
            }
            return singleCountryNode;
        }

        return root; // Fallback to World root if processing a multi-country slice
    }

    /**
     * Recursively updates the internet population of parent nodes 
     * based on the sum of their remaining children.
     */
    private long recalculatePopulation(TreeNode node) {
        // Base case: If it's a leaf (City or bottom-most node), we trust its current population.
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

    /**
     * Recursively traverses the topology tree and replaces all spaces 
     * in the broker names with underscores to ensure clean log parsing.
     */
    private void sanitizeBrokerNames(TreeNode node) {
        if (node == null) return;
        
        String currentName = node.getName();
        if (currentName != null && currentName.contains(" ")) {
            // Replace spaces with underscores
            node.setName(currentName.replaceAll("\\s+", "_"));
        }
        
        // Recurse for all children in the tree
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                sanitizeBrokerNames(child);
            }
        }
    }
}