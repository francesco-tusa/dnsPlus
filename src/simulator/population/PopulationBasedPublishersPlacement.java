package simulator.population;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.regions.BoundedBroker;
import utils.CustomLogger;

/**
 * Implements a placement strategy based on population.
 * 1. Finds all Level 2 brokers.
 * 2. Sorts them by population.
 * 3. Takes the Top-N (poolSize) to create the placement pool.
 */
public class PopulationBasedPublishersPlacement extends AbstractBilevelPublishersPlacement {

    private static final Logger logger = CustomLogger.getLogger(PopulationBasedPublishersPlacement.class.getName());
    private final int poolSize;

    public PopulationBasedPublishersPlacement(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be positive.");
        }
        this.poolSize = poolSize;
    }

    @Override
    protected List<BoundedBroker> createPlacementPool(BoundedBroker rootNode) {
        logger.info("Creating placement pool: Finding all Level 2 regions...");
        
        // 1. Find all brokers at Level 2
        List<BoundedBroker> level2MajorRegions = findBrokersAtLevel(rootNode, 2);
        if (level2MajorRegions.isEmpty()) {
            logger.severe("Error: No parent brokers were found at Level 2.");
            return null;
        }
        
        // 2. Sort these Level 2 regions by population
        level2MajorRegions.sort(Comparator.comparingLong(BoundedBroker::getInternetPopulation).reversed());

        // 3. Select the Top-N (poolSize) to form the "Placement Pool"
        int numToTake = Math.min(level2MajorRegions.size(), this.poolSize);
        List<BoundedBroker> topRegionsPool = level2MajorRegions.subList(0, numToTake);

        logger.info("Identified top " + topRegionsPool.size() + " MAJOR regions (at Level 2) as Data Center *Placement Pool*.");
        if (logger.isLoggable(Level.INFO)) { 
            logger.info("  --- DEBUG: Top " + Math.min(20, topRegionsPool.size()) + " (of " + topRegionsPool.size() + ") Regions in Pool ---");
            for (int i = 0; i < Math.min(20, topRegionsPool.size()); i++) {
                BoundedBroker region = topRegionsPool.get(i);
                logger.info(String.format("  [Rank %d] %s (Pop: %d, Level: %d)", 
                                          i + 1, 
                                          region.getName(), 
                                          region.getInternetPopulation(),
                                          region.getNodeLevel()
                                          ));
            }
            if (topRegionsPool.size() > 20) {
                 logger.info("  ... (and " + (topRegionsPool.size() - 20) + " more regions in pool)");
            }
            logger.info("  -----------------------------------------------------------");
        }
        
        return topRegionsPool;
    }
}