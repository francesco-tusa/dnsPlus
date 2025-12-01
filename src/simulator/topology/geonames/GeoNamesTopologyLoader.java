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
    
    // --- Constants for Region fields (inside "bounds") ---
    private static final String JSON_FIELD_BOTTOM_LEFT = "bottomLeft";
    private static final String JSON_FIELD_TOP_RIGHT = "topRight";
    private static final String JSON_FIELD_X = "x"; // Corresponds to Longitude
    private static final String JSON_FIELD_Y = "y"; // Corresponds to Latitude
    
    // --- Default location for fallback ---
    private static final Location DEFAULT_LOCATION = new Location(0.0, 0.0, 0.0);


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
                
                // Recursively build the tree from the stream
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
     * Recursively builds a BrokerWithRegion from a streaming JsonParser.
     * The parser is expected to be at the START_OBJECT ('{') token of a node.
     * This method uses a "create-then-swap" logic to handle streaming.
     * * @param parser The JsonParser, positioned at the '{' of a broker object.
     * @param parent The parent broker (used for linking and region inheritance).
     * @return The constructed BrokerWithRegion.
     * @throws IOException
     */
    private BoundedBroker buildBrokerFromJsonStream(JsonParser parser, BoundedBroker parent) throws IOException {
        
        // --- THIS IS THE "CREATE-THEN-SWAP" LOGIC ---
        // 1. Create a broker *immediately*, assuming it's a NON-LEAF.
        // We need this object to exist so it can be passed as a parent to its children.
        String initialName = "broker_" + this.brokerIdCounter++;
        BoundedBroker currentBroker = brokerFactory.createBroker(initialName);
        if (parent != null) {
            parent.addChild(currentBroker); // This sets the parent link immediately!
        }
        
        String brokerName = initialName;
        long internetPopulation = 0;
        Region region = null;
        boolean isLeafNodeInJson = true; // Assume leaf until "children" array is found

        // 2. Loop through all fields and populate the broker
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); // Move parser to the field's value

            if (JSON_FIELD_NAME.equals(fieldName)) {
                brokerName = parser.getText();
                currentBroker.setName(brokerName);
            } else if (JSON_FIELD_INTERNET_POPULATION.equals(fieldName)) {
                internetPopulation = parser.getLongValue();
                currentBroker.setInternetPopulation(internetPopulation);
            } else if (JSON_FIELD_BOUNDS.equals(fieldName)) {
                region = parseRegionFromStream(parser); 
                if (region != null) {
                    currentBroker.getRegion().set(region);
                }
            } else if (JSON_FIELD_CHILDREN.equals(fieldName)) {
                isLeafNodeInJson = false; // We found children!
                
                // 3. Recurse. Pass 'currentBroker' as the parent for its children.
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    buildBrokerFromJsonStream(parser, currentBroker); // Pass self as parent
                }
            } else {
                parser.skipChildren(); 
            }
        }
        
        // --- 4. THE "SWAP" ---
        // We are at END_OBJECT. We now know if the broker was *actually* a leaf.
        if (isLeafNodeInJson) {
            
            // It was a leaf, but we created a non-leaf. We must replace it.
            Location p1 = null, p2 = null;
            
            if (region != null && region.getBottomLeft() != null) {
                // This leaf had its own valid region.
                p1 = region.getBottomLeft();
                p2 = region.getTopRight();
            } else {
                // Leaf has no region. Find the first valid parent region.
                logger.info("Leaf broker " + brokerName + " has null/missing bounds. Searching for parent region.");
                Region parentRegion = findFirstValidParentRegion(parent); 
                
                if (parentRegion != null) {
                    // Found one! Use its region.
                    p1 = parentRegion.getBottomLeft();
                    p2 = parentRegion.getTopRight();
                    logger.info("  ... Found parent region: " + parentRegion.toShortString() + " and assigned to " + brokerName);
                } else {
                    // Last resort fallback (e.g., for the World node if it was a leaf)
                    logger.warning("Leaf broker " + brokerName + " AND its parents have no region. Using default [0,0,0].");
                    p1 = DEFAULT_LOCATION;
                    p2 = DEFAULT_LOCATION;
                }
            }
            
            // Create the *correct* leaf broker
            BoundedBroker leafBroker = brokerFactory.createLeafBroker(brokerName, p1, p2);
            leafBroker.setInternetPopulation(internetPopulation);
            
            // Swap it into the parent's children list
            if (parent != null) {
                parent.removeChild(currentBroker); // Remove the temp non-leaf
                parent.addChild(leafBroker);     // Add the correct leaf
            }
            return leafBroker; // Return the new leaf
        }

        // It was a non-leaf, so our original object was correct.
        return currentBroker;
    }


    /**
     * Walks up the tree from a broker to find the first ancestor with a valid region.
     * @param broker The broker to start the search from (we search its parents).
     * @return The first valid Region found, or null if no parents have a region.
     */
    private Region findFirstValidParentRegion(BoundedBroker broker) {
        if (broker == null) {
            return null;
        }
        
        // Start search from the broker itself, then go up.
        // A broker passed as 'parent' might have a region.
        BoundedBroker p = broker;
        
        while (p != null) {
            // Check if this parent is a BrokerWithRegion and has a valid region
            Region r = p.getRegion();
            if (r != null && r.getBottomLeft() != null) {
                return r; // Found a valid region
            }
            
            // Move up to the next parent
            if (p.getParent() instanceof BoundedBroker) {
                 p = (BoundedBroker) p.getParent();
            } else {
                 p = null; // Reached root or non-broker node
            }
        }
        
        return null; // No ancestor had a valid region
    }


    /**
     * Parses a Region object from the JSON stream.
     * * @param parser The JsonParser, positioned at the '{' of the bounds object.
     * @return The parsed Region, or NULL if parsing fails (e.g., on "{}").
     * @throws IOException
     */
    private Region parseRegionFromStream(JsonParser parser) throws IOException {
        Location bottomLeft = null;
        Location topRight = null;
        
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
             logger.finer("Parsed a null 'bounds' object.");
             return null; // Handle "bounds": null
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); // Move to value (or '{' of location)

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
             logger.finer("Failed to parse 'bottomLeft' or 'topRight' from bounds stream (region is likely empty). Returning null region.");
             return null; 
        }
    }

    /**
     * Parses a Location object from the JSON stream.
     * * @param parser The JsonParser, positioned at the '{' of the location object.
     * @return The parsed Location, or NULL if the JSON token is null.
     * @throws IOException
     */
    private Location parseLocationFromStream(JsonParser parser) throws IOException {
        double x = 0.0, y = 0.0, z = 0.0;
        
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
             logger.finer("Parsed a null location object (e.g., 'bottomLeft': null).");
             return null;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); // Move to value

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
