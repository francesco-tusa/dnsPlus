package simulator.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;

/**
 * Generates a tree-based topology specifically for simulations using
 * Brokers with Regions that process Region-based Subscriptions.
 * Extends AbstractTopologyFactory to structure the generation process.
 * Regions are defined bottom-up based on subscriber locations.
 * Leaf brokers are assigned to region definitions based on their index.
 */
public class RegionProcessingTopologyGenerator extends AbstractTopologyFactory<RegionTopologyConfiguration, BrokerWithRegionProcessingRegion> {

    // State specific to this generator
    private List<Region> leafRegionsDefinition; // Defines *where* subs/pubs are placed
    private List<LeafBrokerWithRegionProcessingRegion> allLeafBrokers;

    // Define the overall simulation space using integer coordinates (used for region partitioning)
    private static final int MAX_X = 1000;
    private static final int MAX_Y = 1000;
    private static final int MAX_Z = 100;

    @Override
    protected void initialize(TopologyConfiguration genericConfig) {
        System.out.println("Initializing RegionProcessingTopologyGenerator...");
        if (!(genericConfig instanceof RegionTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of RegionTopologyConfiguration for this generator.");
        }
        this.config = (RegionTopologyConfiguration) genericConfig;

        brokerIdCounter = 0;
        leafBrokerIdCounter = 0;
        subscriberIdCounter = 0;
        publisherIdCounter = 0;

        this.leafRegionsDefinition = null;
        this.allLeafBrokers = new ArrayList<>();

        // Generate the *definitions* of regions where leaf brokers will operate
        this.leafRegionsDefinition = generateRegionDefinitions(config.getNumRegions());
        if (this.leafRegionsDefinition == null || this.leafRegionsDefinition.isEmpty()) {
             throw new IllegalStateException("Failed to generate region definitions during initialization.");
        }
        System.out.println("Generated " + this.leafRegionsDefinition.size() + " region definitions.");
        System.out.println("Initialization complete.");
    }

    @Override
    protected BrokerWithRegionProcessingRegion buildCoreTopology() {
        System.out.println("Building core broker topology...");
        Objects.requireNonNull(config, "Configuration must be initialized before building topology.");

        BrokerWithRegionProcessingRegion root = new BrokerWithRegionProcessingRegion(generateBrokerName());
        System.out.println("Created Root Broker: " + root.getName() + " (Initial Region: " + root.getRegion() + ")"); // Region starts empty

        buildBrokerLevelRecursive(root, 0, config.getTreeDepth() - 1, config.getMaxBranchingFactor());

        System.out.println("Core broker topology built. Total leaf brokers generated: " + allLeafBrokers.size());
        return root;
    }

    /**
     * Recursive helper method to build broker levels.
     * Populates the 'allLeafBrokers' list.
     */
    private void buildBrokerLevelRecursive(BrokerWithRegionProcessingRegion parent, int currentDepth, int leafDepth, int maxBranchingFactor) {
        int numChildren = (maxBranchingFactor <= 1) ? 1 : (1 + random.nextInt(maxBranchingFactor));

        if (currentDepth == leafDepth) {
            for (int i = 0; i < numChildren; i++) {
                LeafBrokerWithRegionProcessingRegion leafBroker = new LeafBrokerWithRegionProcessingRegion(generateLeafBrokerName());
                parent.addChild(leafBroker);
                allLeafBrokers.add(leafBroker);
            }
            return;
        }

        for (int i = 0; i < numChildren; i++) {
            BrokerWithRegionProcessingRegion childBroker = new BrokerWithRegionProcessingRegion(generateBrokerName());
            parent.addChild(childBroker);
            buildBrokerLevelRecursive(childBroker, currentDepth + 1, leafDepth, maxBranchingFactor);
        }
    }

    @Override
    protected void attachSubscribers(BrokerWithRegionProcessingRegion root) {
        System.out.println("Attaching subscribers to leaf brokers...");
        Objects.requireNonNull(config, "Configuration must be initialized.");
        Objects.requireNonNull(leafRegionsDefinition, "Region definitions must be generated before attaching subscribers.");
        Objects.requireNonNull(allLeafBrokers, "Leaf brokers must be generated before attaching subscribers.");

        int numRegions = config.getNumRegions();
        int subscribersPerLeafBroker = config.getSubscribersPerLeafNode();

        if (allLeafBrokers.isEmpty()) {
             System.err.println("Warning: No leaf brokers were generated, cannot attach subscribers.");
             return;
        }

        // Iterate through all generated leaf brokers
        for (int i = 0; i < allLeafBrokers.size(); i++) {
            LeafBrokerWithRegionProcessingRegion leafBroker = allLeafBrokers.get(i);

            // Assign this leaf broker to a region definition using modulo
            int regionIndex = i % numRegions;
            Region regionDef = leafRegionsDefinition.get(regionIndex);

            System.out.println("  Processing Leaf Broker: " + leafBroker.getName() + " for region definition " + regionIndex);

            // Create and Connect Subscribers for this leaf broker within the assigned region definition area
            createAndConnectSubscribersForLeaf(leafBroker, subscribersPerLeafBroker, regionDef);
        }

        System.out.println("Subscriber attachment complete.");
        // Region propagation should have happened via addChild -> updateRegion calls.
    }

    /**
     * Helper method to create and connect subscribers for a single leaf broker.
     * Adding subscribers triggers region updates.
     * Subscribers are placed within the bounds of the 'placementRegion'.
     */
    private void createAndConnectSubscribersForLeaf(LeafBrokerWithRegionProcessingRegion leafBroker, int count, Region placementRegion) {
        Objects.requireNonNull(placementRegion, "Placement region cannot be null for subscriber generation");
        Objects.requireNonNull(placementRegion.getBottomLeft(), "Placement region must have a bottom-left point");
        Objects.requireNonNull(placementRegion.getTopRight(), "Placement region must have a top-right point");

        if (count == 0) return;

        System.out.println("    Creating " + count + " subscribers for " + leafBroker.getName() + " within area " + placementRegion);
        for (int i = 0; i < count; i++) {
            Location subLocation = generateLocationInRegion(placementRegion);
            SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
            // This addChild call triggers the region update mechanism in LeafBrokerWithRegionProcessingRegion
            leafBroker.addChild(subscriber);
            System.out.println("      Created " + subscriber.getName() + " at " + subLocation);
        }
        // The leaf broker's region is now defined/updated by the subscribers added.
        System.out.println("    Finished adding subscribers for " + leafBroker.getName() + ". Leaf region after updates: " + leafBroker.getRegion());
    }

    /**
     * Creates and attaches publisher nodes to the appropriate leaf nodes.
     * Publishers are placed within the defined region area associated with the leaf broker.
     */
    @Override
    protected void attachPublishers(BrokerWithRegionProcessingRegion root) {
        System.out.println("Attaching publishers to leaf brokers...");
        Objects.requireNonNull(config, "Configuration must be initialized.");
        Objects.requireNonNull(leafRegionsDefinition, "Region definitions must be generated before attaching publishers.");
        Objects.requireNonNull(allLeafBrokers, "Leaf brokers must be generated before attaching publishers.");

        int numRegions = config.getNumRegions();
        int publishersPerLeafBroker = config.getPublishersPerLeafNode();

        if (publishersPerLeafBroker <= 0) {
            System.out.println("No publishers per leaf configured, skipping publisher attachment.");
            return;
        }

        if (allLeafBrokers.isEmpty()) {
             System.err.println("Warning: No leaf brokers were generated, cannot attach publishers.");
             return;
        }

        // Iterate through all generated leaf brokers
        for (int i = 0; i < allLeafBrokers.size(); i++) {
            LeafBrokerWithRegionProcessingRegion leafBroker = allLeafBrokers.get(i);

            // Assign this leaf broker to a region definition using modulo
            int regionIndex = i % numRegions;
            Region regionDef = leafRegionsDefinition.get(regionIndex);

            System.out.println("  Processing Leaf Broker: " + leafBroker.getName() + " for region definition " + regionIndex + " for publishers.");

            // Create and Connect Publishers for this leaf broker within the assigned region definition area
            createAndConnectPublishersForLeaf(leafBroker, publishersPerLeafBroker, regionDef);
        }

        System.out.println("Publisher attachment complete.");
    }

    /**
     * Helper method to create and connect publishers for a single leaf broker.
     * Publishers are placed within the bounds of the 'placementRegion'.
     */
    private void createAndConnectPublishersForLeaf(LeafBrokerWithRegionProcessingRegion leafBroker, int count, Region placementRegion) {
        Objects.requireNonNull(placementRegion, "Placement region cannot be null for publisher generation");
        Objects.requireNonNull(placementRegion.getBottomLeft(), "Placement region must have a bottom-left point");
        Objects.requireNonNull(placementRegion.getTopRight(), "Placement region must have a top-right point");

        if (count == 0) return;

        System.out.println("    Creating " + count + " publishers for " + leafBroker.getName() + " within area " + placementRegion);
        for (int i = 0; i < count; i++) {
            Location pubLocation = generateLocationInRegion(placementRegion);
            PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
            // Attach publisher - assumes addChild handles this without calling updateRegion
            leafBroker.addChild(publisher);
            System.out.println("      Created " + publisher.getName() + " at " + pubLocation);
        }
        System.out.println("    Finished adding publishers for " + leafBroker.getName());
    }


    // --- Helper methods for region/location generation ---

    private List<Region> generateRegionDefinitions(int count) {
        List<Region> regions = new ArrayList<>();
        if (count <= 0) return regions;
        int gridCols = (int) Math.ceil(Math.sqrt(count));
        int gridRows = (int) Math.ceil((double) count / gridCols);
        int regionWidth = Math.max(1, MAX_X / gridCols);
        int regionHeight = Math.max(1, MAX_Y / gridRows);
        int regionDepth = MAX_Z;

        System.out.println("Generating region definitions in a " + gridRows + "x" + gridCols + " grid (approx).");

        int regionsCreated = 0;
        for (int row = 0; row < gridRows && regionsCreated < count; row++) {
            for (int col = 0; col < gridCols && regionsCreated < count; col++) {
                int x1 = col * regionWidth;
                int y1 = row * regionHeight;
                int z1 = 0;
                int x2 = Math.min(MAX_X, x1 + regionWidth -1);
                int y2 = Math.min(MAX_Y, y1 + regionHeight -1);
                int z2 = Math.min(MAX_Z, z1 + regionDepth -1);
                x1 = Math.min(x1, x2);
                y1 = Math.min(y1, y2);
                z1 = Math.min(z1, z2);
                regions.add(new Region(new Location(x1, y1, z1), new Location(x2, y2, z2)));
                regionsCreated++;
            }
        }
        return regions;
    }

    private Location generateLocationInRegion(Region region) {
        Objects.requireNonNull(region, "Region cannot be null for location generation");
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        Objects.requireNonNull(bl, "Region point bottomLeft cannot be null");
        Objects.requireNonNull(tr, "Region point topRight cannot be null");

        int minX = Math.min(bl.getX(), tr.getX());
        int minY = Math.min(bl.getY(), tr.getY());
        int minZ = Math.min(bl.getZ(), tr.getZ());
        int maxX = Math.max(bl.getX(), tr.getX());
        int maxY = Math.max(bl.getY(), tr.getY());
        int maxZ = Math.max(bl.getZ(), tr.getZ());

        int rangeX = maxX - minX + 1;
        int rangeY = maxY - minY + 1;
        int rangeZ = maxZ - minZ + 1;

        if (rangeX <= 0 || rangeY <= 0 || rangeZ <= 0) {
             System.err.println("Warning: Generating location in degenerate region definition: " + region);
             return new Location(minX, minY, minZ);
        }

        int randomX = minX + random.nextInt(rangeX);
        int randomY = minY + random.nextInt(rangeY);
        int randomZ = minZ + random.nextInt(rangeZ);

        return new Location(randomX, randomY, randomZ);
    }
}
