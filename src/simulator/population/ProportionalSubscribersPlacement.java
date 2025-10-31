package simulator.population;

import java.util.List;
import java.util.Random;
import java.util.logging.Level; // Import Level
import java.util.logging.Logger; // Import Logger
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import utils.CustomLogger; // Import CustomLogger

public class ProportionalSubscribersPlacement implements SubscribersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(ProportionalSubscribersPlacement.class.getName());

    private final Random random = new Random();
    private int subscriberIdCounter = 0;
    private static final int DEBUG_SAMPLE_SIZE = 10; // Log this many placements

    public ProportionalSubscribersPlacement() {
        // Default constructor, no debug flag needed
    }
    
    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalSubscribersToCreate) {
        System.out.println("\n--- Starting Proportional Subscriber Placement ---");
        System.out.println("Distributing " + totalSubscribersToCreate + " total subscribers...");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: The provided list of leaf brokers is empty. Cannot generate subscribers.");
            return;
        }

        long worldTotalInternetPopulation = rootNode.getInternetPopulation();

        if (worldTotalInternetPopulation == 0) {
            System.err.println("Warning: Total internet population is zero. Using uniform random distribution.");
            generateAndAttachUniformly(leafBrokers, totalSubscribersToCreate);
            return;
        }
        
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        boolean isDebug = logger.isLoggable(Level.FINE);
        if (isDebug) {
            logger.fine("  --- DEBUG: Subscriber Placement (Sample) ---");
        }

        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * worldTotalInternetPopulation);
            BrokerWithRegion chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null || brokerRegion.getBottomLeft() == null) continue;

                Location subLocation = generateLocationInRegion(brokerRegion);
                SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
                chosenBroker.addChild(subscriber);
                subscribersCreated++;
                
                if (isDebug && i < DEBUG_SAMPLE_SIZE) {
                     logger.fine(String.format("  DEBUG: Placed %s at %s in Region %s (Pop: %d)",
                                      subscriber.getName(), subLocation.toShortString(), chosenBroker.getName(), chosenBroker.getInternetPopulation()));
                }
            }
        }
        
        if (isDebug && totalSubscribersToCreate > DEBUG_SAMPLE_SIZE) {
             logger.fine(String.format("  DEBUG: ... (logged first %d of %d subscribers)", DEBUG_SAMPLE_SIZE, totalSubscribersToCreate));
             logger.fine("  --------------------------------------------");
        }
        
        System.out.println("--- Proportional Subscriber Placement Complete. Total subscribers created: " + subscribersCreated + " ---");
    }

    private BrokerWithRegion findBrokerForWeight(long weight, List<BrokerWithRegion> brokers, long[] cumulativeWeights) {
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

    private void generateAndAttachUniformly(List<BrokerWithRegion> leafBrokers, long totalSubscribersToCreate) {
        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            BrokerWithRegion chosenBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));

            Region brokerRegion = chosenBroker.getRegion();
            if (brokerRegion == null || brokerRegion.getBottomLeft() == null) continue;

            Location subLocation = generateLocationInRegion(brokerRegion);
            SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
            chosenBroker.addChild(subscriber);
            subscribersCreated++;
        }
         System.out.println("--- Uniform Subscriber Placement Complete. Total subscribers created: " + subscribersCreated + " ---");
    }

    private Location generateLocationInRegion(Region region) {
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        double rangeX = tr.getX() - bl.getX();
        double rangeY = tr.getY() - bl.getY();
        double rangeZ = tr.getZ() - bl.getZ();
        double randomX = bl.getX() + random.nextDouble() * rangeX;
        double randomY = bl.getY() + random.nextDouble() * rangeY;
        double randomZ = bl.getZ() + random.nextDouble() * rangeZ;
        return new Location(randomX, randomY, randomZ);
    }

    private String generateSubscriberName() {
        return "Sub-" + subscriberIdCounter++;
    }
}