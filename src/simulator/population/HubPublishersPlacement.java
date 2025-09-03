package simulator.population;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;

/**
 * A strategy for placing publishers in a few, high-density "hub" regions
 * to simulate global services.
 */
public class HubPublishersPlacement implements PublishersPlacementStrategy {

    private final Random random = new Random();
    private int publisherIdCounter = 0;

    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        System.out.println("\n--- Starting Hub-Based Publisher Placement (for Global Services) ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: The provided list of leaf brokers is empty. Cannot generate publishers.");
            return;
        }
        
        // This strategy requires finding the "hubs" first. We define hubs as the leaf
        // brokers with the highest internet population.
        int numberOfHubs = (int) Math.min(totalPublishersToCreate, Math.max(1, leafBrokers.size() / 10)); // e.g., top 10%
        
        List<BrokerWithRegion> hubBrokers = leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(numberOfHubs)
            .collect(Collectors.toList());

        System.out.println("Distributing " + totalPublishersToCreate + " publishers among " + hubBrokers.size() + " hub regions...");

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
        System.out.println("--- Hub-Based Publisher Placement Complete. Total publishers created: " + publishersCreated + " ---");
    }

    protected Location generateLocationInRegion(Region region) {
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        double rangeX = tr.getX() - bl.getX() + 1;
        double rangeY = tr.getY() - bl.getY() + 1;
        double rangeZ = tr.getZ() - bl.getZ() + 1;
        double randomX = bl.getX() + random.nextDouble() * rangeX;
        double randomY = bl.getY() + random.nextDouble() * rangeY;
        double randomZ = bl.getZ() + random.nextDouble() * rangeZ;
        return new Location(randomX, randomY, randomZ);
    }

    protected String generatePublisherName() {
        return "Pub-" + publisherIdCounter++;
    }
}