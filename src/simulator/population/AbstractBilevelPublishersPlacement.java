package simulator.population;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;

/**
 * Abstract superclass for publisher placement strategies that operate on a
 * "bilevel" logic:
 * 1. Select a high-level region (e.g., a Level 2 broker).
 * 2. Place the publisher on a leaf broker underneath that region.
 */
public abstract class AbstractBilevelPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(AbstractBilevelPublishersPlacement.class.getName());

    /**
     * Concrete subclasses must implement this method to return a list of
     * Level 2 brokers that will serve as the "placement pool".
     *
     * @param rootNode The root of the broker topology.
     * @return A list of Level 2 brokers to be used for placement.
     */
    protected abstract List<BoundedBroker> createPlacementPool(BoundedBroker rootNode);

    /**
     * Main placement logic. It uses the template method pattern, calling
     * createPlacementPool() to get the list of target regions.
     */
    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate) {
        logger.info("");
        logger.info("--- Starting Bilevel Publisher Placement (" + this.getClass().getSimpleName() + ") ---");

        if (rootNode == null) {
            logger.severe("Error: Root node is null. Cannot find Level 2 regions.");
            return;
        }

        // 1. Get the placement pool from the concrete subclass
        List<BoundedBroker> placementPool = createPlacementPool(rootNode);
        if (placementPool == null || placementPool.isEmpty()) {
            logger.severe("Error: The placement pool is empty. Cannot proceed.");
            return;
        }

        logger.info("Distributing " + totalPublishersToCreate + " replicas *randomly* among " + placementPool.size() + " regions in the pool...");

        long publishersCreated = 0;

        for (long i = 0; i < totalPublishersToCreate; i++) {
            
            // 2. Select a region *randomly* from the pool (Uniform Distribution)
            BoundedBroker chosenMajorRegion = placementPool.get(random.nextInt(placementPool.size()));
            
            // 3. Find a random leaf broker *under* that L2 region
            BoundedBroker chosenLeaf = findRandomLeafBroker(chosenMajorRegion);
            if (chosenLeaf == null) {
                logger.warning("Failed to find a leaf broker under " + chosenMajorRegion.getName() + ". Skipping placement.");
                continue;
            }
            
            Region leafRegion = chosenLeaf.getRegion();
            
            if (leafRegion == null || leafRegion.getBottomLeft() == null || leafRegion.getTopRight() == null) {
                logger.warning("Skipping publisher placement: Chosen leaf broker " + chosenLeaf.getName() + 
                               " has a null or incomplete region object.");
                continue;
            }

            // 4. Place the publisher
            Location pubLocation = generateLocationInRegion(leafRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenLeaf.addChild(publisher);
            publishersCreated++;
            
            logger.info(String.format("  -> Placed %s in LEAF '%s' (inside RANDOMLY SELECTED region '%s')",
                              publisher.getName(), 
                              chosenLeaf.getName(),
                              chosenMajorRegion.getName()
                              ));
        }
        logger.info("--- Data Center Placement Complete. Total replicas placed: " + publishersCreated + " ---");
    }
    
    
    // --- COMMON HELPER METHODS ---

    /**
     * Helper method to find all brokers at a specific level (e.g., 2)
     * by traversing from the root.
     */
    protected List<BoundedBroker> findBrokersAtLevel(BoundedBroker root, int targetLevel) {
        List<BoundedBroker> result = new ArrayList<>();
        if (root == null || root.getNodeLevel() > targetLevel) {
            return result;
        }

        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            BoundedBroker current = queue.poll();

            if (current.getNodeLevel() == targetLevel) {
                result.add(current);
            } else if (current.getNodeLevel() < targetLevel) {
                // Only add children to the queue if they are not beyond the target level
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BoundedBroker childBroker) {
                        queue.add(childBroker);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Helper method to find a random leaf broker *under* a given start node.
     */
    protected BoundedBroker findRandomLeafBroker(BoundedBroker startNode) {
        List<BoundedBroker> leaves = new ArrayList<>();
        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(startNode);
        
        while (!queue.isEmpty()) {
            BoundedBroker current = queue.poll();
            boolean hasBrokerChild = false;
            for (TreeNode child : current.getChildren()) {
                if (child instanceof BoundedBroker childBroker) {
                    queue.add(childBroker);
                    hasBrokerChild = true;
                }
            }
            
            if (!hasBrokerChild) {
                // This is a leaf broker (no more broker children)
                leaves.add(current);
            }
        }
        
        if (leaves.isEmpty()) {
            logger.warning("Could not find any leaf brokers under " + startNode.getName() + ". Using node itself.");
            return startNode;
        }
        
        // Return a random leaf from the collected list
        return leaves.get(random.nextInt(leaves.size()));
    }
}