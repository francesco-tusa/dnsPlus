package marketplace.tests;

import marketplace.tests.scenarios.MarketplaceAggregationLogicTest;
import marketplace.tests.scenarios.MarketplaceContinuumE2ETest;
import marketplace.tests.scenarios.MarketplaceLogicDecompositionTest;
import marketplace.tests.scenarios.MarketplaceRoutingSelectionTest;
import marketplace.tests.scenarios.MarketplaceStrategyTest;

public class MarketplaceTestSuite {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   MARKETPLACE LOGIC VERIFICATION SUITE   ");
        System.out.println("==========================================");

        boolean allPassed = true;

        // 1. Run Propagation & Routing Test (Topological)
        MarketplaceLogicDecompositionTest propagationTest = new MarketplaceLogicDecompositionTest();
        if (!runTest(propagationTest))
            allPassed = false;

        // 2. Run Basic Selection Test (Sanity Check)
        MarketplaceRoutingSelectionTest selectionTest = new MarketplaceRoutingSelectionTest();
        if (!runTest(selectionTest))
            allPassed = false;

        // 3. Run Strategy Comparison Test (Optimization Logic)
        MarketplaceStrategyTest strategyTest = new MarketplaceStrategyTest();
        if (!runTest(strategyTest))
            allPassed = false;

        // 4. Run N-Dimensional Aggregation Test (Spatial & QoS)
        MarketplaceAggregationLogicTest aggregationTest = new MarketplaceAggregationLogicTest();
        if (!runTest(aggregationTest))
            allPassed = false;
            
        // 5. Run the End-to-End Continuum Test
        // (This validates both brokers route successfully in a Cloud-Fog-Edge topology)
        MarketplaceContinuumE2ETest e2eTest = new MarketplaceContinuumE2ETest();
        if (!runTest(e2eTest))
            allPassed = false;

        System.out.println("\n==========================================");
        if (allPassed) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.err.println("SOME TESTS FAILED");
            System.exit(1);
        }
        System.out.println("==========================================");
    }

    private static boolean runTest(simulator.tests.framework.TestScenario scenario) {
        System.out.println("\n>>> RUNNING: " + scenario.getTestName());
        try {
            boolean result = scenario.run(null);
            if (result)
                System.out.println("   -> RESULT: PASS");
            else
                System.err.println("   -> RESULT: FAIL");
            return result;
        } catch (Exception e) {
            System.err.println("   -> EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}