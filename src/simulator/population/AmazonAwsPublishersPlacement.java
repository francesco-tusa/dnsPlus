package simulator.population;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
 */
public class AmazonAwsPublishersPlacement extends AbstractBilevelPublishersPlacement {

    private static final Logger logger = CustomLogger.getLogger(AmazonAwsPublishersPlacement.class.getName());

    private record AwsRegion(String l2Country, String l3AdminDivision, String l4City, String awsRegionName) {}

    private static final List<AwsRegion> AWS_REGION_LIST = Arrays.asList(
        new AwsRegion("United States", "Virginia", "Ashburn", "us-east-1"),
        new AwsRegion("United States", "Ohio", "New Albany", "us-east-2"),
        new AwsRegion("United States", "California", "Santa Clara", "us-west-1"),
        new AwsRegion("United States", "Oregon", "Boardman", "us-west-2"),
        new AwsRegion("Canada", "Quebec", "Montreal", "ca-central-1"),
        new AwsRegion("Canada", "Alberta", "Calgary", "ca-west-1"),
        new AwsRegion("Mexico", "Querétaro", "Querétaro", "mx-central-1"),
        new AwsRegion("Brazil", "São Paulo", "São Paulo", "sa-east-1"),
        new AwsRegion("South Africa", "Western Cape", "Cape Town", "af-south-1"),
        new AwsRegion("Hong Kong", "Hong Kong", "Hong Kong", "ap-east-1"),
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
        new AwsRegion("Germany", "Hesse", "Frankfurt", "eu-central-1"),
        new AwsRegion("Switzerland", "Zurich", "Zurich", "eu-central-2"),
        new AwsRegion("Ireland", "Leinster", "Dublin", "eu-west-1"),
        new AwsRegion("United Kingdom", "England", "London", "eu-west-2"),
        new AwsRegion("France", "Île-de-France", "Paris", "eu-west-3"),
        new AwsRegion("Sweden", "Stockholm County", "Stockholm", "eu-north-1"),
        new AwsRegion("Italian Republic", "Lombardia", "Milan", "eu-south-1"),
        new AwsRegion("Spain", "Aragón", "Zaragoza", "eu-south-2"),
        new AwsRegion("Israel", "Tel Aviv District", "Tel Aviv", "il-central-1"),
        new AwsRegion("Bahrain", "Capital Governorate", "Manama", "me-south-1"),
        new AwsRegion("United Arab Emirates", "Emirate of Dubai", "Dubai", "me-central-1")
    );

    public AmazonAwsPublishersPlacement() {}
    
    @Override
    protected List<BrokerWithRegion> createPlacementPool(BrokerWithRegion rootNode) {
        return null; 
    }

    private BrokerWithRegion findNodeRecursively(BrokerWithRegion startNode, String nodeName) {
        if (startNode == null || nodeName == null) return null;
        Queue<BrokerWithRegion> queue = new LinkedList<>();
        for (TreeNode child : startNode.getChildren()) {
            if (child instanceof BrokerWithRegion childBroker) {
                queue.add(childBroker);
            }
        }
        while (!queue.isEmpty()) {
            BrokerWithRegion current = queue.poll();
            if (current.getName().equals(nodeName)) return current;
            for (TreeNode child : current.getChildren()) {
                if (child instanceof BrokerWithRegion childBroker) {
                    queue.add(childBroker);
                }
            }
        }
        return null;
    }

    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        logger.info("\n--- Starting AWS-Based Data Center Publisher Placement ---");

        if (rootNode == null) {
             logger.severe("Error: Root node is null. Cannot find Level 2 regions.");
             return;
        }

        List<BrokerWithRegion> allLevel2Regions = findBrokersAtLevel(rootNode, 2);
        if (allLevel2Regions.isEmpty()) {
            logger.severe("Error: No brokers were found at Level 2.");
            return;
        }

        Map<String, BrokerWithRegion> l2BrokerMap = allLevel2Regions.stream()
            .collect(Collectors.toMap(TreeNode::getName, b -> b, (b1, b2) -> b1)); 
        
        List<AwsRegion> availableAwsRegions = AWS_REGION_LIST.stream()
            .filter(awsRegion -> l2BrokerMap.containsKey(awsRegion.l2Country()))
            .collect(Collectors.toList());

        if (availableAwsRegions.isEmpty()) {
            logger.severe("Error: No AWS regions from the curated list were found in the topology.");
            return;
        }

        logger.info("Found " + availableAwsRegions.size() + " matching AWS regions. Distributing " + totalPublishersToCreate + " replicas.");

        long publishersCreated = 0;
        int maxAttempts = (int) totalPublishersToCreate * 5; // Safety break
        int attempts = 0;

        // Retry loop to ensure we get exactly totalPublishersToCreate
        while (publishersCreated < totalPublishersToCreate && attempts < maxAttempts) {
            attempts++;
            
            AwsRegion chosenAwsRegion = availableAwsRegions.get(random.nextInt(availableAwsRegions.size()));
            BrokerWithRegion l2Broker = l2BrokerMap.get(chosenAwsRegion.l2Country());
            BrokerWithRegion placementBase = l2Broker;
            String placementLevelLog = "L2";

            BrokerWithRegion l3Broker = findNodeRecursively(l2Broker, chosenAwsRegion.l3AdminDivision());
            if (l3Broker != null) {
                placementBase = l3Broker;
                placementLevelLog = "L3";
                BrokerWithRegion l4Broker = findNodeRecursively(l3Broker, chosenAwsRegion.l4City());
                if (l4Broker != null) {
                    placementBase = l4Broker;
                    placementLevelLog = "L4";
                }
            }

            BrokerWithRegion chosenLeaf = findRandomLeafBroker(placementBase);
            if (chosenLeaf == null) continue;
            
            Region leafRegion = chosenLeaf.getRegion();
            // If region is invalid (like Singapore in previous logs), retry.
            if (leafRegion == null || leafRegion.getBottomLeft() == null) {
                logger.fine("Skipping placement at " + chosenLeaf.getName() + ": Invalid region. Retrying...");
                continue; 
            }

            Location pubLocation = generateLocationInRegion(leafRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenLeaf.addChild(publisher);
            publishersCreated++;
            
            logger.info(String.format("  -> Placed %s in LEAF '%s' (AWS: %s)", publisher.getName(), chosenLeaf.getName(), chosenAwsRegion.awsRegionName()));
        }
        
        if (publishersCreated < totalPublishersToCreate) {
            logger.warning("Could only place " + publishersCreated + " out of " + totalPublishersToCreate + " publishers after " + attempts + " attempts.");
        } else {
            logger.info("--- Placement Complete. Total replicas placed: " + publishersCreated + " ---");
        }
    }
}