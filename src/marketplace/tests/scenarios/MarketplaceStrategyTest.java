package marketplace.tests.scenarios;

import java.util.Map;

import marketplace.agents.AbstractMarketplaceBroker;
import marketplace.agents.MarketplaceProvider;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.events.ServiceRequest;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.tests.fixtures.MarketplaceContinuumTopologyFixture;
import marketplace.topology.MarketplaceBrokerFactory;
import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

public class MarketplaceStrategyTest extends TestScenario {

    @Override
    public String getTestName() {
        return "Marketplace Strategy: Distance, Min/Max & Weighted Logic";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING STRATEGY COMPARISON TEST");
        try {
            testDistancePenalty();
            testUtilityWeighting();
            return true;
        } catch (Exception e) {
            System.err.println(">>> STRATEGY TESTS FAILED");
            e.printStackTrace();
            return false;
        }
    }

    private void testDistancePenalty() throws Exception {
        System.out.println("\n=== [Test 1] Distance Penalty (Network Latency) Check ===");
        
        MarketplaceContinuumTopologyFixture fixture = new MarketplaceContinuumTopologyFixture();
        fixture.setup(new MarketplaceBrokerFactory()); 
        
        Location clientLoc = new Location(0, 0, 0);

        MarketplaceProvider pClose = new MarketplaceProvider("pClose_Fog", new Location(10, 0, 0));
        MarketplaceProvider pFar = new MarketplaceProvider("pFar_Cloud", new Location(50, 0, 0));

        // FIX: Add to brokers FIRST
        fixture.findNode("Edge_Westminster", AbstractMarketplaceBroker.class).addChild(pClose);
        fixture.findNode("Cloud_Core", AbstractMarketplaceBroker.class).addChild(pFar);

        // THEN Advertise (Now the provider has a parent, so send() will succeed)
        pClose.advertiseService(1, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0));
        pFar.advertiseService(1, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0));

        // Request preferences
        Map<String, Double> constraints = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 100.0);
        Map<String, Double> weights = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 1.0);
        
        ServiceRequest req = new ServiceRequest(1, constraints, weights, clientLoc);

        WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();
        
        double scoreClose = strategy.inspect(pClose.getLastAdvertisedOffer(), req).score();
        double scoreFar = strategy.inspect(pFar.getLastAdvertisedOffer(), req).score();

        System.out.println("   -> pClose Score (Internal 10ms + Dist 10): " + scoreClose);
        System.out.println("   -> pFar Score   (Internal 10ms + Dist 50): " + scoreFar);

        if (scoreClose < scoreFar) {
            System.out.println("   -> PASS: Strategy correctly penalized network distance.");
        } else {
            throw new RuntimeException("FAIL Test 1: pFar was preferred or equal.");
        }
    }

    private void testUtilityWeighting() throws Exception {
        System.out.println("\n=== [Test 2] Min/Max Optimization (Cost vs Reliability) Check ===");
        
        // Use a basic fixture or create a temporary broker just to satisfy the "send()" requirement
        MarketplaceContinuumTopologyFixture fixture = new MarketplaceContinuumTopologyFixture();
        fixture.setup(new MarketplaceBrokerFactory());

        Location clientLoc = new Location(0, 0, 0);

        MarketplaceProvider pCheap = new MarketplaceProvider("pCheap_Fog", new Location(0, 0, 0));
        MarketplaceProvider pReliable = new MarketplaceProvider("pReliable_Fog", new Location(0, 0, 0));

        // FIX: Add to topology FIRST
        AbstractMarketplaceBroker edge = fixture.findNode("Edge_Westminster", AbstractMarketplaceBroker.class);
        edge.addChild(pCheap);
        edge.addChild(pReliable);

        // THEN Advertise
        // pCheap: Cheap ($10) but lower Reliability (0.90)
        pCheap.advertiseService(1, Map.of(
            MarketplaceMetricSchema.METRIC_COST, 10.0,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.90
        ));

        // pReliable: Expensive ($50) but higher Reliability (0.99)
        pReliable.advertiseService(1, Map.of(
            MarketplaceMetricSchema.METRIC_COST, 50.0,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99
        ));

        // Request
        Map<String, Double> constraints = Map.of(
            MarketplaceMetricSchema.METRIC_COST, 100.0,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.8
        );
        Map<String, Double> weights = Map.of(
            MarketplaceMetricSchema.METRIC_COST, 0.5,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.5
        );

        ServiceRequest req = new ServiceRequest(1, constraints, weights, clientLoc);
        WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();

        double scoreCheap = strategy.inspect(pCheap.getLastAdvertisedOffer(), req).score();
        double scoreReliable = strategy.inspect(pReliable.getLastAdvertisedOffer(), req).score();

        System.out.println("   -> pCheap Utility Score: " + scoreCheap);
        System.out.println("   -> pReliable Utility Score: " + scoreReliable);

        if (scoreCheap < scoreReliable) {
            System.out.println("   -> PASS: Strategy correctly picked CHEAP provider based on Utility Score.");
        } else {
            throw new RuntimeException("FAIL Test 2: Strategy incorrectly prioritized expensive provider.");
        }
    }
}