package simulator.population;

import java.util.List;
import java.util.logging.Logger; // Import Logger
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger; // Import CustomLogger

/**
 * Generates publishers for local/niche services proportionally to the internet population.
 */
public class ProportionalPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(ProportionalPublishersPlacement.class.getName()); // Get logger

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate) {
        logger.info("\n--- Starting Proportional Publisher Generation (for Local Services) ---");
        logger.info("Distributing " + totalPublishersToCreate + " total publishers...");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            logger.severe("Error: The provided list of leaf brokers is empty. Cannot generate publishers.");
            return;
        }

        long totalInternetPopulation = rootNode.getInternetPopulation();

        if (totalInternetPopulation <= 0) {
            logger.warning("Warning: Total internet population is " + totalInternetPopulation + ". Falling back to uniform random distribution.");
            generateAndAttachUniformly(leafBrokers, totalPublishersToCreate);
            return;
        }
        
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        logger.info("Created cumulative distribution for " + leafBrokers.size() + " leaf brokers for publisher placement.");

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * totalInternetPopulation);
            BoundedBroker chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null) continue;

                Location pubLocation = generateLocationInRegion(brokerRegion);
                PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
                chosenBroker.addChild(publisher);
                publishersCreated++;
            }
        }
        logger.info("--- Proportional Publisher Generation Complete. Total publishers created: " + publishersCreated + " ---");
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

    private void generateAndAttachUniformly(List<BoundedBroker> leafBrokers, long totalPublishersToCreate) {
        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            BoundedBroker chosenBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));
            Region brokerRegion = chosenBroker.getRegion();
            if (brokerRegion == null) continue;
            Location pubLocation = generateLocationInRegion(brokerRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenBroker.addChild(publisher);
            publishersCreated++;
        }
         logger.info("--- Uniform Publisher Generation Complete. Total publishers created: " + publishersCreated + " ---");
    }
}