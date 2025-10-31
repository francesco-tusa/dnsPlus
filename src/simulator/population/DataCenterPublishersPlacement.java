package simulator.population;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;

/**
 * Places publishers (replicas) in regions that act as proxies for major data centers.
 * It identifies the top N regions by Internet population and treats them as
 * data center locations, similar to how major cloud providers (e.g., AWS)
 * establish regions near major population centers.
 */
public class DataCenterPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    // Default to ~30 major global regions, similar to major cloud providers
    private int maxDataCenters = 30;

    public DataCenterPublishersPlacement() {
        // Use default
    }

    public DataCenterPublishersPlacement(int maxDataCenters) {
        this.maxDataCenters = maxDataCenters;
    }

    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        System.out.println("\n--- Starting Data Center Publisher Placement ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found.");
            return;
        }
        
        // 1. Identify potential Data Center regions (Top N by internet population)
        // We cap this at 'maxDataCenters' to mimic a realistic, finite set of cloud regions.
        int numPotentialDCs = Math.min(leafBrokers.size(), maxDataCenters);
        
        List<BrokerWithRegion> dataCenterBrokers = leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(numPotentialDCs)
            .collect(Collectors.toList());

        System.out.println("Identified top " + dataCenterBrokers.size() + " regions as Data Center locations based on internet population.");
        System.out.println("Distributing " + totalPublishersToCreate + " replicas among these Data Centers...");

        // 2. Distribute replicas among these Data Centers
        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            // Randomly select one of the Data Center regions for this replica
            // (Allows multiple replicas per DC if totalPublishers > maxDataCenters)
            BrokerWithRegion chosenDC = dataCenterBrokers.get(random.nextInt(dataCenterBrokers.size()));
            Region dcRegion = chosenDC.getRegion();
            
            if (dcRegion == null) continue;

            Location pubLocation = generateLocationInRegion(dcRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenDC.addChild(publisher);
            publishersCreated++;
        }
        System.out.println("--- Data Center Placement Complete. Total replicas placed: " + publishersCreated + " ---");
    }
}