package simulator.population;

import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BoundedBroker;
import utils.CustomLogger;

public class PopulationBasedPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(PopulationBasedPublishersPlacement.class.getName());

    public PopulationBasedPublishersPlacement() {
        super();
    }

    protected PublisherWithLocation createPublisher(Location loc) {
        return new PublisherWithLocation(loc); // Default behavior
    }

    @Override
    public void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate) {
        logger.info("--- Starting Population-Based Publisher Placement ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) return;

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

            if (chosenBroker != null && chosenBroker.getRegion() != null) {
                Location pubLocation = generateLocationInRegion(chosenBroker.getRegion());
                
                PublisherWithLocation publisher = this.createPublisher(pubLocation);
                
                chosenBroker.addChild(publisher);
                publishersCreated++;
            }
        }
        logger.info("--- Publisher Placement Complete: " + publishersCreated + " created. ---");
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