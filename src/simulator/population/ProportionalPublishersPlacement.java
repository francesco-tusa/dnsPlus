package simulator.population;

import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.CustomLogger;

public class ProportionalPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(ProportionalPublishersPlacement.class.getName());

    public ProportionalPublishersPlacement() {
        super();
    }

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate) {
        logger.info("");
        logger.info("--- Starting Proportional Publisher Placement ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            logger.severe("Error: The provided list of leaf brokers is empty. Cannot generate publishers.");
            return;
        }

        long worldTotalInternetPopulation = rootNode.getInternetPopulation();
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * worldTotalInternetPopulation);
            BoundedBroker chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null || brokerRegion.getBottomLeft() == null) continue;

                Location pubLocation = generateLocationInRegion(brokerRegion);
                
                PublisherWithLocation publisher = new PublisherWithLocation(pubLocation);
                
                chosenBroker.addChild(publisher);
                publishersCreated++;
            }
        }
        
        logger.info("--- Placement Complete. Total publishers created: " + publishersCreated + " ---");
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
}