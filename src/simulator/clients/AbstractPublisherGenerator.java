package simulator.clients;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.Location;
import simulator.regions.BrokerWithRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;

/**
 * An abstract base class for different publisher generation strategies.
 * It provides common utility methods for creating and placing publishers.
 */
public abstract class AbstractPublisherGenerator {

    protected final Random random = new Random();
    protected int publisherIdCounter = 0;

    /**
     * Abstract method that must be implemented by concrete generator classes.
     * This method contains the specific logic for distributing publishers across a topology.
     *
     * @param rootNode The root of the broker topology.
     * @param leafBrokers A list of all leaf brokers in the topology.
     * @param totalPublishersToCreate The total number of publishers to create and attach.
     */
    public abstract void generateAndAttach(BrokerWithRegion rootNode, List<LeafBrokerWithRegionProcessingRegion> leafBrokers, long totalPublishersToCreate);

    /**
     * Generates a random location within the given region's bounding box.
     * @param region The region to generate a location in.
     * @return A new Location object with random coordinates.
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

    /**
     * Generates a unique name for a new publisher.
     * @return A unique publisher name string.
     */
    protected String generatePublisherName() {
        return "Pub-" + publisherIdCounter++;
    }
}