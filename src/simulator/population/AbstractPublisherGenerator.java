package simulator.population;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;

/**
 * An abstract base class for different publisher generation strategies.
 * It provides common utility methods for creating and placing publishers.
 */
public abstract class AbstractPublisherGenerator {

    protected final Random random = new Random();
    
    // Removed: publisherIdCounter (Now handled inside PublisherWithLocation)

    /**
     * Abstract method that must be implemented by concrete generator classes.
     */
    public abstract void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate);

    /**
     * Generates a random location within the given region's bounding box.
     */
    protected Location generateLocationInRegion(Region region) {
        Objects.requireNonNull(region, "Region cannot be null");
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        Objects.requireNonNull(bl, "Region's bottom-left corner cannot be null");
        Objects.requireNonNull(tr, "Region's top-right corner cannot be null");

        double minX = bl.getX();
        double rangeX = tr.getX() - minX;
        double randomX = (rangeX > 0) ? minX + (random.nextDouble() * rangeX) : minX;

        double minY = bl.getY();
        double rangeY = tr.getY() - minY;
        double randomY = (rangeY > 0) ? minY + (random.nextDouble() * rangeY) : minY;
        
        double randomZ = bl.getZ();

        return new Location(randomX, randomY, randomZ);
    }
}