package marketplace.tests.e2e;

import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;

import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;

public class MarketplaceContinuumE2ETest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceContinuumE2ETest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Continuum: Edge-Fog-Cloud Propagation";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        // --- 1. RETRIEVE TOPOLOGY FROM FIXTURE ---
        // The fixture (MarketplaceContinuumTopologyFixture) has already built the tree.

        MarketplaceBroker cloudBroker = fixture.findNode("Cloud_Core", MarketplaceBroker.class);
        MarketplaceBroker fogBroker = fixture.findNode("Fog_London", MarketplaceBroker.class);
        MarketplaceBroker edgeBroker = fixture.findNode("Edge_Westminster", MarketplaceBroker.class);

        if (cloudBroker == null || fogBroker == null || edgeBroker == null) {
            logger.severe("FAIL: Fixture did not provide the expected Cloud-Fog-Edge topology nodes.");
            return false;
        }

        // --- 2. AGENT DEPLOYMENT ---

        // A. Cloud Provider
        // Location: (900, 900). Scope: Global.
        MarketplaceProvider cloudProvider = new MarketplaceProvider("Prov_Cloud", new Location(900, 900, 0));
        cloudBroker.addChild(cloudProvider);

        // B. Fog Provider
        // Location: (100, 100). Scope: Regional.
        // Covers the client at (10,10) because local scope is +/- 150.
        MarketplaceProvider fogProvider = new MarketplaceProvider("Prov_Fog", new Location(100, 100, 0));
        fogBroker.addChild(fogProvider);

        // C. Edge Provider
        // Location: (10, 10). Scope: Local.
        MarketplaceProvider edgeProvider = new MarketplaceProvider("Prov_Edge", new Location(10, 10, 0));
        edgeBroker.addChild(edgeProvider);

        // D. Client
        // Location: (10, 10).
        MarketplaceClient client = new MarketplaceClient("Client_Mobile", new Location(10, 10, 0));
        edgeBroker.addChild(client);

        // --- 3. ADVERTISEMENT PHASE ---
        System.out.println("\n--- Phase 1: Service Advertisement ---");

        // Advertise same service ID (1001) with different QoS profiles
        cloudProvider.advertiseService(1001, Map.of("latency", 100.0, "cost", 5.0));
        fogProvider.advertiseService(1001, Map.of("latency", 50.0, "cost", 20.0));
        edgeProvider.advertiseService(1001, Map.of("latency", 10.0, "cost", 50.0));

        System.out.println("-> Services Advertised.");

        // --- 4. EXECUTION PHASE ---

        // TEST CASE 1: Low Latency (< 20ms) -> Edge
        System.out.println("\n--- Test Case 1: Requirement < 20ms (Edge Only) ---");
        client.requestService(1001, Map.of("latency", 20.0));
        if (!verifyDelivery(edgeProvider, 1, "Edge"))
            return false;
        if (!verifyDelivery(fogProvider, 0, "Fog"))
            return false;
        if (!verifyDelivery(cloudProvider, 0, "Cloud"))
            return false;
        System.out.println("PASS: Routed to Edge.");

        // TEST CASE 2: Low Cost (< $10) -> Cloud
        System.out.println("\n--- Test Case 2: Requirement < $10 (Cloud Only) ---");
        client.requestService(1001, Map.of("cost", 10.0));
        if (!verifyDelivery(cloudProvider, 1, "Cloud"))
            return false;
        System.out.println("PASS: Routed to Cloud.");

        // TEST CASE 3: Balanced (Lat < 60, Cost < 30) -> Fog
        System.out.println("\n--- Test Case 3: Balanced Req (Fog Only) ---");
        client.requestService(1001, Map.of("latency", 60.0, "cost", 30.0));

        if (!verifyDelivery(fogProvider, 1, "Fog"))
            return false;
        // Edge (Cost 50 > 30) should fail. Count remains 1.
        if (!verifyDelivery(edgeProvider, 1, "Edge"))
            return false;
        // Cloud (Lat 100 > 60) should fail. Count remains 1.
        if (!verifyDelivery(cloudProvider, 1, "Cloud"))
            return false;

        System.out.println("PASS: Routed to Fog.");

        return true;
    }

    private boolean verifyDelivery(MarketplaceProvider prov, int expectedCount, String label) {
        int actual = prov.getnPublications();
        if (actual != expectedCount) {
            logger.severe("FAIL: " + label + " Provider expected " + expectedCount + " requests, but got " + actual);
            return false;
        }
        return true;
    }
}