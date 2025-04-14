package simulator.topology.fixed; // Assuming topology classes are in this package

import java.util.List;
import java.util.Objects;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * Generates a specific, hardcoded topology defined manually.
 */
public class FixedTopologyGenerator extends AbstractTopologyFactory<FixedTopologyConfiguration, BrokerWithRegionProcessingRegion> {

    private BrokerNodes brokerNodes;
    private List<SubscriberWithLocation> subscribers;
    private List<PublisherWithLocation> publishers;

    /**
     * Helper record specifically for broker nodes created.
     * Using specific types as the topology is fixed.
     */
    private record BrokerNodes(
        BrokerWithRegionProcessingRegion root,
        BrokerWithRegionProcessingRegion child1,
        BrokerWithRegionProcessingRegion child2,
        BrokerWithRegionProcessingRegion child3,
        LeafBrokerWithRegionProcessingRegion grandchild1,
        LeafBrokerWithRegionProcessingRegion grandchild2,
        LeafBrokerWithRegionProcessingRegion grandchild3,
        LeafBrokerWithRegionProcessingRegion grandchild4
    ) {}

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        System.out.println("Initialising ManualTopologyGenerator...");
        if (!(genericConfig instanceof FixedTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of ManualTopologyConfiguration.");
        }
        this.config = (FixedTopologyConfiguration) genericConfig;

        // Reset internal state
        this.brokerNodes = null;
        this.subscribers = null;
        this.publishers = null;

        System.out.println("Initialisation complete.");
    }

    @Override
    protected BrokerWithRegionProcessingRegion buildCoreTopology() {
        System.out.println("Building manual core broker topology...");
        Objects.requireNonNull(config, "Configuration must be initialised.");

        // --- Create Brokers ---
        System.out.println("  Creating Brokers...");
        BrokerWithRegionProcessingRegion root = new BrokerWithRegionProcessingRegion("root");
        BrokerWithRegionProcessingRegion child1 = new BrokerWithRegionProcessingRegion("child1");
        BrokerWithRegionProcessingRegion child2 = new BrokerWithRegionProcessingRegion("child2");
        BrokerWithRegionProcessingRegion child3 = new BrokerWithRegionProcessingRegion("child3");
        LeafBrokerWithRegionProcessingRegion grandchild1 = new LeafBrokerWithRegionProcessingRegion("grandchild1");
        LeafBrokerWithRegionProcessingRegion grandchild2 = new LeafBrokerWithRegionProcessingRegion("grandchild2");
        LeafBrokerWithRegionProcessingRegion grandchild3 = new LeafBrokerWithRegionProcessingRegion("grandchild3");
        LeafBrokerWithRegionProcessingRegion grandchild4 = new LeafBrokerWithRegionProcessingRegion("grandchild4");
        System.out.println("  Brokers created.");
        this.brokerNodes = new BrokerNodes(root, child1, child2, child3, grandchild1, grandchild2, grandchild3, grandchild4);

        // --- Build Structure ---
        System.out.println("  Building Topology Structure...");
        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);

        child1.addChild(grandchild1);
        child2.addChild(grandchild2);
        child2.addChild(grandchild3);
        child3.addChild(grandchild4);
        System.out.println("  Broker hierarchy established.");

        System.out.println("Core broker topology built.");
        return root; // Return the root node
    }

    @Override
    protected void attachSubscribers(BrokerWithRegionProcessingRegion root) {
        System.out.println("Attaching manual subscribers...");
        Objects.requireNonNull(brokerNodes, "Broker nodes must be built before attaching subscribers.");

        // --- Create Subscribers ---
        System.out.println("  Creating Subscribers...");
        this.subscribers = List.of(
            new SubscriberWithLocation("sub1", new Location(0, 0, 0)),
            new SubscriberWithLocation("sub2", new Location(5, 2, 0)),
            new SubscriberWithLocation("sub3", new Location(10, 1, 0)),
            new SubscriberWithLocation("sub4", new Location(15, 1, 0)),
            new SubscriberWithLocation("sub5", new Location(4, 3, 0)),
            new SubscriberWithLocation("sub6", new Location(9, 5, 0)),
            new SubscriberWithLocation("sub7", new Location(13, 3, 0)),
            new SubscriberWithLocation("sub8", new Location(20, 5, 0))
        );
        System.out.println("  Subscribers created.");

        // --- Attach Subscribers ---
        System.out.println("  Attaching Subscribers...");
        brokerNodes.grandchild1.addChild(subscribers.get(0)); // s1
        brokerNodes.grandchild2.addChild(subscribers.get(1)); // s2
        brokerNodes.grandchild3.addChild(subscribers.get(2)); // s3
        brokerNodes.grandchild4.addChild(subscribers.get(3)); // s4

        brokerNodes.grandchild1.addChild(subscribers.get(4)); // s5
        brokerNodes.grandchild2.addChild(subscribers.get(5)); // s6
        brokerNodes.grandchild3.addChild(subscribers.get(6)); // s7
        brokerNodes.grandchild4.addChild(subscribers.get(7)); // s8
        System.out.println("  Subscribers attached.");
        System.out.println("Subscriber attachment complete.");
    }

    @Override
    protected void attachPublishers(BrokerWithRegionProcessingRegion root) {
        System.out.println("Attaching manual publishers...");
         Objects.requireNonNull(brokerNodes, "Broker nodes must be built before attaching publishers.");

        // --- Create Publishers ---
        System.out.println("  Creating Publishers...");
        PublisherWithLocation p1 = new PublisherWithLocation("pub1", new Location(7, 7, 0));
        PublisherWithLocation p2 = new PublisherWithLocation("pub2", new Location(18, 4, 0));
        this.publishers = List.of(p1, p2);
        System.out.println("  Publishers created.");

        // --- Attach Publishers ---
        System.out.println("  Attaching Publishers...");
        brokerNodes.grandchild1.addChild(publishers.get(0)); // p1
        brokerNodes.grandchild4.addChild(publishers.get(1)); // p2
        System.out.println("  Publishers attached.");
        System.out.println("Publisher attachment complete.");
    }

    /** Expose nodes if needed by external test logic.
     * Alternatively, tests could traverse the generated tree using rootNode.
     */
    public BrokerNodes getBrokerNodes() { return brokerNodes; }
    public List<SubscriberWithLocation> getSubscribers() { return subscribers; }
    public List<PublisherWithLocation> getPublishers() { return publishers; }
}