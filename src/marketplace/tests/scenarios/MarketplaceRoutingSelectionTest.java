package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;
import simulator.topology.factories.BoundedBrokerFactory;

import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.topology.MarketplaceBrokerFactory;

/**
 * LOGIC TEST 2: Optimal Service Instantiation (Selection).
 * * Verifies that the MarketplaceBroker implements "Best Match" logic
 * rather than simple "Feasibility Broadcasting".
 */
public class MarketplaceRoutingSelectionTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceRoutingSelectionTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Logic: Optimal Service Instantiation";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: INSTANTIATION SELECTION");

        // 1. Setup: 1 Broker, 2 Providers, 1 Client
        BoundedBrokerFactory factory = new MarketplaceBrokerFactory();
        MarketplaceBroker broker = (MarketplaceBroker) factory.createLeafBroker("CentralBroker",
                new Location(0, 0, 0), new Location(100, 100, 0));

        // 2. Deploy Providers
        // Provider A: "Best" (Latency 10ms)
        MarketplaceProvider pBest = new MarketplaceProvider("Prov_Best", new Location(10, 10, 0));
        broker.addChild(pBest);
        pBest.advertiseService(1001, Map.of("latency", 10.0));

        // Provider B: "Mediocre" (Latency 18ms)
        MarketplaceProvider pMediocre = new MarketplaceProvider("Prov_Mediocre", new Location(20, 20, 0));
        broker.addChild(pMediocre);
        pMediocre.advertiseService(1001, Map.of("latency", 18.0));

        // 3. Client Requests Service < 20ms
        // Both A (10) and B (18) satisfy the constraint < 20.
        // A naive broker would send to BOTH.
        // An optimal broker must select ONLY A.
        MarketplaceClient client = new MarketplaceClient("Client", new Location(50, 50, 0));
        broker.addChild(client);

        System.out.println("   -> Requesting Service < 20ms.");
        System.out.println("      Candidates: Best(10ms), Mediocre(18ms).");

        client.requestService(1001, Map.of("latency", 20.0));

        // 4. Verification
        int countBest = pBest.getnPublications();
        int countMediocre = pMediocre.getnPublications();

        if (countBest == 1 && countMediocre == 0) {
            System.out.println("   -> PASS: Only Best Provider received the request.");
            System.out.println(">>> INSTANTIATION SELECTION TEST PASSED");
            return true;
        } else if (countBest == 1 && countMediocre == 1) {
            logger.severe("FAIL: Both providers received the request (Broadcasting detected).");
            return false;
        } else if (countBest == 0) {
            logger.severe("FAIL: Best provider did not receive the request.");
            return false;
        } else {
            logger.severe("FAIL: Unexpected counts. Best=" + countBest + ", Mediocre=" + countMediocre);
            return false;
        }
    }
}