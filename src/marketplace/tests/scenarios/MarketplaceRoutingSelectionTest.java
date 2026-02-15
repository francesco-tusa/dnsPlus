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
import marketplace.common.MarketplaceMetricSchema; // Import the schema

public class MarketplaceRoutingSelectionTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceRoutingSelectionTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Logic: Optimal Service Instantiation";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: INSTANTIATION SELECTION");

        BoundedBrokerFactory factory = new MarketplaceBrokerFactory();
        MarketplaceBroker broker = (MarketplaceBroker) factory.createLeafBroker("CentralBroker",
                new Location(0, 0, 0), new Location(100, 100, 0));

        // Provider A: 10ms (Loc 10,10)
        MarketplaceProvider pBest = new MarketplaceProvider("Prov_Best", new Location(10, 10, 0));
        broker.addChild(pBest);
        // FIX: Use proper Schema key and explicitly declare a 10.0 coverage radius
        pBest.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0), 10.0);

        // Provider B: 18ms (Loc 20,20)
        MarketplaceProvider pMediocre = new MarketplaceProvider("Prov_Mediocre", new Location(20, 20, 0));
        broker.addChild(pMediocre);
        // FIX: Use proper Schema key and explicitly declare a 10.0 coverage radius
        pMediocre.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 18.0), 10.0);

        // Client at (14, 14) is physically covered by BOTH providers' 10.0 radius.
        MarketplaceClient client = new MarketplaceClient("Client", new Location(14, 14, 0));
        broker.addChild(client);

        System.out.println("   -> Requesting Service < 20ms.");
        // FIX: Use proper Schema key
        client.requestService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 20.0));

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