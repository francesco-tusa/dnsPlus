package simulator.population;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import simulator.regions.BoundedBroker;
import utils.CustomLogger;

public class AmazonAwsPublishersPlacement extends AbstractBilevelPublishersPlacement {

    private static final Logger logger = CustomLogger.getLogger(AmazonAwsPublishersPlacement.class.getName());

    public AmazonAwsPublishersPlacement() {}
    
    @Override
    protected List<BoundedBroker> createPlacementPool(BoundedBroker rootNode) {
        logger.info("Identifying AWS Regions in the topology...");
        
        List<BoundedBroker> awsBrokers = AwsRegions.findAwsBrokers(rootNode);
        
        if (awsBrokers.isEmpty()) {
            logger.severe("Error: No AWS regions found in the topology.");
            return null;
        }
        
        List<BoundedBroker> validBrokers = awsBrokers.stream()
            .filter(b -> b.getRegion() != null && b.getRegion().getBottomLeft() != null)
            .collect(Collectors.toList());

        if (validBrokers.size() < awsBrokers.size()) {
            logger.warning(String.format("Filtered out %d AWS regions due to missing bounds/geometry.", 
                awsBrokers.size() - validBrokers.size()));
        }

        logger.info("Found " + validBrokers.size() + " valid AWS regions for Publisher Placement.");
        return validBrokers;
    }
}