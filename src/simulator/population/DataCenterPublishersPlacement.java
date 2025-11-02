package simulator.population;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import utils.CustomLogger;

/**
 * Places publishers (replicas) in regions that act as proxies for major data centers.
 * It identifies the top N regions by Internet population and treats them as
 * data center locations, similar to how major cloud providers (e.g., AWS)
 * establish regions near major population centers.
 */
public class DataCenterPublishersPlacement extends AbstractPublisherGenerator implements PublishersPlacementStrategy {

    private static final Logger logger = CustomLogger.getLogger(DataCenterPublishersPlacement.class.getName());

    private int maxDataCenters;

    public DataCenterPublishersPlacement(int maxDataCenters) {
        if (maxDataCenters <= 0) {
            throw new IllegalArgumentException("maxDataCenters must be positive.");
        }
        this.maxDataCenters = maxDataCenters;
    }

    @Override
    public void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalPublishersToCreate) {
        logger.info("\n--- Starting Data Center Publisher Placement ---");

        if (leafBrokers == null || leafBrokers.isEmpty()) {
            logger.severe("Error: No leaf brokers found.");
            return;
        }
        
        // 1. Identify potential Data Center regions (Top N by internet population)
        // We cap this at 'maxDataCenters' to mimic a realistic, finite set of cloud regions.
        int numPotentialDCs = Math.min(leafBrokers.size(), maxDataCenters);
        
        List<BrokerWithRegion> dataCenterBrokers = leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(numPotentialDCs)
            .collect(Collectors.toList());

        if (dataCenterBrokers.isEmpty()) {
            logger.severe("Error: No data center brokers found (list of leaves was empty).");
            return;
        }

        logger.info("Identified top " + dataCenterBrokers.size() + " regions as Data Center locations based on internet population.");
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("  --- DEBUG: Top Data Center Regions ---");
            for (int i = 0; i < dataCenterBrokers.size(); i++) {
                BrokerWithRegion dc = dataCenterBrokers.get(i);
                logger.fine(String.format("  [%d] %s (Pop: %d, Region: %s)", 
                                  i + 1, dc.getName(), dc.getInternetPopulation(), 
                                  dc.getRegion() != null ? dc.getRegion().toShortString() : "N/A"));
            }
            logger.fine("  --------------------------------------");
        }

        logger.info("Distributing " + totalPublishersToCreate + " replicas among these Data Centers...");

        long publishersCreated = 0;
        for (long i = 0; i < totalPublishersToCreate; i++) {
            // Randomly select one of the Data Center regions for this replica
            BrokerWithRegion chosenDC = dataCenterBrokers.get(random.nextInt(dataCenterBrokers.size()));
            Region dcRegion = chosenDC.getRegion();
            
            if (dcRegion == null) {
                logger.warning("Skipping publisher placement: Chosen Data Center broker " + chosenDC.getName() + " has a null region.");
                continue;
            }

            Location pubLocation = generateLocationInRegion(dcRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            chosenDC.addChild(publisher);
            publishersCreated++;
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("  DEBUG: Placed %s at %s in Data Center %s (Region: %s)",
                                  publisher.getName(), pubLocation.toShortString(), chosenDC.getName(), dcRegion.toShortString()));
            }
        }
        logger.info("--- Data Center Placement Complete. Total replicas placed: " + publishersCreated + " ---");
    }
}