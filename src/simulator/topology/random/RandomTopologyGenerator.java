package simulator.topology.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;

public class RandomTopologyGenerator extends AbstractTopologyFactory<RegionRandomTopologyConfiguration, BrokerWithRegion> {

    private final BrokerFactory brokerFactory;
    private List<Region> leafRegionsDefinition;
    private final List<BrokerWithRegion> allLeafBrokers = new ArrayList<>();
    private final Random random = new Random();

    private static final int MAX_Z = 100; // Z-axis can remain constant
    private static final long MOCK_POPULATION = 10000;

    public RandomTopologyGenerator(BrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        if (!(genericConfig instanceof RegionRandomTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of RegionRandomTopologyConfiguration.");
        }
        this.config = (RegionRandomTopologyConfiguration) genericConfig;
        this.allLeafBrokers.clear();
        
        // --- UPDATED ---
        // Use world dimensions from the config object
        this.leafRegionsDefinition = generateRegionDefinitions(
            config.getNumRegions(), 
            config.getWorldWidth(), 
            config.getWorldHeight()
        );
    }

    @Override
    protected BrokerWithRegion buildCoreTopology() {
        BrokerWithRegion root = brokerFactory.createBroker(generateBrokerName());
        buildBrokerLevelRecursive(root, 0, config.getTreeDepth() - 1, config.getMaxBranchingFactor());
        aggregateData(root);
        return root;
    }

    private void buildBrokerLevelRecursive(BrokerWithRegion parent, int currentDepth, int leafDepth, int maxBranchingFactor) {
        int numChildren = (maxBranchingFactor <= 1) ? 1 : (1 + random.nextInt(maxBranchingFactor));

        if (currentDepth == leafDepth) {
            for (int i = 0; i < numChildren; i++) {
                // Use modulo to wrap around region definitions if there are more brokers than definitions
                Region regionDef = leafRegionsDefinition.get(allLeafBrokers.size() % config.getNumRegions());
                BrokerWithRegion leafBroker = brokerFactory.createLeafBroker(generateLeafBrokerName(), regionDef.getBottomLeft(), regionDef.getTopRight());
                leafBroker.setInternetPopulation(MOCK_POPULATION);
                parent.addChild(leafBroker);
                allLeafBrokers.add(leafBroker);
            }
            return;
        }

        for (int i = 0; i < numChildren; i++) {
            BrokerWithRegion childBroker = brokerFactory.createBroker(generateBrokerName());
            parent.addChild(childBroker);
            buildBrokerLevelRecursive(childBroker, currentDepth + 1, leafDepth, maxBranchingFactor);
        }
    }

    private void aggregateData(BrokerWithRegion node) {
        if (node.getChildren().stream().noneMatch(c -> c instanceof BrokerWithRegion)) {
            // This is a leaf node in the broker hierarchy
            return;
        }
        long aggregatedPopulation = 0;
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof BrokerWithRegion childBroker) {
                aggregateData(childBroker); // Recurse first
                aggregatedPopulation += childBroker.getInternetPopulation();
            }
        }
        node.setInternetPopulation(aggregatedPopulation);
    }
    
    @Override
    public void attachSubscribers(BrokerWithRegion root) {
        // This method is now handled by ProportionalSubscribersPlacement,
        // but this logic remains as a fallback if config.getSubscribersPerLeafNode() > 0
        for (int i = 0; i < allLeafBrokers.size(); i++) {
            BrokerWithRegion leafBroker = allLeafBrokers.get(i);
            Region regionDef = leafRegionsDefinition.get(i % config.getNumRegions());
            for (int j = 0; j < config.getSubscribersPerLeafNode(); j++) {
                Location subLocation = generateLocationInRegion(regionDef);
                SubscriberWithLocation subscriber = new SubscriberWithLocation(generateSubscriberName(), subLocation);
                leafBroker.addChild(subscriber);
            }
        }
    }

    @Override
    public void attachPublishers(BrokerWithRegion root) {
        // This method is now handled by DataCenterPublishersPlacement,
        // but this logic remains as a fallback if config.getPublishersPerLeafNode() > 0
        for (int i = 0; i < allLeafBrokers.size(); i++) {
            BrokerWithRegion leafBroker = allLeafBrokers.get(i);
            Region regionDef = leafRegionsDefinition.get(i % config.getNumRegions());
            for (int j = 0; j < config.getPublishersPerLeafNode(); j++) {
                Location pubLocation = generateLocationInRegion(regionDef);
                PublisherWithLocation publisher = new PublisherWithLocation(generatePublisherName(), pubLocation);
                leafBroker.addChild(publisher);
            }
        }
    }
    
    /**
     * Updated to accept world dimensions.
     */
    private List<Region> generateRegionDefinitions(int count, double worldWidth, double worldHeight) {
        List<Region> regions = new ArrayList<>();
        if (count <= 0) return regions;
        int gridCols = (int) Math.ceil(Math.sqrt(count));
        int gridRows = (int) Math.ceil((double) count / gridCols);
        
        // --- UPDATED ---
        // Use parameters instead of MAX_X, MAX_Y
        double regionWidth = Math.max(1.0, worldWidth / gridCols);
        double regionHeight = Math.max(1.0, worldHeight / gridRows);

        int regionsCreated = 0;
        for (int row = 0; row < gridRows && regionsCreated < count; row++) {
            for (int col = 0; col < gridCols && regionsCreated < count; col++) {
                double x1 = col * regionWidth;
                double y1 = row * regionHeight;
                // Use (width - 1) for coordinate span calculation if needed, but direct addition is simpler
                // e.g., Region 0: [0, 332], Region 1: [333, 665], Region 2: [666, 999]
                double x2 = (col == gridCols - 1) ? worldWidth : (x1 + regionWidth);
                double y2 = (row == gridRows - 1) ? worldHeight : (y1 + regionHeight);
                
                // Ensure floating point precision doesn't slightly exceed world bounds
                x2 = Math.min(x2, worldWidth);
                y2 = Math.min(y2, worldHeight);

                regions.add(new Region(new Location(x1, y1, 0), new Location(x2, y2, MAX_Z)));
                regionsCreated++;
            }
        }
        return regions;
    }

    private Location generateLocationInRegion(Region region) {
        Objects.requireNonNull(region, "Region cannot be null");
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        Objects.requireNonNull(bl, "Region's bottom-left corner cannot be null");
        Objects.requireNonNull(tr, "Region's top-right corner cannot be null");

        double rangeX = tr.getX() - bl.getX();
        double rangeY = tr.getY() - bl.getY();
        double rangeZ = tr.getZ() - bl.getZ();
        
        double randomX = bl.getX() + (rangeX > 0 ? random.nextDouble() * rangeX : 0);
        double randomY = bl.getY() + (rangeY > 0 ? random.nextDouble() * rangeY : 0);
        double randomZ = bl.getZ() + (rangeZ > 0 ? random.nextDouble() * rangeZ : 0);
        
        return new Location(randomX, randomY, randomZ);
    }
}