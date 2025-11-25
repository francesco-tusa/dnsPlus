package simulator.topology.grid;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BoundedBrokerFactory;
import utils.CustomLogger;

public class GridTopologyGenerator extends AbstractTopologyFactory<GridTopologyConfiguration, BoundedBroker> {

    private static final Logger logger = CustomLogger.getLogger(GridTopologyGenerator.class.getName());
    private final BoundedBrokerFactory brokerFactory;
    private BoundedBroker[][] leafBrokers; // To store leaves for client attachment

    public GridTopologyGenerator(BoundedBrokerFactory brokerFactory) {
        // The factory is passed in and stored as a local field.
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration config) {
        if (!(config instanceof GridTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be of type GridTopologyConfiguration.");
        }
        // Cast and store the specific configuration
        this.config = (GridTopologyConfiguration) config;
        // Reset counters from the superclass
        this.brokerIdCounter = 0;
        this.leafBrokerIdCounter = 0;
        this.subscriberIdCounter = 0;
        this.publisherIdCounter = 0;
    }

    @Override
    protected BoundedBroker buildCoreTopology() {
        logger.info("Building procedural grid-based core broker topology...");
        int dim = this.config.getGridDimension();
        this.leafBrokers = new BoundedBroker[dim][dim];
        double regionWidth = 360.0 / dim;
        double regionHeight = 180.0 / dim;

        // Create the grid of leaf brokers
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                double lon = -180 + i * regionWidth;
                double lat = -90 + j * regionHeight;
                Location p1 = new Location(lon, lat, 0);
                Location p2 = new Location(lon + regionWidth, lat + regionHeight, 0);
                
                String leafName = "leaf-" + i + "-" + j;
                leafBrokers[i][j] = this.brokerFactory.createLeafBroker(leafName, p1, p2);
            }
        }

        BoundedBroker root = buildParentHierarchy(leafBrokers, this.config.getTreeDepth() - 1);
        logger.info("Core broker hierarchy with " + this.config.getNumberOfLeafBrokers() + " leaves created.");
        return root;
    }

    @Override
    public void attachSubscribers(BoundedBroker root) {
        if (leafBrokers == null) return;
        for (int i = 0; i < config.getGridDimension(); i++) {
            for (int j = 0; j < config.getGridDimension(); j++) {
                BoundedBroker leafBroker = leafBrokers[i][j];
                for (int k = 0; k < config.getSubscribersPerLeaf(); k++) {
                    Location loc = leafBroker.getRegion().getRandomLocation();
                    String subName = "sub-" + i + "-" + j + "-" + k;
                    SubscriberWithLocation sub = new SubscriberWithLocation(subName, loc);
                    leafBroker.addChild(sub);
                }
            }
        }
    }

    @Override
    public void attachPublishers(BoundedBroker root) {
        if (leafBrokers == null) return;
        for (int i = 0; i < config.getGridDimension(); i++) {
            for (int j = 0; j < config.getGridDimension(); j++) {
                BoundedBroker leafBroker = leafBrokers[i][j];
                for (int k = 0; k < config.getPublishersPerLeaf(); k++) {
                    Location loc = leafBroker.getRegion().getRandomLocation();
                    String pubName = "pub-" + i + "-" + j + "-" + k;
                    PublisherWithLocation pub = new PublisherWithLocation(pubName, loc);
                    leafBroker.addChild(pub);
                }
            }
        }
    }

    private BoundedBroker buildParentHierarchy(BoundedBroker[][] children, int depth) {
        if (children.length == 1 && children[0].length == 1) {
            return children[0][0];
        }
        
        int parentRows = (int) Math.ceil(children.length / 2.0);
        int parentCols = (int) Math.ceil(children[0].length / 2.0);
        BoundedBroker[][] parents = new BoundedBroker[parentRows][parentCols];

        for (int i = 0; i < parentRows; i++) {
            for (int j = 0; j < parentCols; j++) {
                parents[i][j] = this.brokerFactory.createBroker(generateBrokerName());
                for (int k = 0; k < 2; k++) {
                    for (int l = 0; l < 2; l++) {
                        int childRow = 2 * i + k;
                        int childCol = 2 * j + l;
                        if (childRow < children.length && childCol < children[0].length) {
                            parents[i][j].addChild(children[childRow][childCol]);
                        }
                    }
                }
            }
        }
        return buildParentHierarchy(parents, depth - 1);
    }
}
