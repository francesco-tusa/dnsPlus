package simulator.clients;

import java.util.List;
import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;

/**
 * Generates publishers for global/hyperscale services.
 * This strategy places all publishers exclusively within a small, predefined list of "hub" regions.
 */
public class HubBasedPublisherGenerator extends AbstractPublisherGenerator {

    /**
     * This method is not supported for hub-based generation. 
     * Use the version that accepts a specific list of hub brokers.
     */
    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        throw new UnsupportedOperationException("This method is not supported for HubBasedPublisherGenerator. Use the version with a specific hubBrokers parameter.");
    }

    /**
     * Generates and attaches publishers by placing them randomly among a predefined list of hub brokers.
     *
     * @param hubBrokers A generic list of leaf brokers designated as data center hubs.
     * @param totalPublishersToCreate The total number of publishers to create and attach.
     */
    public void generateAndAttach(List<BrokerWithRegion> hubBrokers, long totalPublishersToCreate) {
        System.out.println("\n--- Starting Hub-Based Publisher Generation (for Global Services) ---");
        System.out.println("Distributing " + totalPublishersToCreate + " publishers among " + hubBrokers.size() + " hub regions...");

        if (hubBrokers == null || hubBrokers.isEmpty()) {
            System.err.println("Error: The provided list of hub brokers is empty. Cannot generate publishers.");
            return;
        }

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            // Pick one of the hub brokers uniformly at random
            BrokerWithRegion chosenHub = hubBrokers.get(random.nextInt(hubBrokers.size()));

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