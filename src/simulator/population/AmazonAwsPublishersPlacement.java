package simulator.population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import utils.CustomLogger;

/**
 * Places publishers (replicas) to mimic real-world AWS data center locations.
 *
 * --- Implementation ---
 * 1. Defines a static, curated list of "AwsRegion" paths (Country, AdminDivision, City).
 * 2. Finds all Level 2 (Country) brokers from the topology.
 * 3. For each publisher, it picks a random AwsRegion from the list.
 * 4. It traverses the topology: L2 (Country) -> L3 (AdminDivision) -> L4+ (City).
 * 5. It places the publisher on a random leaf broker under the most specific
 * node found (e.g., under the "Frankfurt" broker, or falling back to "Hesse").
 */
public class AmazonAwsPublishersPlacement extends AbstractBilevelPublishersPlacement {
    // Note: We extend the abstract class to reuse the helper methods,
    // but we will override generateAndAttach entirely.

    private static final Logger logger = CustomLogger.getLogger(AmazonAwsPublishersPlacement.class.getName());

    /**
     * A simple data structure to hold the 3-level path for an AWS region.
     */
    private record AwsRegion(String l2Country, String l3AdminDivision, String l4City, String awsRegionName) {}

    // --- Curated List of Real-World AWS Regions ---
    // This list is based on the user-provided table.
    private static final List<AwsRegion> AWS_REGION_LIST = Arrays.asList(
        // Americas
        new AwsRegion("United States", "Virginia", "Ashburn", "us-east-1"),
        new AwsRegion("United States", "Ohio", "New Albany", "us-east-2"), // Note: City is New Albany
        new AwsRegion("United States", "California", "Santa Clara", "us-west-1"), // Note: City is Santa Clara
        new AwsRegion("United States", "Oregon", "Boardman", "us-west-2"),
        new AwsRegion("Canada", "Quebec", "Montreal", "ca-central-1"),
        new AwsRegion("Canada", "Alberta", "Calgary", "ca-west-1"),
        new AwsRegion("Mexico", "Querétaro", "Querétaro", "mx-central-1"),
        new AwsRegion("Brazil", "São Paulo", "São Paulo", "sa-east-1"),

        // Asia Pacific (APAC) & Oceania
        new AwsRegion("South Africa", "Western Cape", "Cape Town", "af-south-1"),
        new AwsRegion("Hong Kong", "Hong Kong", "Hong Kong", "ap-east-1"),
        // new AwsRegion("Taiwan", "Taipei City", "Taipei", "ap-east-2"), // L2 name might be "Taiwan"
        new AwsRegion("India", "Maharashtra", "Mumbai", "ap-south-1"),
        new AwsRegion("India", "Telangana", "Hyderabad", "ap-south-2"),
        new AwsRegion("Singapore", "Singapore", "Singapore", "ap-southeast-1"),
        new AwsRegion("Australia", "New South Wales", "Sydney", "ap-southeast-2"),
        new AwsRegion("Indonesia", "DKI Jakarta", "Jakarta", "ap-southeast-3"),
        new AwsRegion("Australia", "Victoria", "Melbourne", "ap-southeast-4"),
        new AwsRegion("Malaysia", "Kuala Lumpur", "Kuala Lumpur", "ap-southeast-5"),
        new AwsRegion("New Zealand", "Auckland Region", "Auckland", "ap-southeast-6"),
        new AwsRegion("Thailand", "Bangkok", "Bangkok", "ap-southeast-7"),
        new AwsRegion("Japan", "Tokyo Metropolis", "Tokyo", "ap-northeast-1"),
        new AwsRegion("South Korea", "Seoul Special City", "Seoul", "ap-northeast-2"),
        new AwsRegion("Japan", "Osaka Prefecture", "Osaka", "ap-northeast-3"),

        // Europe, Middle East & Africa (EMEA)
        new AwsRegion("Germany", "Hesse", "Frankfurt", "eu-central-1"),
        new AwsRegion("Switzerland", "Zurich", "Zurich", "eu-central-2"),
        new AwsRegion("Ireland", "Leinster", "Dublin", "eu-west-1"),
        new AwsRegion("United Kingdom", "England", "London", "eu-west-2"),
        new AwsRegion("France", "Île-de-France", "Paris", "eu-west-3"),
        new AwsRegion("Sweden", "Stockholm County", "Stockholm", "eu-north-1"),
        new AwsRegion("Italian Republic", "Lombardia", "Milan", "eu-south-1"), // L2 name is "Italian Republic"
        new AwsRegion("Spain", "Aragón", "Zaragoza", "eu-south-2"),
        new AwsRegion("Israel", "Tel Aviv District", "Tel Aviv", "il-central-1"),
        new AwsRegion("Bahrain", "Capital Governorate", "Manama", "me-south-1"),
        new AwsRegion("United Arab Emirates", "Emirate of Dubai", "Dubai", "me-central-1")
    );


    public AmazonAwsPublishersPlacement() {
        // No arguments needed
    }
    
    /**
     * This implementation is not needed as we override generateAndAttach.
     */
    @Override
    protected List<BrokerWithRegion> createPlacementPool(BrokerWithRegion rootNode) {
        return null; // Not used
    }

    /**
     * Recursively searches *under* a startNode for a child/grandchild/etc.
     * with the specified name.
     * @return The found BrokerWithRegion, or null.
     */
    private BrokerWithRegion findNodeRecursively(BrokerWithRegion startNode, String nodeName) {
        if (startNode == null || nodeName == null) return null;

        Queue<BrokerWithRegion> queue = new LinkedList<>();
        // Start the search with the children of the startNode
        for (TreeNode child : startNode.getChildren()) {
            if (child instanceof BrokerWithRegion childBroker) {
                queue.add(childBroker);
            }
        }

        while (!queue.isEmpty()) {
            BrokerWithRegion current = queue.poll();
            
            if (current.getName().equals(nodeName)) {
                return current; // Found it
            }

            // Add its children to the queue
            for (TreeNode child : current.getChildren()) {
                if (child instanceof BrokerWithRegion childBroker) {
                    queue.add(childBroker);
                }
            }
        }
        return null; // Not found
    }

    /**
     * Main placement logic. Overrides the abstract parent's method entirely.
     */
    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        logger.info("\n--- Starting AWS-Based Data Center Publisher Placement ---");

        if (rootNode == null) {
             logger.severe("Error: Root node is null. Cannot find Level 2 regions.");
             return;
        }

        // --- 1. Find all Level 2 brokers and map them by name ---
        List<BrokerWithRegion> allLevel2Regions = findBrokersAtLevel(rootNode, 2);
        if (allLevel2Regions.isEmpty()) {
            logger.severe("Error: No brokers were found at Level 2.");
            return;
        }

        Map<String, BrokerWithRegion> l2BrokerMap = allLevel2Regions.stream()
            .collect(Collectors.toMap(TreeNode::getName, b -> b, (b1, b2) -> b1)); // Handle potential name duplicates
        
        logger.info("Mapped " + l2BrokerMap.size() + " unique Level 2 brokers.");

        // --- 2. Filter our AWS list to only include L2 regions we actually found ---
        List<AwsRegion> availableAwsRegions = AWS_REGION_LIST.stream()
            .filter(awsRegion -> l2BrokerMap.containsKey(awsRegion.l2Country()))
            .collect(Collectors.toList());

        if (availableAwsRegions.isEmpty()) {
            logger.severe("Error: No AWS regions from the curated list were found in the topology's Level 2 brokers.");
            return;
        }

        logger.info("Found " + availableAwsRegions.size() + " matching AWS regions in the topology. This is the new *Placement Pool*.");
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("  --- DEBUG: Matched AWS Regions ---");
            for (AwsRegion region : availableAwsRegions) {
                logger.fine("  - " + region.awsRegionName() + " (" + region.l2Country() + ")");
            }
            logger.fine("  ----------------------------------");
        }
        
        // --- 3. Distribute publishers ---
        logger.info("Distributing " + totalPublishersToCreate + " replicas *randomly* among these " + availableAwsRegions.size() + " regions...");
        long publishersCreated = 0;
        
        for (long i = 0; i < totalPublishersToCreate; i++) {
            
            // 4. Select a region *randomly* from the AWS pool
            AwsRegion chosenAwsRegion = availableAwsRegions.get(random.nextInt(availableAwsRegions.size()));
            
            // 5. Start the hierarchical search
            BrokerWithRegion l2Broker = l2BrokerMap.get(chosenAwsRegion.l2Country());
            BrokerWithRegion placementBase = l2Broker; // Default to L2
            String placementLevelLog = "L2 (Country)";

            // 6. Try to find L3 (Admin Division)
            BrokerWithRegion l3Broker = findNodeRecursively(l2Broker, chosenAwsRegion.l3AdminDivision());
            if (l3Broker != null) {
                placementBase = l3Broker;
                placementLevelLog = "L3 (AdminDiv)";

                // 7. Try to find L4 (City)
                BrokerWithRegion l4Broker = findNodeRecursively(l3Broker, chosenAwsRegion.l4City());
                if (l4Broker != null) {
                    placementBase = l4Broker;
                    placementLevelLog = "L4+ (City)";
                } else {
                    logger.fine("Could not find City node '" + chosenAwsRegion.l4City() + "' under '" + l3Broker.getName() + "'. Placing under L3.");
                }
            } else {
                 logger.fine("Could not find AdminDiv node '" + chosenAwsRegion.l3AdminDivision() + "' under '" + l2Broker.getName() + "'. Placing under L2.");
            }

            // 8. Find a random leaf broker *under* the most specific node we found
            BrokerWithRegion chosenLeaf = findRandomLeafBroker(placementBase);
            if (chosenLeaf == null) {
                 logger.warning("Failed to find a leaf broker under " + placementBase.getName() + ". Skipping placement.");
                 continue;
            }
            
            Region leafRegion = chosenLeaf.getRegion();
            if (leafRegion == null || leafRegion.getBottomLeft() == null || leafRegion.getTopRight() == null) {
                logger.warning("Skipping publisher placement: Chosen leaf broker " + chosenLeaf.getName() + "Entry has a null or incomplete region object.");
                continue;
            }

            // 9. Place the publisher
            Location pubLocation = generateLocationInRegion(leafRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenLeaf.addChild(publisher);
            publishersCreated++;
            
            logger.info(String.format("  -> Placed %s in LEAF '%s' (found %s '%s' for AWS Region '%s')",
                              publisher.getName(), 
                              chosenLeaf.getName(),
                              placementLevelLog,
                              placementBase.getName(),
                              chosenAwsRegion.awsRegionName()
                              ));
        }
        logger.info("--- Data Center Placement Complete. Total replicas placed: " + publishersCreated + " ---");
    }
}