package marketplace.tests.scenarios;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;
import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.common.MarketplaceMetricSchema;

public class MarketplaceStrategyTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceStrategyTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Strategy: Distance, Min/Max & Weighted Logic";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING STRATEGY COMPARISON TEST");
        
        boolean distancePass = runDistancePenaltyCheck();
        boolean minMaxPass = runMinMaxOptimizationCheck();
        
        if (distancePass && minMaxPass) {
            System.out.println(">>> ALL STRATEGY TESTS PASSED");
            return true;
        } else {
            System.err.println(">>> STRATEGY TESTS FAILED");
            return false;
        }
    }

    /**
     * Helper to create a Full Schema Map.
     * Ensures that if a test only specifies "Latency", the other fields (Cost, Reliability, etc.)
     * are filled with "Best Case" defaults so the Provider isn't disqualified by the Hypercube check.
     */
    private Map<String, Double> createCompleteOffer(Map<String, Double> specificValues) {
        Map<String, Double> fullOffer = new HashMap<>();
        
        // Fill defaults for all keys in Schema
        for (String key : MarketplaceMetricSchema.KEYS) {
            boolean isMinimize = MarketplaceMetricSchema.DIRECTIONS.get(key);
            // If Minimize (Cost/Lat), default to 0.0 (Best).
            // If Maximize (Rel/Band), default to MAX_VALUE (Best) -> effectively infinite capability.
            // This ensures the Provider is "Valid" for dimensions we aren't testing.
            double bestCase = isMinimize ? 0.0 : Double.MAX_VALUE;
            fullOffer.put(key, bestCase);
        }

        // Overwrite with test-specific values
        fullOffer.putAll(specificValues);
        return fullOffer;
    }

    private boolean runDistancePenaltyCheck() {
        System.out.println("\n=== [Test 1] Distance Penalty (Network Latency) Check ===");

        MarketplaceBrokerFactory factory = new MarketplaceBrokerFactory();
        MarketplaceBroker broker = (MarketplaceBroker) factory.createLeafBroker("DistanceBroker",
                new Location(0, 0, 0), new Location(100, 100, 0));
        
        MarketplaceClient client = new MarketplaceClient("Client_Origin", new Location(0, 0, 0));
        broker.addChild(client);

        // Provider FAR (Distance 50) - Compute 5ms
        // FIX: Renamed to contain "Cloud" to ensure Radius=60.0, covering the client at distance 50.
        MarketplaceProvider pFar = new MarketplaceProvider("Prov_Far_Cloud_Fast", new Location(50, 0, 0));
        broker.addChild(pFar);
        pFar.advertiseService(1001, createCompleteOffer(Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 5.0)));

        // Provider CLOSE (Distance 10) - Compute 20ms
        // FIX: Renamed to contain "Region" to ensure Radius=20.0, covering the client at distance 10.
        MarketplaceProvider pClose = new MarketplaceProvider("Prov_Close_Region_Slow", new Location(10, 0, 0));
        broker.addChild(pClose);
        pClose.advertiseService(1001, createCompleteOffer(Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 20.0)));

        broker.setSelectionStrategy(new WeightedUtilityStrategy());
        
        int farCount = pFar.getnPublications();
        int closeCount = pClose.getnPublications();

        // Request: < 100ms. Weight: 100% Latency.
        System.out.println("   -> Sending Request (Prefers Low Latency)...");
        client.requestService(1001, 
            Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 100.0), 
            Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 1.0)
        );

        // Check if messages were received
        if (pClose.getnPublications() == closeCount + 1 && pFar.getnPublications() == farCount) {
            System.out.println("   -> PASS: Strategy correctly picked CLOSE provider (Total 30ms) over FAR (Total 55ms).");
            return true;
        } else {
            logger.severe("FAIL Test 1: Strategy picked wrong provider (or none). Close=" + 
                          (pClose.getnPublications() - closeCount) + 
                          " Far=" + (pFar.getnPublications() - farCount));
            return false;
        }
    }

    private boolean runMinMaxOptimizationCheck() {
        System.out.println("\n=== [Test 2] Min/Max Optimization (Cost vs Reliability) Check ===");

        MarketplaceBrokerFactory factory = new MarketplaceBrokerFactory();
        MarketplaceBroker broker = (MarketplaceBroker) factory.createLeafBroker("MinMaxBroker",
                new Location(0, 0, 0), new Location(100, 100, 0));

        MarketplaceClient client = new MarketplaceClient("Client_MM", new Location(0, 0, 0));
        broker.addChild(client);

        // P1: Cheap ($10) but Low Reliability (60%)
        // Using "Region" to ensure safe physical radius overlap
        MarketplaceProvider pCheap = new MarketplaceProvider("Prov_Cheap_Risky_Region", new Location(0, 0, 0));
        broker.addChild(pCheap);
        pCheap.advertiseService(2002, createCompleteOffer(Map.of(
            MarketplaceMetricSchema.METRIC_COST, 10.0,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.60
        )));

        // P2: Expensive ($90) but High Reliability (99%)
        MarketplaceProvider pExpensive = new MarketplaceProvider("Prov_Expensive_Safe_Region", new Location(0, 0, 0));
        broker.addChild(pExpensive);
        pExpensive.advertiseService(2002, createCompleteOffer(Map.of(
            MarketplaceMetricSchema.METRIC_COST, 90.0,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99
        )));

        broker.setSelectionStrategy(new WeightedUtilityStrategy());
        
        int cheapCount = pCheap.getnPublications();
        int expCount = pExpensive.getnPublications();

        System.out.println("   -> Sending Request (Equal Weights Cost/Reliability)...");
        client.requestService(2002, 
            Map.of(
                MarketplaceMetricSchema.METRIC_COST, 100.0, 
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.5
            ), 
            Map.of(
                MarketplaceMetricSchema.METRIC_COST, 0.5, 
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.5
            )
        );

        if (pCheap.getnPublications() == cheapCount + 1 && pExpensive.getnPublications() == expCount) {
            System.out.println("   -> PASS: Strategy correctly picked CHEAP provider based on Utility Score.");
            return true;
        } else {
            logger.severe("FAIL Test 2: Strategy picked wrong provider. Cheap=" + 
                          (pCheap.getnPublications() - cheapCount) + 
                          " Expensive=" + (pExpensive.getnPublications() - expCount));
            return false;
        }
    }
}