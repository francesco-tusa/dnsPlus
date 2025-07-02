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
 * Parses "bounds" from the JSON to initialize broker Regions and also parses
 * the "internetPopulation" for data-driven client allocation.
 */
public class FileBasedTopologyGenerator // Changed class name
        extends AbstractTopologyFactory<FileBasedTopologyConfiguration, BrokerWithRegionProcessingRegion> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<BrokerWithRegionProcessingRegion> allBrokers = new ArrayList<>();

    // Constants for JSON field names
    private static final String JSON_FIELD_NAME = "name";
    private static final String JSON_FIELD_INTERNET_POPULATION = "internetPopulation";
    private static final String JSON_FIELD_CHILDREN = "children";
    private static final String JSON_FIELD_BOUNDS = "bounds";
    private static final String JSON_FIELD_MIN_LAT = "minLat";
    private static final String JSON_FIELD_MAX_LAT = "maxLat";
    private static final String JSON_FIELD_MIN_LON = "minLon";
    private static final String JSON_FIELD_MAX_LON = "maxLon";


    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        if (!(genericConfig instanceof FileBasedTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of FileBasedTopologyConfiguration.");
        }
        this.config = (FileBasedTopologyConfiguration) genericConfig;
        Objects.requireNonNull(this.config.getTopologyFilePath(), "Topology file path cannot be null in configuration.");

        this.brokerIdCounter = 0;
        this.leafBrokerIdCounter = 0;
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
        System.out.println("FileBasedTopologyGenerator: Subscriber attachment is handled by a dedicated generator class.");
    }

    @Override
    protected void attachPublishers(BrokerWithRegionProcessingRegion root) {
         if (root == null || this.rootNode == null || root != this.rootNode) {
             System.err.println("Warning: Root node mismatch or null during attachPublishers. Aborting publisher attachment.");
             return;
         }
        System.out.println("FileBasedTopologyGenerator: Publisher attachment is handled by a dedicated generator class.");
    }

    /**
     * Recursively builds a broker and its children from a JSON node.
     * Initializes the broker's region and internet population from the JSON.
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

         String brokerName = jsonNode.path(JSON_FIELD_NAME).asText("broker_" + this.brokerIdCounter++);
         long internetPopulation = jsonNode.path(JSON_FIELD_INTERNET_POPULATION).asLong(0);
         
         Region region = parseRegion(jsonNode.path(JSON_FIELD_BOUNDS));

         JsonNode childrenNode = jsonNode.path(JSON_FIELD_CHILDREN);
         boolean isLeafNodeInJson = !childrenNode.isArray() || childrenNode.isEmpty();

         BrokerWithRegionProcessingRegion currentBroker;
          try {
             Location p1 = region.getBottomLeft();
             Location p2 = region.getTopRight();

             if (p1 == null || p2 == null) {
                 System.err.println("Warning: Region for broker " + brokerName + " has null corners. Using default point region.");
                 Location defaultLoc = new Location(0.0, 0.0, 0.0);
                 p1 = defaultLoc;
                 p2 = defaultLoc;
             }

             if (isLeafNodeInJson) {
                 currentBroker = new LeafBrokerWithRegionProcessingRegion(brokerName, p1, p2);
             } else {
                 currentBroker = new BrokerWithRegionProcessingRegion(brokerName, p1, p2);
             }
             // We need a way to store the internet population on the broker node.
             // Since BrokerWithRegion doesn't have a field for this, we will need to
             // add one or use a workaround. For now, we will print it.
             // In a real implementation, you would add `setInternetPopulation(long)` to BrokerWithRegion.
             System.out.println("  Created " + brokerName + " with Internet Population: " + internetPopulation);


         } catch (Exception e) {
             System.err.println("Error creating broker instance for " + brokerName + ". Error: " + e.getMessage());
             e.printStackTrace();
             return null;
         }

         if (!isLeafNodeInJson) {
             for (JsonNode childNode : childrenNode) {
                 BrokerWithRegionProcessingRegion childBroker = buildBrokerFromJson(childNode, currentBroker);
                 if (childBroker != null) {
                     currentBroker.addChild(childBroker);
                 }
             }
         }
         return currentBroker;
    }

    private Region parseRegion(JsonNode boundsNode) {
        if (boundsNode == null || !boundsNode.isObject()) {
            System.err.println("Warning: Invalid or missing 'bounds' node. Using default Region (point at origin).");
            Location defaultPoint = new Location(0.0, 0.0, 0.0);
            return new Region(defaultPoint, defaultPoint);
        }

        double minLon = boundsNode.path(JSON_FIELD_MIN_LON).asDouble(0.0);
        double minLat = boundsNode.path(JSON_FIELD_MIN_LAT).asDouble(0.0);
        double maxLon = boundsNode.path(JSON_FIELD_MAX_LON).asDouble(0.0);
        double maxLat = boundsNode.path(JSON_FIELD_MAX_LAT).asDouble(0.0);
        
        if (minLon > maxLon) maxLon = minLon;
        if (minLat > maxLat) maxLat = minLat;

        Location bottomLeft = new Location(minLon, minLat, 0.0);
        Location topRight   = new Location(maxLon, maxLat, 0.0);

        return new Region(bottomLeft, topRight);
    }

    private void addAllBrokersRecursively(BrokerWithRegionProcessingRegion broker) {
        if (broker == null) return;
        this.allBrokers.add(broker);
        List<TreeNode> children = broker.getChildren();
        if (children != null) {
            for (TreeNode childNode : children) {
                if (childNode instanceof BrokerWithRegionProcessingRegion) {
                    addAllBrokersRecursively((BrokerWithRegionProcessingRegion) childNode);
                }
            }
        }
    }

     public List<BrokerWithRegionProcessingRegion> getBrokers() {
         return new ArrayList<>(this.allBrokers);
     }
}