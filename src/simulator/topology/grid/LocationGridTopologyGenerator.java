package simulator.topology.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import simulator.Location;
import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingLocation;
import simulator.regions.LeafBrokerWithRegionProcessingLocation;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * A topology generator that creates a grid-based world using the
 * location-processing broker types.
 */
public class LocationGridTopologyGenerator extends AbstractTopologyFactory<GridTopologyConfiguration, BrokerWithRegionProcessingLocation> {

    private List<LeafBrokerWithRegionProcessingLocation> leafBrokers;
    private final Random random = new Random();

    private static final double MIN_LON = -180.0;
    private static final double MAX_LON = 180.0;
    private static final double MIN_LAT = -90.0;
    private static final double MAX_LAT = 90.0;

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        System.out.println("Initialising LocationGridTopologyGenerator...");
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
    protected BrokerWithRegionProcessingLocation buildCoreTopology() {
        System.out.println("Building procedural grid-based core broker topology with location-processing brokers...");
        Objects.requireNonNull(config, "Configuration must be initialised before building topology.");

        BrokerWithRegionProcessingLocation root = new BrokerWithRegionProcessingLocation("Root");

        List<LeafBrokerWithRegionProcessingLocation> allLeaves = createAllLeafBrokers();
        this.leafBrokers.addAll(allLeaves);

        buildRecursive(root, allLeaves, 1, config.getTreeDepth());
        
        // Correctly aggregate data from the bottom up after the tree is built.
        aggregateData(root);

        System.out.println("Core broker hierarchy with " + allLeaves.size() + " leaves created.");
        System.out.println("Root node total internet population: " + root.getInternetPopulation());

        return root;
    }

    private void buildRecursive(BrokerWithRegion parent, List<LeafBrokerWithRegionProcessingLocation> leavesToPlace, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth - 1) {
            for (LeafBrokerWithRegionProcessingLocation leaf : leavesToPlace) {
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
            List<LeafBrokerWithRegionProcessingLocation> childLeaves = leavesToPlace.subList(startIndex, endIndex);
            
            if (childLeaves.isEmpty()) continue;
            
            BrokerWithRegionProcessingLocation childBroker = new BrokerWithRegionProcessingLocation(generateBrokerName());
            parent.addChild(childBroker);
            
            buildRecursive(childBroker, childLeaves, currentDepth + 1, maxDepth);
        }
    }

    private List<LeafBrokerWithRegionProcessingLocation> createAllLeafBrokers() {
        List<LeafBrokerWithRegionProcessingLocation> leaves = new ArrayList<>();
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
            
            LeafBrokerWithRegionProcessingLocation leafBroker = new LeafBrokerWithRegionProcessingLocation(generateLeafBrokerName(), bl, tr);
            leafBroker.setInternetPopulation(getMockPopulationForRegion(gridRow, gridCol));
            leaves.add(leafBroker);
        }
        return leaves;
    }
    
    /**
     * A single recursive method to aggregate both population and region data
     * in a post-order traversal (children first, then parent). This version
     * manually calculates the bounding box to avoid incorrect wrapping logic.
     * @param node The current node to process.
     */
    private void aggregateData(BrokerWithRegion node) {
        if (node instanceof LeafBrokerWithRegionProcessingLocation) {
            return;
        }
        
        long aggregatedPopulation = 0;
        // Use sentinel values for min/max calculation
        double minLon = 181, maxLon = -181, minLat = 91, maxLat = -91;

        for (Object childObj : node.getChildren()) {
            if (childObj instanceof BrokerWithRegion) {
                BrokerWithRegion childBroker = (BrokerWithRegion) childObj;
                aggregateData(childBroker); // Recurse first

                aggregatedPopulation += childBroker.getInternetPopulation();
                
                // Manually find the min/max coordinates across all children
                Region childRegion = childBroker.getRegion();
                if (childRegion != null && childRegion.getBottomLeft() != null && childRegion.getTopRight() != null) {
                    minLon = Math.min(minLon, childRegion.getBottomLeft().getX());
                    minLat = Math.min(minLat, childRegion.getBottomLeft().getY());
                    maxLon = Math.max(maxLon, childRegion.getTopRight().getX());
                    maxLat = Math.max(maxLat, childRegion.getTopRight().getY());
                }
            }
        }

        node.setInternetPopulation(aggregatedPopulation);
        // Only set region if valid children were found
        if (minLon <= maxLon && minLat <= maxLat) { 
            node.getRegion().setBottomLeft(new Location(minLon, minLat, 0));
            node.getRegion().setTopRight(new Location(maxLon, maxLat, 0));
        }
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
    protected void attachSubscribers(BrokerWithRegionProcessingLocation root) {}

    @Override
    protected void attachPublishers(BrokerWithRegionProcessingLocation root) {}

    public List<LeafBrokerWithRegionProcessingLocation> getLeafBrokers() {
        return this.leafBrokers;
    }
}
