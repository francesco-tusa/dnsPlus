package simulator.simulations.functional;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import utils.CustomLogger;

import java.util.function.Predicate;
import java.util.logging.Logger;

public class RegionFpFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(RegionFpFunctionalTests.class.getName());
    private static final double TINY_DELTA = 1e-8; 

    public static final Predicate<SpatialRegion> ALL_REGION_TESTS = root -> {
        logger.info("\n>>> SCENARIO: Running Self-Contained Region Class Floating-Point Tests. <<<");
        
        boolean containsTests = testContainsLogic();
        boolean intersectsTests = testIntersectsLogic();
        boolean expandTests = testExpandLogic();
        boolean intersectionMethodTests = testIntersectionMethodLogic();
        boolean aggregationTests = testRegionAggregationLogic();

        boolean allPassed = containsTests && intersectsTests && expandTests && intersectionMethodTests && aggregationTests;

        logger.info("\n--- Validation Result ---");
        if (allPassed) {
            logger.info("SUCCESS: All Region.java floating-point tests passed.");
        } else {
            logger.severe("FAILED: One or more Region.java tests failed. Check logs above.");
        }
        return allPassed;
    };

    private static boolean testContainsLogic() {
        logger.info("\n--- Testing contains() [INCLUSIVE] ---");
        boolean allPassed = true;
        Region r_std = new Region(new Location(10.5, 10.5, 0), new Location(20.5, 20.5, 0));
        allPassed &= assertTest(r_std.contains(new Location(15.0, 15.0, 0)), true, "Standard: Center point");
        allPassed &= assertTest(r_std.contains(new Location(10.5, 10.5, 0)), true, "Standard: Bottom-left corner");
        allPassed &= assertTest(r_std.contains(new Location(10.5 - TINY_DELTA, 15.0, 0)), false, "Standard: Just left");

        Region r_wrap = new Region(new Location(170.1, 0.0, 0), new Location(-170.2, 10.0, 0));
        allPassed &= assertTest(r_wrap.contains(new Location(175.0, 5.0, 0)), true, "Wrap: Inside (right part)");
        allPassed &= assertTest(r_wrap.contains(new Location(-175.0, 5.0, 0)), true, "Wrap: Inside (left part)");
        allPassed &= assertTest(r_wrap.contains(new Location(180.0, 5.0, 0)), true, "Wrap: Inside (antimeridian)");
        allPassed &= assertTest(r_wrap.contains(new Location(0.0, 5.0, 0)), false, "Wrap: In gap (center)");
        return allPassed;
    }

    private static boolean testIntersectsLogic() {
        logger.info("\n--- Testing intersects() [INCLUSIVE] ---");
        boolean allPassed = true;
        Region r_std = new Region(new Location(10.5, 10.5, 0), new Location(20.5, 20.5, 0));
        Region r_overlap = new Region(new Location(20.0, 15.0, 0), new Location(21.0, 25.0, 0));
        allPassed &= assertTest(r_std.intersects(r_overlap), true, "Intersect: Overlapping");

        Region r_touch_right = new Region(new Location(20.5, 10.5, 0), new Location(21.5, 20.5, 0));
        allPassed &= assertTest(r_std.intersects(r_touch_right), true, "Intersect: Touching right edge");

        Region r_wrap = new Region(new Location(170.1, 0.0, 0), new Location(-170.2, 10.0, 0));
        Region r_gap = new Region(new Location(0.0, 0.0, 0), new Location(10.0, 10.0, 0));
        allPassed &= assertTest(r_wrap.intersects(r_gap), false, "Wrap Intersect: In gap");
        
        Region r_touch_gap = new Region(new Location(160.0, 0.0, 0), new Location(170.1, 10.0, 0));
        allPassed &= assertTest(r_wrap.intersects(r_touch_gap), true, "Wrap Intersect: Touching gap edge");
        return allPassed;
    }

    private static boolean testExpandLogic() {
        logger.info("\n--- Testing expand() ---");
        boolean allPassed = true;
        Region r1 = new Region(new Location(10.1, 10.1, 0), new Location(11.1, 11.1, 0));
        r1.expand(new Location(12.2, 12.2, 0));
        allPassed &= assertTest(r1.getTopRight().getX(), 12.2, "Expand: Standard X");

        Region r2 = new Region(new Location(170.5, 0.0, 0), new Location(175.5, 10.0, 0));
        r2.expand(new Location(-170.5, 5.0, 0));
        allPassed &= assertTest(r2.getBottomLeft().getX(), 175.5, "Expand: Wrap minLon");
        allPassed &= assertTest(r2.getTopRight().getX(), -170.5, "Expand: Wrap maxLon");
        return allPassed;
    }

    private static boolean testRegionAggregationLogic() {
        logger.info("\n--- Testing Region Aggregation (expand Region) ---");
        boolean allPassed = true;

        // 1. Non-wrapping Disjoint: [-100, -50] + [50, 100]
        // Shortest bounding box is standard: [-100, 100] (Width 200).
        // Wrapping would be [100, -100] (Width 160) BUT it excludes the content [-50, 50].
        // WAIT. Bounding Box must COVER inputs. 
        // [100, -100] covers 100..180..-100. It does NOT cover [-100, -50] or [50, 100].
        // So wrapping is invalid. Standard is the ONLY validity.
        Region r1 = new Region(new Location(-100, 0, 0), new Location(-50, 10, 0));
        Region r2 = new Region(new Location(50, 0, 0), new Location(100, 10, 0));
        r1.expand(r2);
        allPassed &= assertTest(r1.getBottomLeft().getX(), -100.0, "Agg: Disjoint [-100,100] Min");
        allPassed &= assertTest(r1.getTopRight().getX(), 100.0, "Agg: Disjoint [-100,100] Max");

        // 2. Non-wrapping Overlapping: [-100, 50] + [-50, 100]
        // Covers -100 to 100. Width 200.
        Region r3 = new Region(new Location(-100, 0, 0), new Location(50, 10, 0));
        Region r4 = new Region(new Location(-50, 0, 0), new Location(100, 10, 0));
        r3.expand(r4);
        allPassed &= assertTest(r3.getBottomLeft().getX(), -100.0, "Agg: Overlap [-100,100] Min");
        allPassed &= assertTest(r3.getTopRight().getX(), 100.0, "Agg: Overlap [-100,100] Max");

        // 3. Wrapping Anti-Meridian Disjoint: [-100, -50] + [150, -150]
        // R6 is [150, -150]. Covers 150..180..-150.
        // R5 is [-100, -50].
        // Gap 1: -150 to -100 (Size 50).
        // Gap 2: -50 to 150 (Size 200).
        // Largest Gap is 200.
        // Region is complement of [GapStart=-50, GapEnd=150].
        // Region = [150, -50].
        Region r5 = new Region(new Location(-100, 0, 0), new Location(-50, 10, 0));
        Region r6 = new Region(new Location(150, 0, 0), new Location(-150, 10, 0));
        r5.expand(r6);
        allPassed &= assertTest(r5.getBottomLeft().getX(), 150.0, "Agg: Wrap Disjoint Min");
        allPassed &= assertTest(r5.getTopRight().getX(), -50.0, "Agg: Wrap Disjoint Max");

        // 4. Wrapping Overlapping: [-150, -50] + [150, -100]
        // R7: [-150, -50].
        // R8: [150, -100]. (150..180..-100).
        // Union: 150..180..-100..-50.
        // Contiguous from 150 to -50.
        Region r7 = new Region(new Location(-150, 0, 0), new Location(-50, 10, 0));
        Region r8 = new Region(new Location(150, 0, 0), new Location(-100, 10, 0));
        r7.expand(r8);
        allPassed &= assertTest(r7.getBottomLeft().getX(), 150.0, "Agg: Wrap Overlap Min");
        allPassed &= assertTest(r7.getTopRight().getX(), -50.0, "Agg: Wrap Overlap Max");

        // 5. Full Globe
        Region r9 = new Region(new Location(-150, 0, 0), new Location(0, 10, 0));
        Region r10 = new Region(new Location(-50, 0, 0), new Location(-100, 10, 0));
        r9.expand(r10);
        allPassed &= assertTest(r9.getWidth(), 360.0, "Agg: Full Globe Width check");

        // 6. Point
        Region r11 = new Region(new Location(0, 0, 0), new Location(0, 10, 0));
        Region r12 = new Region(new Location(50, 0, 0), new Location(100, 10, 0));
        r11.expand(r12);
        allPassed &= assertTest(r11.getBottomLeft().getX(), 0.0, "Agg: Point+Region Min");
        allPassed &= assertTest(r11.getTopRight().getX(), 100.0, "Agg: Point+Region Max");

        return allPassed;
    }

    private static boolean testIntersectionMethodLogic() {
        logger.info("\n--- Testing intersection() [Method] ---");
        boolean allPassed = true;
        Region r1 = new Region(new Location(0, 0, 0), new Location(10, 10, 0));
        Region r2 = new Region(new Location(5, 5, 0), new Location(15, 15, 0));
        SpatialRegion res1 = r1.intersection(r2);
        allPassed &= assertTest(res1.getBottomLeft().getX(), 5.0, "Intersection: Standard MinX");

        Region rWrap = new Region(new Location(170, 0, 0), new Location(-170, 10, 0));
        Region rCross = new Region(new Location(-175, 0, 0), new Location(-165, 10, 0));
        SpatialRegion res3 = rWrap.intersection(rCross);
        allPassed &= assertTest(res3.getBottomLeft().getX(), -175.0, "Intersection: WS MinX");
        allPassed &= assertTest(res3.getTopRight().getX(), -170.0, "Intersection: WS MaxX");
        return allPassed;
    }

    private static boolean assertTest(boolean actual, boolean expected, String testName) {
        boolean passed = (actual == expected);
        if (passed) logger.info(String.format("  [PASS] %s", testName));
        else logger.severe(String.format("  [FAIL] %s (Expected: %b, Got: %b)", testName, expected, actual));
        return passed;
    }

    private static boolean assertTest(double actual, double expected, String testName) {
        boolean passed = (Math.abs(actual - expected) < 1e-9);
        if (passed) logger.info(String.format("  [PASS] %s", testName));
        else logger.severe(String.format("  [FAIL] %s (Expected: %f, Got: %f)", testName, expected, actual));
        return passed;
    }
}