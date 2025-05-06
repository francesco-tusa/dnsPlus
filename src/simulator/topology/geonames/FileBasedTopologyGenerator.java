package simulator.topology.geonames;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import simulator.Location;
import simulator.regions.Region;
import simulator.TreeNode;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generator for building broker topologies based on a JSON file definition.
 * Parses "bounds" from the JSON to initialize broker Regions.
 * Assumes BrokerWithRegionProcessingRegion and LeafBrokerWithRegionProcessingRegion
 * have constructors that accept (String name, Location p1, Location p2) to set their initial region.
 */
public class FileBasedTopologyGenerator // Changed class name
        extends AbstractTopologyFactory<FileBasedTopologyConfiguration, BrokerWithRegionProcessingRegion> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<BrokerWithRegionProcessingRegion> allBrokers = new ArrayList<>();

    // Constants for JSON field names
    private static final String JSON_FIELD_BROKER_ID = "brokerId"; // Or "name" or "geonameId" if that's what your JSON uses for the broker's name
    private static final String JSON_FIELD_CHILDREN = "children";
    private static final String JSON_FIELD_BOUNDS = "bounds"; // For the region
    // Fields within "bounds"
    private static final String JSON_FIELD_MIN_LAT = "minLat";
    private static final String JSON_FIELD_MAX_LAT = "maxLat";
    private static final String JSON_FIELD_MIN_LON = "minLon";
    private static final String JSON_FIELD_MAX_LON = "maxLon";
    // Optional: if JSON also has a specific 'location' for the broker itself (e.g. centroid)
    // These are for a generic single location point, not directly used for region corners here.
    private static final String JSON_FIELD_LOCATION = "location";
    private static final String JSON_FIELD_LATITUDE = "latitude";
    private static final String JSON_FIELD_LONGITUDE = "longitude";
    private static final String JSON_FIELD_ALTITUDE = "altitude";


    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        if (!(genericConfig instanceof FileBasedTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of FileBasedTopologyConfiguration.");
        }
        this.config = (FileBasedTopologyConfiguration) genericConfig;
        Objects.requireNonNull(this.config.getTopologyFilePath(), "Topology file path cannot be null in configuration.");

        this.brokerIdCounter = 0; // Used if brokerId is generated
        this.leafBrokerIdCounter = 0; // May not be needed
        this.subscriberIdCounter = 0;
        this.publisherIdCounter = 0;
        this.rootNode = null;
        this.allBrokers.clear();

        System.out.println("FileBasedTopologyGenerator initialized with config: " + this.config);
    }

    @Override
    protected BrokerWithRegionProcessingRegion buildCoreTopology() {
        Objects.requireNonNull(this.config, "Configuration must be set during initialise before building topology.");
        String filePath = this.config.getTopologyFilePath();
        System.out.println("Building core topology from file: " + filePath);

        try {
            File topologyFile = new File(filePath);
            if (!topologyFile.exists()) {
                throw new IOException("Topology file not found: " + filePath);
            }

            JsonNode rootJsonNode = objectMapper.readTree(topologyFile);
            this.rootNode = buildBrokerFromJson(rootJsonNode, null);

            if (this.rootNode == null) {
                throw new IllegalStateException("Failed to build root broker from JSON file: " + filePath);
            }

            this.allBrokers.clear();
            addAllBrokersRecursively(this.rootNode);

            System.out.println("Successfully built core topology. Root: " + this.rootNode.getName() + ", Total brokers: " + this.allBrokers.size());
            return this.rootNode;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read or parse topology file: " + filePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Error building core topology from JSON file: " + filePath, e);
        }
    }

    @Override
    protected void attachSubscribers(BrokerWithRegionProcessingRegion root) {
         if (root == null || this.rootNode == null || root != this.rootNode) {
             System.err.println("Warning: Root node mismatch or null during attachSubscribers. Aborting subscriber attachment.");
             return;
         }
        System.out.println("Attaching subscribers (Placeholder - No subscribers attached by default from JSON).");
        // If subscribers are added here, their locations will expand the broker's region
        // via the existing updateRegion mechanism in LeafBrokerWithRegionProcessingRegion.
    }

    @Override
    protected void attachPublishers(BrokerWithRegionProcessingRegion root) {
         if (root == null || this.rootNode == null || root != this.rootNode) {
             System.err.println("Warning: Root node mismatch or null during attachPublishers. Aborting publisher attachment.");
             return;
         }
        System.out.println("Attaching publishers (Placeholder - No publishers attached by default from JSON).");
    }

    /**
     * Recursively builds a broker and its children from a JSON node.
     * Initializes the broker's region based on the "bounds" field in the JSON.
     *
     * @param jsonNode The JSON node representing the broker.
     * @param parent   The parent broker object (null for the root).
     * @return The constructed BrokerWithRegionProcessingRegion.
     * @throws IOException If JSON parsing fails.
     */
    private BrokerWithRegionProcessingRegion buildBrokerFromJson(JsonNode jsonNode, BrokerWithRegionProcessingRegion parent) throws IOException {
         if (jsonNode == null || !jsonNode.isObject()) {
             System.err.println("Warning: Encountered invalid JSON node structure while building broker. Skipping.");
             return null;
         }
         // Use "name" from JSON if "brokerId" is not the correct field for the broker's name
         // Or use "geonameId" if that's the unique identifier and name.
         String brokerName = jsonNode.path(JSON_FIELD_BROKER_ID).asText(); 
         if (brokerName.isEmpty()) {
             // Fallback or use another field like "name" if "brokerId" is missing or not the intended name
             brokerName = jsonNode.path("name").asText("broker_" + this.brokerIdCounter++);
         } else {
             this.brokerIdCounter++; // Increment if using a generated suffix is not desired when ID is present
         }
         
         // Parse the region for this broker from the "bounds" field
         Region region = parseRegion(jsonNode.path(JSON_FIELD_BOUNDS));

         JsonNode childrenNode = jsonNode.path(JSON_FIELD_CHILDREN);
         boolean isLeafNodeInJson = !childrenNode.isArray() || childrenNode.isEmpty();

         BrokerWithRegionProcessingRegion currentBroker;
          try {
             Location p1 = region.getBottomLeft();
             Location p2 = region.getTopRight();

             if (p1 == null || p2 == null) {
                 System.err.println("Warning: Region for broker " + brokerName + " has null corners from JSON. Using default point region for broker construction.");
                 Location defaultLoc = new Location(0.0, 0.0, 0.0); // Assumes Location(double,double,double)
                 p1 = defaultLoc;
                 p2 = defaultLoc;
             }

             if (isLeafNodeInJson) {
                 // Assumes constructor: LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2)
                 currentBroker = new LeafBrokerWithRegionProcessingRegion(brokerName, p1, p2);
             } else {
                 // Assumes constructor: BrokerWithRegionProcessingRegion(String name, Location p1, Location p2)
                 currentBroker = new BrokerWithRegionProcessingRegion(brokerName, p1, p2);
             }
         } catch (Exception e) {
             System.err.println("Error creating broker instance for " + brokerName + " with region. Check constructors and Region/Location data. Falling back to name-only constructor. Error: " + e.getMessage());
             // Fallback to name-only constructor if region-based construction fails
             if (isLeafNodeInJson) {
                 currentBroker = new LeafBrokerWithRegionProcessingRegion(brokerName);
             } else {
                 currentBroker = new BrokerWithRegionProcessingRegion(brokerName);
             }
             // e.printStackTrace(); // For more detailed debugging
         }

         // Add children (parent link is set by addChild in TreeNode)
         if (!isLeafNodeInJson) {
             if (currentBroker instanceof LeafBrokerWithRegionProcessingRegion && !childrenNode.isEmpty()) {
                  System.err.println("Warning: JSON defines children for broker " + brokerName + " but it was created as a LeafBroker type.");
             }
             for (JsonNode childNode : childrenNode) {
                 BrokerWithRegionProcessingRegion childBroker = buildBrokerFromJson(childNode, currentBroker); // Pass currentBroker as parent
                 if (childBroker != null) {
                     try {
                         currentBroker.addChild(childBroker);
                     } catch (UnsupportedOperationException | ClassCastException e) {
                         System.err.println("Error adding child " + childBroker.getName() + " to parent " + currentBroker.getName() + ": " + e.getMessage());
                         if (e instanceof ClassCastException) throw e; // Re-throw if critical
                     }
                 } else {
                      System.err.println("Warning: Failed to build child broker object from JSON node for parent: " + brokerName);
                 }
             }
         }
         return currentBroker;
    }

    /**
     * Parses a single Location point from a JSON node.
     * This method is a general helper and might not be directly used if "bounds" are parsed directly.
     * Assumes JSON format: {"latitude": double, "longitude": double, "altitude": double (optional)}
     * Assumes Longitude maps to X, Latitude to Y, Altitude to Z for Location constructor.
     *
     * @param locationNode The JSON node representing a single location.
     * @return A Location object. Defaults to (0.0, 0.0, 0.0) if invalid.
     */
    private Location parseLocation(JsonNode locationNode) {
        if (locationNode == null || !locationNode.isObject()) {
            // System.err.println("Debug: parseLocation called with invalid node. Returning default Location(0,0,0).");
            return new Location(0.0, 0.0, 0.0); // Assumes Location(double,double,double)
        }
        
        double lon = locationNode.path(JSON_FIELD_LONGITUDE).asDouble(0.0);
        double lat = locationNode.path(JSON_FIELD_LATITUDE).asDouble(0.0);
        double alt = locationNode.path(JSON_FIELD_ALTITUDE).asDouble(0.0);

        return new Location(lon, lat, alt); // X=lon, Y=lat, Z=alt
    }

    /**
     * Parses Region data from a JSON "bounds" node.
     * Expects {"minLat": val, "maxLat": val, "minLon": val, "maxLon": val}.
     *
     * @param boundsNode The JSON "bounds" node.
     * @return A Region object. Defaults to a point region at (0.0,0.0,0.0) if node is invalid or fields are missing/invalid.
     */
    private Region parseRegion(JsonNode boundsNode) {
        if (boundsNode == null || !boundsNode.isObject()) {
            System.err.println("Warning: Invalid or missing 'bounds' node for a broker. Using default Region (point at origin).");
            Location defaultPoint = new Location(0.0, 0.0, 0.0); // Assumes Location(double,double,double)
            return new Region(defaultPoint, defaultPoint); // Assumes Region(Location, Location)
        }

        // Parse coordinates from the boundsNode
        // Assuming Longitude maps to X, Latitude to Y for Location(x,y,z)
        // And Z defaults to 0.0 for simplicity from 2D bounds
        double minLon = boundsNode.path(JSON_FIELD_MIN_LON).asDouble(0.0);
        double minLat = boundsNode.path(JSON_FIELD_MIN_LAT).asDouble(0.0);
        double maxLon = boundsNode.path(JSON_FIELD_MAX_LON).asDouble(0.0);
        double maxLat = boundsNode.path(JSON_FIELD_MAX_LAT).asDouble(0.0);
        
        // Basic validation for parsed bounds to prevent issues with Region constructor
        // if min > max, which can happen if JSON contains sentinel values like -181 for maxLon.
        if (minLon > maxLon) {
            System.err.println("Warning: Corrected invalid bounds: minLon (" + minLon + ") > maxLon (" + maxLon + "). Setting maxLon = minLon.");
            maxLon = minLon; // Create a "flat" region along this dimension
        }
        if (minLat > maxLat) {
            System.err.println("Warning: Corrected invalid bounds: minLat (" + minLat + ") > maxLat (" + maxLat + "). Setting maxLat = minLat.");
            maxLat = minLat; // Create a "flat" region along this dimension
        }

        // Create Location objects for the corners of the region
        // Assumes Location constructor is (double x, double y, double z)
        // Mapping: X=Longitude, Y=Latitude, Z=0.0 (default for 2D bounds)
        Location bottomLeft = new Location(minLon, minLat, 0.0);
        Location topRight   = new Location(maxLon, maxLat, 0.0);

        // Assumes Region constructor is (Location bottomLeft, Location topRight)
        return new Region(bottomLeft, topRight);
    }

    private void addAllBrokersRecursively(BrokerWithRegionProcessingRegion broker) {
        if (broker == null) return;
        this.allBrokers.add(broker);
        List<TreeNode> children = broker.getChildren(); // Expects List<TreeNode>
        if (children != null) {
            for (TreeNode childNode : children) {
                if (childNode instanceof BrokerWithRegionProcessingRegion) {
                    addAllBrokersRecursively((BrokerWithRegionProcessingRegion) childNode);
                } else if (childNode != null) {
                     System.err.println("Warning: Child node " + childNode.getName() + " is not BrokerWithRegionProcessingRegion during recursive add.");
                }
            }
        }
    }

     public List<BrokerWithRegionProcessingRegion> getBrokers() {
         return new ArrayList<>(this.allBrokers);
     }

    private List<LeafBrokerWithRegionProcessingRegion> findLeafBrokers(BrokerWithRegionProcessingRegion root) {
        List<LeafBrokerWithRegionProcessingRegion> leaves = new ArrayList<>();
        findLeavesRecursive(root, leaves);
        return leaves;
    }

    private void findLeavesRecursive(TreeNode node, List<LeafBrokerWithRegionProcessingRegion> leaves) { // node is TreeNode
        if (node == null) return;
        if (node instanceof LeafBrokerWithRegionProcessingRegion) {
            leaves.add((LeafBrokerWithRegionProcessingRegion) node);
        } else {
            List<TreeNode> children = node.getChildren(); // Expects List<TreeNode>
             if (children != null && !children.isEmpty()) {
                for (TreeNode child : children) {
                    findLeavesRecursive(child, leaves);
                }
            }
        }
    }
}