package simulator.tests.fixtures;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.regions.BoundedBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.BoundedBrokerFactory;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import utils.CustomLogger;

public class GeoNamesTopologyFixture implements TopologyFixture {
    private static final Logger logger = CustomLogger.getLogger(GeoNamesTopologyFixture.class.getName());
    private BoundedBroker root;

    @Override
    public void setup(BrokerFactory factory) {
        // Validation: GeoNames specifically requires a region-aware factory
        if (!(factory instanceof BoundedBrokerFactory boundedFactory)) {
            throw new IllegalArgumentException("GeoNamesTopologyFixture requires a BoundedBrokerFactory (Region-based), but received: " + factory.getClass().getSimpleName());
        }

        // 1. Load the Topology Structure
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader loader = new GeoNamesTopologyLoader(boundedFactory);
        this.root = loader.generateTopology(config);

        // 2. Attach Regression Test Clients
        attachRegressionClients();
    }

    private void attachRegressionClients() {
        // Find required brokers using partial name matching (e.g. "Dhaka" -> "Dhaka (Division)")
        // Note: These must be BoundedBrokers because we access their regions below
        BoundedBroker dhaka = findNodeContains("Dhaka", BoundedBroker.class);
        BoundedBroker sylhet = findNodeContains("Sylhet", BoundedBroker.class);
        BoundedBroker rajshahi = findNodeContains("Rajshahi", BoundedBroker.class);
        BoundedBroker chittagong = findNodeContains("Chittagong", BoundedBroker.class);
        BoundedBroker beijing = findNodeContains("Beijing", BoundedBroker.class);

        if (dhaka == null || sylhet == null || rajshahi == null || chittagong == null || beijing == null) {
            logger.warning("Skipping Client Attachment: Could not find required GeoNames brokers. (Is the topology file correct?)");
            return;
        }

        logger.info("Attaching Regression Test Clients to GeoNames Topology...");

        // --- Dhaka Clients ---
        Location dhakaLoc = dhaka.getRegion().getKeyPoints().get(8); // Use a valid point inside region
        attachSubscriber(dhaka, "Sub-Dhaka-Local", dhakaLoc);
        attachSubscriber(dhaka, "Sub-Dhaka-Remote", dhakaLoc);
        attachSubscriber(dhaka, "Sub-Dhaka-Small", dhakaLoc); // For Phase 2 Filtering

        // --- Sylhet Clients ---
        Location sylhetLoc = sylhet.getRegion().getKeyPoints().get(8);
        attachPublisher(sylhet, "Pub-Sylhet", sylhetLoc);
        attachPublisher(sylhet, "Pub-Tibet", sylhetLoc); // For Phase 4 Outlier

        // --- Rajshahi Clients ---
        Location rajshahiLoc = rajshahi.getRegion().getKeyPoints().get(8);
        attachSubscriber(rajshahi, "Sub-Rajshahi", rajshahiLoc);
        attachPublisher(rajshahi, "Pub-Rajshahi", rajshahiLoc);

        // --- Chittagong Clients ---
        Location chitLoc = chittagong.getRegion().getKeyPoints().get(8);
        attachSubscriber(chittagong, "Sub-Chit-A", chitLoc);
        attachSubscriber(chittagong, "Sub-Chit-B", chitLoc);

        // --- Beijing Clients ---
        Location beijingLoc = beijing.getRegion().getKeyPoints().get(8);
        attachPublisher(beijing, "Pub-Beijing", beijingLoc);
    }

    // Helper to ensure consistent attachment + region update
    private void attachSubscriber(BoundedBroker broker, String name, Location loc) {
        SubscriberWithLocation sub = new SubscriberWithLocation(name, loc);
        broker.addChild(sub);
        broker.updateRegion(sub);
    }

    private void attachPublisher(BoundedBroker broker, String name, Location loc) {
        PublisherWithLocation pub = new PublisherWithLocation(name, loc);
        broker.addChild(pub);
    }

    @Override
    public SimulationBroker getRoot() { return this.root; }

    @Override
    public <T extends TreeNode> T findNode(String name, Class<T> clazz) {
        return TopologyAnalyser.findNodeByName(this.root, name, clazz);
    }

    @Override
    public <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz) {
        return TopologyAnalyser.findNodeByNameContains(this.root, partialName, clazz);
    }

    @Override
    public String getName() { return "GeoNames Real-World Topology"; }
}