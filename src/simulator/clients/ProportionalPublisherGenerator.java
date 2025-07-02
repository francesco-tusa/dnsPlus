package simulator.clients;

import java.util.List;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;

/**
 * Generates publishers for local/niche services.
 *
 * This strategy distributes publishers across all leaf brokers proportionally
 * to the internet population of each broker's region. It's suitable for modeling
 * services that are likely to originate where users are.
 */
public class ProportionalPublisherGenerator extends AbstractPublisherGenerator {

    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalPublishersToCreate) {
        System.out.println("\n--- Starting Proportional Publisher Generation (for Local Services) ---");
        System.out.println("Distributing " + totalPublishersToCreate + " total publishers...");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: The provided list of leaf brokers is empty. Cannot generate publishers.");
            return;
        }

        long totalInternetPopulation = rootNode.getInternetPopulation();

        if (totalInternetPopulation <= 0) {
            System.err.println("Warning: Total internet population is " + totalInternetPopulation + ". Falling back to uniform random distribution.");
            generateAndAttachUniformly(leafBrokers, totalPublishersToCreate);
            return;
        }
        
        long[] cumulativeWeights = new long[leafBrokers.size()];
        long runningTotal = 0;
        for (int i = 0; i < leafBrokers.size(); i++) {
            runningTotal += leafBrokers.get(i).getInternetPopulation();
            cumulativeWeights[i] = runningTotal;
        }

        System.out.println("Created cumulative distribution for " + leafBrokers.size() + " leaf brokers for publisher placement.");

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            long randomWeight = (long) (random.nextDouble() * totalInternetPopulation);
            LeafBrokerWithRegionProcessingRegion chosenBroker = findBrokerForWeight(randomWeight, leafBrokers, cumulativeWeights);

            if (chosenBroker != null) {
                Region brokerRegion = chosenBroker.getRegion();
                if (brokerRegion == null) continue;

                Location pubLocation = generateLocationInRegion(brokerRegion);
                PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
                chosenBroker.addChild(publisher);
                publishersCreated++;
            }
        }
        System.out.println("--- Proportional Publisher Generation Complete. Total publishers created: " + publishersCreated + " ---");
    }

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

    private void generateAndAttachUniformly(List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalPublishersToCreate) {
        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            LeafBrokerWithRegionProcessingRegion chosenBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));
            Region brokerRegion = chosenBroker.getRegion();
            if (brokerRegion == null) continue;
            Location pubLocation = generateLocationInRegion(brokerRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenBroker.addChild(publisher);
            publishersCreated++;
        }
         System.out.println("--- Uniform Publisher Generation Complete. Total publishers created: " + publishersCreated + " ---");
    }
}