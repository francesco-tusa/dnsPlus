package simulator.clients;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import simulator.Location;
import simulator.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;

/**
 * A utility class for generating subscribers and attaching them to a broker topology
 * using a weighted random distribution based on internet population.
 */
public class ProportionalSubscriberGenerator {

    private final Random random = new Random();
    private int subscriberIdCounter = 0;

    /**
     * Generates and attaches a total number of subscribers to a pre-built topology.
     * <p>
     * This method uses a weighted random sampling algorithm. It creates a cumulative
     * distribution of the internet population across all leaf brokers. For each
     * subscriber to be created, it picks a random number and uses the cumulative
     * distribution to select a broker, making brokers with higher populations
     * proportionally more likely to be chosen.
     *
     * @param rootNode The root of the broker topology, used to get total world population.
     * @param leafBrokers A list of all leaf brokers in the topology, for direct access.
     * @param totalSubscribersToCreate The total number of subscribers to distribute.
     */
    public void generateAndAttach(BrokerWithRegion rootNode, List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalSubscribersToCreate) {
        System.out.println("\n--- Starting Proportional Subscriber Generation (Weighted Random Sampling) ---");
        System.out.println("Distributing " + totalSubscribersToCreate + " total subscribers...");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: The provided list of leaf brokers is empty. Cannot generate subscribers.");
            return;
        }

        // The total population is now known from the root node.
        long totalInternetPopulation = rootNode.getInternetPopulation();

        if (totalInternetPopulation <= 0) {
            System.err.println("Warning: Total internet population is " + totalInternetPopulation + ". Falling back to uniform random distribution.");
            generateAndAttachUniformly(leafBrokers, totalSubscribersToCreate);
            return;
        }
        
        // Step 1: Create a cumulative weight array for weighted random sampling.
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        System.out.println("Created cumulative distribution for " + leafBrokers.size() + " leaf brokers.");

        // Step 2: Generate each subscriber and assign it to a broker.
        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            // Generate a random value within the total population range.
            long randomWeight = (long) (random.nextDouble() * totalInternetPopulation);

            // Find which broker this random value falls into.
            LeafBrokerWithRegionProcessingRegion chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null) continue;

                Location subLocation = generateLocationInRegion(brokerRegion);
                SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
                chosenBroker.addChild(subscriber);
                subscribersCreated++;
            }
        }
        System.out.println("--- Proportional Subscriber Generation Complete. Total subscribers created: " + subscribersCreated + " ---");
    }

    /**
     * Uses binary search to find the index of the broker corresponding to the random weight.
     * This is an efficient way to perform the weighted selection.
     */
    private LeafBrokerWithRegionProcessingRegion findBrokerForWeight(long weight, List<LeafBrokerWithRegionProcessingRegion> brokers, long[] cumulativeWeights) {
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

    /**
     * A fallback method for topologies that don't have population data. It distributes
     * subscribers uniformly at random across all available leaf brokers.
     */
    private void generateAndAttachUniformly(List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalSubscribersToCreate) {
        long subscribersCreated = 0;
        for (long i = 0; i < totalSubscribersToCreate; i++) {
            // Pick a leaf broker with equal probability
            LeafBrokerWithRegionProcessingRegion chosenBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));

            Region brokerRegion = chosenBroker.getRegion();
            if (brokerRegion == null) continue;

            Location subLocation = generateLocationInRegion(brokerRegion);
            SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
            chosenBroker.addChild(subscriber);
            subscribersCreated++;
        }
         System.out.println("--- Uniform Subscriber Generation Complete. Total subscribers created: " + subscribersCreated + " ---");
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