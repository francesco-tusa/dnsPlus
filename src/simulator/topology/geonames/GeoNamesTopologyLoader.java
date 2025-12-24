package simulator.topology.geonames;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import simulator.regions.Region;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BoundedBrokerFactory;
import utils.CustomLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Loads the runtime broker topology from a pre-computed JSON file.
 * Extends AbstractTopologyFactory to fit the simulation framework.
 */
public class GeoNamesTopologyLoader
        extends AbstractTopologyFactory<GeoNamesTopologyConfiguration, BoundedBroker> {

    private static final Logger logger = CustomLogger.getLogger(GeoNamesTopologyLoader.class.getName());
    
    private final BoundedBrokerFactory brokerFactory;
    private final List<BoundedBroker> allBrokers = new ArrayList<>();
    private final JsonFactory jsonFactory = new JsonFactory();

    // --- Constants for JSON field names ---
    private static final String JSON_FIELD_NAME = "name";
    private static final String JSON_FIELD_INTERNET_POPULATION = "internetPopulation";
    private static final String JSON_FIELD_CHILDREN = "children";
    private static final String JSON_FIELD_BOUNDS = "bounds";
    private static final String JSON_FIELD_IS_LEAF = "isLeaf";
    
    // --- Constants for Region fields (inside "bounds") ---
    private static final String JSON_FIELD_BOTTOM_LEFT = "bottomLeft";
    private static final String JSON_FIELD_TOP_RIGHT = "topRight";
    private static final String JSON_FIELD_X = "x"; 
    private static final String JSON_FIELD_Y = "y"; 
    
    public GeoNamesTopologyLoader(BoundedBrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        if (!(genericConfig instanceof GeoNamesTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of GeoNamesTopologyConfiguration.");
        }
        this.config = (GeoNamesTopologyConfiguration) genericConfig;
        Objects.requireNonNull(this.config.getTopologyFilePath(), "Topology file path cannot be null in configuration.");

        this.brokerIdCounter = 0;
        this.rootNode = null;
        this.allBrokers.clear();

        logger.fine("GeoNamesTopologyGenerator initialized with config: " + this.config);
    }

    @Override
    protected BoundedBroker buildCoreTopology() {
        Objects.requireNonNull(this.config, "Configuration must be set during initialise before building topology.");
        String filePath = this.config.getTopologyFilePath();
        logger.info("Building core topology from file (streaming): " + filePath);

        try {
            File topologyFile = new File(filePath);
            if (!topologyFile.exists()) {
                throw new IOException("Topology file not found: " + filePath);
            }

            try (JsonParser parser = jsonFactory.createParser(topologyFile)) {
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IOException("Expected JSON to start with an object '{'.");
                }
                
                this.rootNode = buildBrokerFromJsonStream(parser, null);
            }

            if (this.rootNode == null) {
                throw new IllegalStateException("Failed to build root broker from JSON file: " + filePath);
            }

            this.allBrokers.clear();
            addAllBrokersRecursively(this.rootNode);
            logger.info("Successfully built core topology. Root: " + this.rootNode.getName() + ", Total brokers: " + this.allBrokers.size());
            return this.rootNode;

        } catch (IOException e) {
            logger.severe("Failed to read or parse topology file: " + filePath + " " + e.getMessage());
            throw new RuntimeException("Failed to read or parse topology file: " + filePath, e);
        }
    }

    @Override
    public void attachSubscribers(BoundedBroker root) {
         if (root == null || this.rootNode == null || root != this.rootNode) {
             logger.warning("Root node mismatch or null during attachSubscribers. Aborting subscriber attachment.");
             return;
         }
        logger.fine("GeoNamesTopologyGenerator: Subscriber attachment is handled by a dedicated population class.");
    }

    @Override
    public void attachPublishers(BoundedBroker root) {
         if (root == null || this.rootNode == null || root != this.rootNode) {
             logger.warning("Root node mismatch or null during attachPublishers. Aborting publisher attachment.");
             return;
         }
        logger.fine("GeoNamesTopologyGenerator: Publisher attachment is handled by a dedicated population class.");
    }

    /**
     * Recursively builds a Broker from a streaming JsonParser.
     * OPTIMIZATION: Uses 'isLeaf' field (if present) to instantiate the correct class immediately.
     */
    private BoundedBroker buildBrokerFromJsonStream(JsonParser parser, BoundedBroker parent) throws IOException {
        
        BoundedBroker currentBroker = null;
        String pendingName = "broker_" + this.brokerIdCounter++; // Default name
        
        // Loop through fields
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); // Move to value

            if (JSON_FIELD_IS_LEAF.equals(fieldName)) {
                boolean isLeaf = parser.getBooleanValue();
                
                // INSTANTIATE IMMEDIATELY based on hint
                // We create the broker without regions first, then populate them as we parse 'bounds'
                if (isLeaf) {
                    currentBroker = brokerFactory.createLeafBroker(pendingName);
                } else {
                    currentBroker = brokerFactory.createBroker(pendingName);
                }
                if (parent != null) parent.addChild(currentBroker);

            } else if (JSON_FIELD_NAME.equals(fieldName)) {
                pendingName = parser.getText();
                if (currentBroker != null) {
                    currentBroker.setName(pendingName);
                }
            } else if (JSON_FIELD_INTERNET_POPULATION.equals(fieldName)) {
                if (currentBroker != null) {
                    currentBroker.setInternetPopulation(parser.getLongValue());
                }
            } else if (JSON_FIELD_BOUNDS.equals(fieldName)) {
                Region region = parseRegionFromStream(parser);
                if (currentBroker != null && region != null) {
                    currentBroker.getRegion().set(region);
                }
            } else if (JSON_FIELD_CHILDREN.equals(fieldName)) {
                // If 'isLeaf' was processed correctly, currentBroker is already a BoundedBroker (Non-Leaf)
                // Recurse
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    buildBrokerFromJsonStream(parser, currentBroker); 
                }
            } else {
                parser.skipChildren();
            }
        }
        
        return currentBroker;
    }

    private Region parseRegionFromStream(JsonParser parser) throws IOException {
        Location bottomLeft = null;
        Location topRight = null;
        
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); 
            if (JSON_FIELD_BOTTOM_LEFT.equals(fieldName)) {
                bottomLeft = parseLocationFromStream(parser);
            } else if (JSON_FIELD_TOP_RIGHT.equals(fieldName)) {
                topRight = parseLocationFromStream(parser);
            } else {
                parser.skipChildren();
            }
        }

        if (bottomLeft != null && topRight != null) {
            return new Region(bottomLeft, topRight);
        } else {
             return null; 
        }
    }

    private Location parseLocationFromStream(JsonParser parser) throws IOException {
        double x = 0.0, y = 0.0, z = 0.0;
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); 

            if (JSON_FIELD_X.equals(fieldName)) {
                x = parser.getDoubleValue();
            } else if (JSON_FIELD_Y.equals(fieldName)) {
                y = parser.getDoubleValue();
            } else if ("z".equals(fieldName)) {
                z = parser.getDoubleValue();
            } else {
                parser.skipChildren();
            }
        }
        return new Location(x, y, z);
    }

    private void addAllBrokersRecursively(BoundedBroker broker) {
        if (broker == null) return;
        this.allBrokers.add(broker);
        List<TreeNode> children = broker.getChildren();
        if (children != null) {
            for (TreeNode childNode : children) {
                if (childNode instanceof BoundedBroker) {
                    addAllBrokersRecursively((BoundedBroker) childNode);
                }
            }
        }
    }

     public List<BoundedBroker> getBrokers() {
         return new ArrayList<>(this.allBrokers);
     }
}