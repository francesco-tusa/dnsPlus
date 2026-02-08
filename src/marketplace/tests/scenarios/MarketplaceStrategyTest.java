package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;
import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.optimization.LatencyFirstStrategy;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.topology.MarketplaceBrokerFactory;

public class MarketplaceStrategyTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceStrategyTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Strategy: Weighted vs Latency (Local & End-to-End)";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING STRATEGY COMPARISON TEST");
        
        boolean localPass = runLocalCheck();
        boolean networkPass = runEndToEndNetworkCheck();
        
        if (localPass && networkPass) {
            System.out.println(">>> ALL STRATEGY TESTS PASSED");
            return true;
        } else {
            System.err.println(">>> STRATEGY TESTS FAILED");
            return false;
        }
    }

    /**
     * PHASE 1 & 2: Local Logic Check (Single Broker)
     * Verifies the Strategy calculation without network complexity.
     */
    private boolean runLocalCheck() {
        System.out.println("\n=== [Phases 1-2] Local Strategy Logic Check ===");

        // 1. Setup Single Leaf Broker (Needs bounds as it is a leaf)
        MarketplaceBrokerFactory factory = new MarketplaceBrokerFactory();
        MarketplaceBroker broker = (MarketplaceBroker) factory.createLeafBroker("LocalBroker",
                new Location(0, 0, 0), new Location(100, 100, 0));

        // 2. Setup Providers
        // P1: Fast (10ms) but Expensive ($100)
        MarketplaceProvider pFast = new MarketplaceProvider("Fog_FastExpensive", new Location(50, 50, 0));
        broker.addChild(pFast);
        pFast.advertiseService(2001, Map.of("latency", 10.0, "cost", 100.0));

        // P2: Slow (50ms) but Cheap ($10)
        MarketplaceProvider pSlow = new MarketplaceProvider("Fog_SlowCheap", new Location(50, 50, 0));
        broker.addChild(pSlow);
        pSlow.advertiseService(2001, Map.of("latency", 50.0, "cost", 10.0));

        // Client at same location
        MarketplaceClient client = new MarketplaceClient("LocalClient", new Location(50, 50, 0));
        broker.addChild(client);

        // --- PHASE 1: LATENCY FIRST ---
        System.out.println("   [Phase 1] Testing LatencyFirstStrategy...");
        broker.setSelectionStrategy(new LatencyFirstStrategy());
        
        int fastCountBefore = pFast.getnPublications();
        int slowCountBefore = pSlow.getnPublications();

        // Request (Weights ignored by LatencyFirst)
        client.requestService(2001, Map.of("latency", 100.0, "cost", 100.0));

        if (pFast.getnPublications() == fastCountBefore + 1 && pSlow.getnPublications() == slowCountBefore) {
            System.out.println("   -> PASS: LatencyFirst correctly picked Fast provider.");
        } else {
            logger.severe("FAIL Phase 1: LatencyFirst selection error. Fast=" + pFast.getnPublications() + " Slow=" + pSlow.getnPublications());
            return false;
        }

        // --- PHASE 2: WEIGHTED UTILITY ---
        System.out.println("   [Phase 2] Testing WeightedUtilityStrategy (Cost Preference)...");
        broker.setSelectionStrategy(new WeightedUtilityStrategy());

        fastCountBefore = pFast.getnPublications();
        slowCountBefore = pSlow.getnPublications();

        // Weights: Latency=0.1, Cost=0.9 (User cares about COST)
        client.requestService(2001, 
            Map.of("latency", 100.0, "cost", 100.0), 
            Map.of("latency", 0.1, "cost", 0.9) 
        );

        if (pSlow.getnPublications() == slowCountBefore + 1 && pFast.getnPublications() == fastCountBefore) {
            System.out.println("   -> PASS: WeightedStrategy correctly picked Cheap provider.");
        } else {
            logger.severe("FAIL Phase 2: WeightedUtility selection error. Fast=" + pFast.getnPublications() + " Slow=" + pSlow.getnPublications());
            return false;
        }

        return true;
    }

    /**
     * PHASE 3: End-to-End Hierarchical Check
     * Verifies that the Root Broker can route to different branches based on Strategy.
     */
    private boolean runEndToEndNetworkCheck() {
        System.out.println("\n=== [Phase 3] End-to-End Hierarchical Routing Check ===");

        MarketplaceBrokerFactory factory = new MarketplaceBrokerFactory();

        // 1. Topology Setup: Y-Shape
        // CORRECTION: Use createBroker() for the Root. It relies on aggregation.
        MarketplaceBroker root = (MarketplaceBroker) factory.createBroker("RootBroker");
        
        // Branch A: Holds the Cheap Provider (Must be a LEAF with bounds)
        MarketplaceBroker bCheap = (MarketplaceBroker) factory.createLeafBroker("Broker_BranchA",
            new Location(0,0,0), new Location(100,100,0));
        
        // Branch B: Holds the Fast Provider (Must be a LEAF with bounds)
        MarketplaceBroker bFast = (MarketplaceBroker) factory.createLeafBroker("Broker_BranchB",
            new Location(0,0,0), new Location(100,100,0)); 
            
        // Branch C: Holds the Client
        MarketplaceBroker bClient = (MarketplaceBroker) factory.createLeafBroker("Broker_BranchC",
            new Location(0,0,0), new Location(100,100,0));

        // Construct Tree
        root.addChild(bCheap);
        root.addChild(bFast);
        root.addChild(bClient);

        // 2. Populate Agents
        MarketplaceProvider pCheap = new MarketplaceProvider("Fog_BranchA_Cheap", new Location(40, 50, 0));
        bCheap.addChild(pCheap);
        pCheap.advertiseService(3001, Map.of("latency", 50.0, "cost", 10.0)); // Slow but Cheap

        MarketplaceProvider pFast = new MarketplaceProvider("Fog_BranchB_Fast", new Location(60, 50, 0));
        bFast.addChild(pFast);
        pFast.advertiseService(3001, Map.of("latency", 10.0, "cost", 100.0)); // Fast but Expensive

        MarketplaceClient client = new MarketplaceClient("NetClient", new Location(50, 50, 0));
        bClient.addChild(client);

        // 3. Configure Root Strategy
        root.setSelectionStrategy(new WeightedUtilityStrategy());

        // Snapshot counters
        int cheapCountBefore = pCheap.getnPublications();
        int fastCountBefore = pFast.getnPublications();

        // --- TEST CASE A: Client prefers COST (Should route to Branch A) ---
        System.out.println("   [Step A] Requesting Cheap Service (Weights: Cost=0.9)...");
        client.requestService(3001, 
            Map.of("latency", 100.0, "cost", 100.0), 
            Map.of("latency", 0.1, "cost", 0.9)
        );

        if (pCheap.getnPublications() == cheapCountBefore + 1 && pFast.getnPublications() == fastCountBefore) {
            System.out.println("   -> PASS: Root routed request down Branch A (Cheap).");
        } else {
            logger.severe("FAIL Network Step A: Routing Error. Cheap=" + pCheap.getnPublications() + " Fast=" + pFast.getnPublications());
            return false;
        }

        // Reset Snapshot
        cheapCountBefore = pCheap.getnPublications();
        fastCountBefore = pFast.getnPublications();

        // --- TEST CASE B: Client prefers SPEED (Should route to Branch B) ---
        System.out.println("   [Step B] Requesting Fast Service (Weights: Latency=0.9)...");
        client.requestService(3001, 
            Map.of("latency", 100.0, "cost", 100.0), 
            Map.of("latency", 0.9, "cost", 0.1)
        );

        if (pFast.getnPublications() == fastCountBefore + 1 && pCheap.getnPublications() == cheapCountBefore) {
            System.out.println("   -> PASS: Root routed request down Branch B (Fast).");
        } else {
            logger.severe("FAIL Network Step B: Routing Error. Cheap=" + pCheap.getnPublications() + " Fast=" + pFast.getnPublications());
            return false;
        }

        return true;
    }
}