package marketplace.tests;

import marketplace.tests.scenarios.MarketplaceLogicDecompositionTest;
import marketplace.tests.scenarios.MarketplaceRoutingSelectionTest;

public class MarketplaceTestSuite {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   MARKETPLACE LOGIC VERIFICATION SUITE   ");
        System.out.println("==========================================");

        boolean allPassed = true;

        // 1. Run Propagation & Routing Test
        MarketplaceLogicDecompositionTest propagationTest = new MarketplaceLogicDecompositionTest();
        if (!runTest(propagationTest))
            allPassed = false;

        // 2. Run Instantiation Selection Test
        MarketplaceRoutingSelectionTest selectionTest = new MarketplaceRoutingSelectionTest();
        if (!runTest(selectionTest))
            allPassed = false;

        System.out.println("\n==========================================");
        if (allPassed) {
            System.out.println("✅ ALL TESTS PASSED");
        } else {
            System.err.println("❌ SOME TESTS FAILED");
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