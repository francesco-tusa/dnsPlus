package simulator.population;

import java.util.List;
import java.util.Random;
import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import utils.SimulationRandom;

/**
 * An abstract base class for different publisher generation strategies.
 * It provides common utility methods for creating and placing publishers.
 */
public abstract class AbstractPublisherGenerator {

    protected final Random random = SimulationRandom.get();
    
    // Removed: publisherIdCounter (Now handled inside PublisherWithLocation)

    /**
     * Abstract method that must be implemented by concrete generator classes.
     */
    public abstract void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate);

    /**
     * Generates a random location within the given region's bounding box.
     */
    protected Location generateLocationInRegion(Region region) {
        return region.getRandomLocation(this.random);
    }
}