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

    private static final int MAX_X = 1000;
    private static final int MAX_Y = 1000;
    private static final int MAX_Z = 100;
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
        this.leafRegionsDefinition = generateRegionDefinitions(config.getNumRegions());
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
            return;
        }
        long aggregatedPopulation = 0;
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof BrokerWithRegion childBroker) {
                aggregateData(childBroker);
                aggregatedPopulation += childBroker.getInternetPopulation();
            }
        }
        node.setInternetPopulation(aggregatedPopulation);
    }
    
    @Override
    public void attachSubscribers(BrokerWithRegion root) {
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
    
    private List<Region> generateRegionDefinitions(int count) {
        List<Region> regions = new ArrayList<>();
        if (count <= 0) return regions;
        int gridCols = (int) Math.ceil(Math.sqrt(count));
        int gridRows = (int) Math.ceil((double) count / gridCols);
        int regionWidth = Math.max(1, MAX_X / gridCols);
        int regionHeight = Math.max(1, MAX_Y / gridRows);

        int regionsCreated = 0;
        for (int row = 0; row < gridRows && regionsCreated < count; row++) {
            for (int col = 0; col < gridCols && regionsCreated < count; col++) {
                int x1 = col * regionWidth;
                int y1 = row * regionHeight;
                regions.add(new Region(new Location(x1, y1, 0), new Location(x1 + regionWidth - 1, y1 + regionHeight - 1, MAX_Z)));
                regionsCreated++;
            }
        }
        return regions;
    }

    private Location generateLocationInRegion(Region region) {
        Location bl = region.getBottomLeft();
        Location tr = region.getTopRight();
        double rangeX = tr.getX() - bl.getX();
        double rangeY = tr.getY() - bl.getY();
        double rangeZ = tr.getZ() - bl.getZ();
        double randomX = bl.getX() + random.nextDouble() * rangeX;
        double randomY = bl.getY() + random.nextDouble() * rangeY;
        double randomZ = bl.getZ() + random.nextDouble() * rangeZ;
        return new Location(randomX, randomY, randomZ);
    }
}