package simulator.clients;

import java.util.List;
import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;

/**
 * Generates publishers for global/hyperscale services.
 *
 * This strategy places all publishers exclusively within a small, predefined
 * list of "hub" regions, simulating the concentration of major services in
 * large data center hubs.
 */
public class HubBasedPublisherGenerator extends AbstractPublisherGenerator {

    /**
     * The `rootNode` and `leafBrokers` parameters are ignored in this implementation,
     * as placement is determined solely by the `hubBrokers` list.
     */
    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalPublishersToCreate) {
        throw new UnsupportedOperationException("This method is not supported. Use the version with hubBrokers parameter.");
    }

    /**
     * Generates and attaches publishers by placing them randomly among a
     * predefined list of hub brokers.
     *
     * @param hubBrokers A list of leaf brokers designated as data center hubs.
     * @param totalPublishersToCreate The total number of publishers to create and attach.
     */
    public void generateAndAttach(List<LeafBrokerWithRegionProcessingRegion> hubBrokers, long totalPublishersToCreate) {
        System.out.println("\n--- Starting Hub-Based Publisher Generation (for Global Services) ---");
        System.out.println("Distributing " + totalPublishersToCreate + " publishers among " + hubBrokers.size() + " hub regions...");

        if (hubBrokers == null || hubBrokers.isEmpty()) {
            System.err.println("Error: The provided list of hub brokers is empty. Cannot generate publishers.");
            return;
        }

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            // Pick one of the hub brokers uniformly at random
            LeafBrokerWithRegionProcessingRegion chosenHub = hubBrokers.get(random.nextInt(hubBrokers.size()));

            Region hubRegion = chosenHub.getRegion();
            if (hubRegion == null) continue;

            Location pubLocation = generateLocationInRegion(hubRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenHub.addChild(publisher);
            publishersCreated++;
        }
        System.out.println("--- Hub-Based Publisher Generation Complete. Total publishers created: " + publishersCreated + " ---");
    }
}