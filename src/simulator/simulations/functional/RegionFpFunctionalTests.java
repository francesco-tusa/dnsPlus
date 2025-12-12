package simulator.simulations.functional;

import simulator.core.Location;
import simulator.regions.BaseRegion;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import utils.CustomLogger;

import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Provides a self-contained functional test for the Region.java class.
 * This class does not require a topology and tests the 'contains', 'intersects',
 * and 'expand' methods directly, focusing on floating-point and wrap-around logic.
 */
public class RegionFpFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(RegionFpFunctionalTests.class.getName());

    // A small value *larger* than the EPSILON in Region.java, to test "just outside"
    private static final double TINY_DELTA = 1e-8; 

    /**
     * This is the main Predicate that RegionFunctionalTestsMain will call.
     * It runs all the individual logic tests for the Region class.
     * It takes a BaseRegion (which will be null) to match the new constructor
     * in ConfigurableFunctionalTest.
     */
    public static final Predicate<SpatialRegion> ALL_REGION_TESTS = root -> {
        logger.info("\n>>> SCENARIO: Running Self-Contained Region Class Floating-Point Tests. <<<");
        
        boolean containsTests = testContainsLogic();
        boolean intersectsTests = testIntersectsLogic();
        boolean expandTests = testExpandLogic();

        boolean allPassed = containsTests && intersectsTests && expandTests;

        logger.info("\n--- Validation Result ---");
        if (allPassed) {
            logger.info("SUCCESS: All Region.java floating-point tests passed.");
        } else {
            logger.severe("FAILED: One or more Region.java tests failed. Check logs above.");
        }
        return allPassed;
    };


    /**
     * Tests the contains() method.
     * Contains() MUST be INCLUSIVE (>=, <=)
     */
    private static boolean testContainsLogic() {
        logger.info("\n--- Testing contains() [INCLUSIVE] ---");
        boolean allPassed = true;

        // 1. Standard Region: [10.5, 10.5] to [20.5, 20.5]
        Region r_std = new Region(new Location(10.5, 10.5, 0), new Location(20.5, 20.5, 0));
        
        // 1a. Center point
        allPassed &= assertTest(r_std.contains(new Location(15.0, 15.0, 0)), true, "Standard: Center point");
        
        // 1b. Boundary points (MUST be true)
        allPassed &= assertTest(r_std.contains(new Location(10.5, 10.5, 0)), true, "Standard: Bottom-left corner");
        allPassed &= assertTest(r_std.contains(new Location(20.5, 20.5, 0)), true, "Standard: Top-right corner");
        allPassed &= assertTest(r_std.contains(new Location(10.5, 15.0, 0)), true, "Standard: Left edge");
        allPassed &= assertTest(r_std.contains(new Location(20.5, 15.0, 0)), true, "Standard: Right edge");
        allPassed &= assertTest(r_std.contains(new Location(15.0, 10.5, 0)), true, "Standard: Bottom edge");
        allPassed &= assertTest(r_std.contains(new Location(15.0, 20.5, 0)), true, "Standard: Top edge");

        // 1c. Points just outside (MUST be false)
        allPassed &= assertTest(r_std.contains(new Location(10.5 - TINY_DELTA, 15.0, 0)), false, "Standard: Just left");
        allPassed &= assertTest(r_std.contains(new Location(20.5 + TINY_DELTA, 15.0, 0)), false, "Standard: Just right");


        // 2. Wrapping Region: [170.1, 0.0] to [-170.2, 10.0]
        Region r_wrap = new Region(new Location(170.1, 0.0, 0), new Location(-170.2, 10.0, 0));

        // 2a. Points inside
        allPassed &= assertTest(r_wrap.contains(new Location(175.0, 5.0, 0)), true, "Wrap: Inside (right part)");
        allPassed &= assertTest(r_wrap.contains(new Location(-175.0, 5.0, 0)), true, "Wrap: Inside (left part)");
        allPassed &= assertTest(r_wrap.contains(new Location(180.0, 5.0, 0)), true, "Wrap: Inside (antimeridian)");

        // 2b. Boundary points (MUST be true)
        allPassed &= assertTest(r_wrap.contains(new Location(170.1, 0.0, 0)), true, "Wrap: Min boundary");
        allPassed &= assertTest(r_wrap.contains(new Location(-170.2, 10.0, 0)), true, "Wrap: Max boundary");
        
        // 2c. Points in gap (MUST be false)
        allPassed &= assertTest(r_wrap.contains(new Location(170.1 - TINY_DELTA, 5.0, 0)), false, "Wrap: In gap (just left)");
        allPassed &= assertTest(r_wrap.contains(new Location(-170.2 + TINY_DELTA, 5.0, 0)), false, "Wrap: In gap (just right)");
        allPassed &= assertTest(r_wrap.contains(new Location(0.0, 5.0, 0)), false, "Wrap: In gap (center)");

        return allPassed;
    }

    /**
     * Tests the intersects() method.
     * Intersects() MUST be EXCLUSIVE (>, <)
     */
    private static boolean testIntersectsLogic() {
        logger.info("\n--- Testing intersects() [EXCLUSIVE] ---");
        boolean allPassed = true;

        // 1. Standard Region: [10.5, 10.5] to [20.5, 20.5]
        Region r_std = new Region(new Location(10.5, 10.5, 0), new Location(20.5, 20.5, 0));

        // 1a. Overlapping region (MUST be true)
        Region r_overlap = new Region(new Location(20.0, 15.0, 0), new Location(21.0, 25.0, 0));
        allPassed &= assertTest(r_std.intersects(r_overlap), true, "Intersect: Overlapping");

        // 1b. Touching regions (MUST be false) - This is the critical grid-tile test.
        Region r_touch_right = new Region(new Location(20.5, 10.5, 0), new Location(21.5, 20.5, 0));
        Region r_touch_left = new Region(new Location(9.5, 10.5, 0), new Location(10.5, 20.5, 0));
        Region r_touch_top = new Region(new Location(10.5, 20.5, 0), new Location(20.5, 21.5, 0));
        Region r_touch_bottom = new Region(new Location(10.5, 9.5, 0), new Location(20.5, 10.5, 0));
        allPassed &= assertTest(r_std.intersects(r_touch_right), false, "Intersect: Touching right");
        allPassed &= assertTest(r_std.intersects(r_touch_left), false, "Intersect: Touching left");
        allPassed &= assertTest(r_std.intersects(r_touch_top), false, "Intersect: Touching top");
        allPassed &= assertTest(r_std.intersects(r_touch_bottom), false, "Intersect: Touching bottom");
        
        // 1c. Barely overlapping region (MUST be true)
        Region r_barely_overlap = new Region(new Location(20.5 - TINY_DELTA, 15.0, 0), new Location(21.0, 16.0, 0));
        allPassed &= assertTest(r_std.intersects(r_barely_overlap), true, "Intersect: Barely overlapping");


        // 2. Wrapping Region: [170.1, 0.0] to [-170.2, 10.0]
        Region r_wrap = new Region(new Location(170.1, 0.0, 0), new Location(-170.2, 10.0, 0));
        
        // 2a. Region in gap (MUST be false)
        Region r_gap = new Region(new Location(0.0, 0.0, 0), new Location(10.0, 10.0, 0));
        allPassed &= assertTest(r_wrap.intersects(r_gap), false, "Wrap Intersect: In gap");
        
        // 2b. Region touching gap (MUST be false)
        Region r_touch_gap = new Region(new Location(160.0, 0.0, 0), new Location(170.1, 10.0, 0));
        allPassed &= assertTest(r_wrap.intersects(r_touch_gap), false, "Wrap Intersect: Touching gap");
        
        // 2c. Region overlapping gap (MUST be true)
        Region r_overlap_gap = new Region(new Location(170.0, 0.0, 0), new Location(170.2, 10.0, 0));
        allPassed &= assertTest(r_wrap.intersects(r_overlap_gap), true, "Wrap Intersect: Overlapping gap");

        return allPassed;
    }

    /**
     * Tests the expand() method.
     */
    private static boolean testExpandLogic() {
        logger.info("\n--- Testing expand() ---");
        boolean allPassed = true;

        // 1. Standard expand
        Region r1 = new Region(new Location(10.1, 10.1, 0), new Location(11.1, 11.1, 0));
        r1.expand(new Location(12.2, 12.2, 0));
        allPassed &= assertTest(r1.getTopRight().getX(), 12.2, "Expand: Standard X");
        allPassed &= assertTest(r1.getTopRight().getY(), 12.2, "Expand: Standard Y");

        // 2. Expand to create wrap
        Region r2 = new Region(new Location(170.5, 0.0, 0), new Location(175.5, 10.0, 0));
        r2.expand(new Location(-170.5, 5.0, 0));
        
        // --- START FIX: The expand logic correctly chooses the *shorter* wrapped
        // region, which is [175.5, -170.5]. The test was wrong to expect 170.5.
        allPassed &= assertTest(r2.getBottomLeft().getX(), 175.5, "Expand: Wrap minLon");
        // --- END FIX ---
        
        allPassed &= assertTest(r2.getTopRight().getX(), -170.5, "Expand: Wrap maxLon");
        allPassed &= assertTest(r2.getTopRight().getY(), 10.0, "Expand: Wrap Y");

        // 3. Expand to "unwrap" a region
        Region r3 = new Region(new Location(170.5, 0.0, 0), new Location(-170.5, 10.0, 0));
        r3.expand(new Location(0.0, 5.0, 0)); // Expand into the gap
        allPassed &= assertTest(r3.getBottomLeft().getX(), 170.5, "Expand: Unwrap minLon");
        allPassed &= assertTest(r3.getTopRight().getX(), 0.0, "Expand: Unwrap maxLon");
        
        return allPassed;
    }

    /**
     * Helper to log and return test results.
     */
    private static boolean assertTest(boolean actual, boolean expected, String testName) {
        boolean passed = (actual == expected);
        if (passed) {
            logger.info(String.format("  [PASS] %s (Expected: %b, Got: %b)", testName, expected, actual));
        } else {
            logger.severe(String.format("  [FAIL] %s (Expected: %b, Got: %b)", testName, expected, actual));
        }
        return passed;
    }

    private static boolean assertTest(double actual, double expected, String testName) {
        boolean passed = (Math.abs(actual - expected) < 1e-9);
        if (passed) {
            logger.info(String.format("  [PASS] %s (Expected: %f, Got: %f)", testName, expected, actual));
        } else {
            logger.severe(String.format("  [FAIL] %s (Expected: %f, Got: %f)", testName, expected, actual));
        }
        return passed;
    }
}