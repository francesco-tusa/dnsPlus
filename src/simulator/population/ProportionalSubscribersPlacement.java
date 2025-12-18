package simulator.population;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;

public class ProportionalSubscribersPlacement implements SubscribersPlacementStrategy {

    // Get the logger instance
    private static final Logger logger = CustomLogger.getLogger(ProportionalSubscribersPlacement.class.getName());

    private final Random random = new Random();
    private int subscriberIdCounter = 0;

    public ProportionalSubscribersPlacement() {
        // Removed debug flag
    }

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        logger.info("");
        logger.info("--- Starting Proportional Subscriber Placement ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            logger.severe("Error: The provided list of leaf brokers is empty. Cannot generate subscribers.");
            return;
        }

        long worldTotalInternetPopulation = rootNode.getInternetPopulation();
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * worldTotalInternetPopulation);
            BoundedBroker chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();

                // Ensure the region is valid before attempting placement
                if (brokerRegion == null || brokerRegion.getBottomLeft() == null) {
                    continue; 
                }

                Location subLocation = generateLocationInRegion(brokerRegion);
                SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
                chosenBroker.addChild(subscriber);
                subscribersCreated++;
            }
        }
        
        logger.info("--- Placement Complete. Total created: " + subscribersCreated + " ---");
    }

    private BoundedBroker findBrokerForWeight(long weight, List<BoundedBroker> brokers, long[] cumulativeWeights) {
        int low = 0;
        int high = cumulativeWeights.length - 1;
        int ans = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (cumulativeWeights[mid] > weight) {
                ans = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return (ans != -1) ? brokers.get(ans) : null;
    }

    private void generateAndAttachUniformly(List<BoundedBroker> leafBrokers, long totalSubscribersToCreate) {
        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            BoundedBroker chosenBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));

            Region brokerRegion = chosenBroker.getRegion();
            if (brokerRegion == null || brokerRegion.getBottomLeft() == null) continue;

            Location subLocation = generateLocationInRegion(brokerRegion);
            SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
            chosenBroker.addChild(subscriber);
            subscribersCreated++;
        }
         logger.info("--- Uniform Subscriber Placement Complete. Total subscribers created: " + subscribersCreated + " ---");
    }

    private Location generateLocationInRegion(Region region) {
        // Ensure region is valid before attempting to get location
        Objects.requireNonNull(region, "Region cannot be null");
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        Objects.requireNonNull(bl, "Region's bottom-left corner cannot be null");
        Objects.requireNonNull(tr, "Region's top-right corner cannot be null");

        double rangeX = tr.getX() - bl.getX();
        double rangeY = tr.getY() - bl.getY();
        double rangeZ = tr.getZ() - bl.getZ();
        
        // Handle cases where region is just a point or a line
        double randomX = bl.getX() + (rangeX > 0 ? random.nextDouble() * rangeX : 0);
        double randomY = bl.getY() + (rangeY > 0 ? random.nextDouble() * rangeY : 0);
        double randomZ = bl.getZ() + (rangeZ > 0 ? random.nextDouble() * rangeZ : 0);
        
        return new Location(randomX, randomY, randomZ);
    }

    private String generateSubscriberName() {
        return "Sub-" + subscriberIdCounter++;
    }
}