package simulator.population;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;

/**
 * A strategy for placing subscribers proportionally to the internet population of leaf brokers.
 */
public class ProportionalSubscribersPlacement implements SubscribersPlacementStrategy {

    private final Random random = new Random();
    private int subscriberIdCounter = 0;

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

        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * worldTotalInternetPopulation);
            BrokerWithRegion chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null) continue;

                Location subLocation = generateLocationInRegion(brokerRegion);
                SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
                chosenBroker.addChild(subscriber);
                subscribersCreated++;
            }
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
            if (brokerRegion == null) continue;

            Location subLocation = generateLocationInRegion(brokerRegion);
            SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
            chosenBroker.addChild(subscriber);
            subscribersCreated++;
        }
         System.out.println("--- Uniform Subscriber Placement Complete. Total subscribers created: " + subscribersCreated + " ---");
    }

    private Location generateLocationInRegion(Region region) {
        Objects.requireNonNull(region, "Region cannot be null");
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        Objects.requireNonNull(bl, "Region's bottom-left corner cannot be null");
        Objects.requireNonNull(tr, "Region's top-right corner cannot be null");

        double minX = bl.getX();
        double rangeX = tr.getX() - minX;
        double randomX = (rangeX > 0) ? minX + (random.nextDouble() * rangeX) : minX;

        double minY = bl.getY();
        double rangeY = tr.getY() - minY;
        double randomY = (rangeY > 0) ? minY + (random.nextDouble() * rangeY) : minY;
        
        double randomZ = bl.getZ();

        return new Location(randomX, randomY, randomZ);
    }

    private String generateSubscriberName() {
        return "Sub-" + subscriberIdCounter++;
    }
}