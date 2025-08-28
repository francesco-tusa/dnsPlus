package simulator.topology.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.Location;
import simulator.regions.BrokerWithRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;

/**
 * Generates a procedural, grid-based broker topology using a provided BrokerFactory
 * to allow for different broker implementations.
 */
public class GridTopologyGenerator extends AbstractTopologyFactory<GridTopologyConfiguration, BrokerWithRegion> {

    private final BrokerFactory brokerFactory;
    private List<LeafBrokerWithRegionProcessingRegion> leafBrokers;
    private final Random random = new Random();

    private static final double MIN_LON = -180.0;
    private static final double MAX_LON = 180.0;
    private static final double MIN_LAT = -90.0;
    private static final double MAX_LAT = 90.0;

    public GridTopologyGenerator(BrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        System.out.println("Initialising GridTopologyGenerator...");
        if (!(genericConfig instanceof GridTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of GridTopologyConfiguration.");
        }
        this.config = (GridTopologyConfiguration) genericConfig;
        this.leafBrokers = new ArrayList<>();
        this.brokerIdCounter = 0;
        this.leafBrokerIdCounter = 0;
        this.subscriberIdCounter = 0;
        this.publisherIdCounter = 0;
        this.rootNode = null;
    }

    @Override
    protected BrokerWithRegion buildCoreTopology() {
        System.out.println("Building procedural grid-based core broker topology...");
        Objects.requireNonNull(config, "Configuration must be initialised before building topology.");

        BrokerWithRegion root = brokerFactory.createBroker("Root");

        List<LeafBrokerWithRegionProcessingRegion> allLeaves = createAllLeafBrokers();
        this.leafBrokers.addAll(allLeaves);

        buildRecursive(root, allLeaves, 1, config.getTreeDepth());
        
        aggregateData(root);

        System.out.println("Core broker hierarchy with " + allLeaves.size() + " leaves created.");
        System.out.println("Root node total internet population: " + root.getInternetPopulation());

        return root;
    }

    private void buildRecursive(BrokerWithRegion parent, List<LeafBrokerWithRegionProcessingRegion> leavesToPlace, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth - 1) {
            for (LeafBrokerWithRegionProcessingRegion leaf : leavesToPlace) {
                parent.addChild(leaf);
            }
            return;
        }

        int remainingDepth = maxDepth - currentDepth;
        int numLeaves = leavesToPlace.size();
        
        int branchingFactor = (int) Math.ceil(Math.pow(numLeaves, 1.0 / remainingDepth));
        int leavesPerChild = (int) Math.ceil((double) numLeaves / branchingFactor);
        
        for (int i = 0; i < branchingFactor; i++) {
            int startIndex = i * leavesPerChild;
            if (startIndex >= numLeaves) break;
            
            int endIndex = Math.min(startIndex + leavesPerChild, numLeaves);
            List<LeafBrokerWithRegionProcessingRegion> childLeaves = leavesToPlace.subList(startIndex, endIndex);
            
            if (childLeaves.isEmpty()) continue;
            
            BrokerWithRegion childBroker = brokerFactory.createBroker(generateBrokerName());
            parent.addChild(childBroker);
            
            buildRecursive(childBroker, childLeaves, currentDepth + 1, maxDepth);
        }
    }

    private List<LeafBrokerWithRegionProcessingRegion> createAllLeafBrokers() {
        List<LeafBrokerWithRegionProcessingRegion> leaves = new ArrayList<>();
        int gridDimension = config.getGridDimension();
        double overlapFactor = config.getOverlapFactor();
        
        double lonStep = (MAX_LON - MIN_LON) / gridDimension;
        double latStep = (MAX_LAT - MIN_LAT) / gridDimension;

        for (int i = 0; i < gridDimension * gridDimension; i++) {
            int gridRow = i / gridDimension;
            int gridCol = i % gridDimension;

            double baseMinLon = MIN_LON + (gridCol * lonStep);
            double baseMaxLon = baseMinLon + lonStep;
            double baseMinLat = MIN_LAT + (gridRow * latStep);
            double baseMaxLat = baseMinLat + latStep;

            double lonExtension = (lonStep / 2.0) * overlapFactor;
            double latExtension = (latStep / 2.0) * overlapFactor;

            double finalMinLon = baseMinLon - (random.nextDouble() * lonExtension);
            double finalMaxLon = baseMaxLon + (random.nextDouble() * latExtension);
            double finalMinLat = baseMinLat - (random.nextDouble() * latExtension);
            double finalMaxLat = baseMaxLat + (random.nextDouble() * latExtension);

            Location bl = new Location(finalMinLon, finalMinLat, 0);
            Location tr = new Location(finalMaxLon, finalMaxLat, 0);
            
            LeafBrokerWithRegionProcessingRegion leafBroker = (LeafBrokerWithRegionProcessingRegion) brokerFactory.createLeafBroker(generateLeafBrokerName(), bl, tr);
            leafBroker.setInternetPopulation(getMockPopulationForRegion(gridRow, gridCol));
            leaves.add(leafBroker);
        }
        return leaves;
    }
    
    private void aggregateData(BrokerWithRegion node) {
        if (node instanceof LeafBrokerWithRegionProcessingRegion) {
            return;
        }
        long aggregatedPopulation = 0;
        Region combinedRegion = new Region();
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof BrokerWithRegion) {
                BrokerWithRegion childBroker = (BrokerWithRegion) childObj;
                aggregateData(childBroker); 
                aggregatedPopulation += childBroker.getInternetPopulation();
                combinedRegion.expand(childBroker.getRegion());
            }
        }
        node.setInternetPopulation(aggregatedPopulation);
        node.getRegion().setBottomLeft(combinedRegion.getBottomLeft());
        node.getRegion().setTopRight(combinedRegion.getTopRight());
    }

    private long getMockPopulationForRegion(int row, int col) {
        int gridDimension = config.getGridDimension();
        if (row > gridDimension / 2 && col > gridDimension / 2) {
            return 1000000;
        }
        if (row < gridDimension / 2 && col < gridDimension / 2) {
            return 250000;
        }
        return 50000;
    }

    @Override
    protected void attachSubscribers(BrokerWithRegion root) {
        System.out.println("GridTopologyGenerator: Subscriber attachment is handled by a dedicated generator class.");
    }

    @Override
    protected void attachPublishers(BrokerWithRegion root) {
         System.out.println("GridTopologyGenerator: Publisher attachment is handled by a dedicated generator class.");
    }

    public List<LeafBrokerWithRegionProcessingRegion> getLeafBrokers() {
        return this.leafBrokers;
    }
}
