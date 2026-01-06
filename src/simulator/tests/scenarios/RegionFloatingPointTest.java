package simulator.tests.scenarios;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

/**
 * Enhanced test suite with verbose logging for manual arithmetic verification.
 * Covers: Contains (Strict), Intersects, Expand, Intersection, Union, and
 * Static Optimization Parity.
 */
public class RegionFloatingPointTest extends TestScenario {

    private static final double TOLERANCE = 1e-5;
    // Restored: Used to verify strict boundary exclusions
    private static final double TINY_DELTA = 1e-4;

    @Override
    public String getTestName() {
        return "Region Floating-Point Logic (Verbose + Union)";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        logger.info(">>> STARTING GEOMETRIC VERIFICATION <<<");

        boolean p1 = testContainsLogic();
        boolean p2 = testIntersectsLogic();
        boolean p3 = testExpandLogic();
        boolean p4 = testIntersectionMethodLogic();
        boolean p5 = testRegionAggregationLogic();
        boolean p6 = testStaticVsInstanceEquivalence();
        boolean p7 = testUnionAreaLogic();

        logger.info(">>> GEOMETRIC VERIFICATION COMPLETE <<<");
        return p1 && p2 && p3 && p4 && p5 && p6 && p7;
    }

    // --- 1. Basic Instance Tests ---

    private boolean testContainsLogic() {
        logger.info("--- Testing Contains Logic ---");
        boolean pass = true;

        // Standard Case: 10.0 to 20.0
        Region rStd = new Region(new Location(10.0, 10.0, 0), new Location(20.0, 20.0, 0));

        pass &= logCheck("Contains Center", rStd.contains(new Location(15.0, 15.0, 0)), true);
        pass &= logCheck("Contains Edge", rStd.contains(new Location(10.0, 10.0, 0)), true);

        pass &= logCheck("Outside Left (Strict)", rStd.contains(new Location(10.0 - TINY_DELTA, 15.0, 0)), false);

        // Wrapping Case
        Region rWrap = new Region(new Location(170.0, 0.0, 0), new Location(-170.0, 10.0, 0));
        pass &= logCheck("Wrap Includes 175", rWrap.contains(new Location(175.0, 5.0, 0)), true);
        pass &= logCheck("Wrap Includes -175", rWrap.contains(new Location(-175.0, 5.0, 0)), true);
        pass &= logCheck("Wrap Includes 180 (Anti-Meridian)", rWrap.contains(new Location(180.0, 5.0, 0)), true);
        pass &= logCheck("Wrap Excludes 0 (Gap)", rWrap.contains(new Location(0.0, 5.0, 0)), false);

        return pass;
    }

    private boolean testIntersectsLogic() {
        logger.info("--- Testing Intersects Logic ---");
        boolean pass = true;
        Region rStd = new Region(new Location(10, 10, 0), new Location(20, 20, 0));
        Region rOverlap = new Region(new Location(15, 15, 0), new Location(25, 25, 0));

        pass &= logCheck("Standard Overlap", rStd.intersects(rOverlap), true);

        // Touching Edge Case
        Region rTouch = new Region(new Location(20.0, 10.0, 0), new Location(21.0, 20.0, 0));
        pass &= logCheck("Touching Edge", rStd.intersects(rTouch), true);

        // Wrapping vs Gap
        Region rWrap = new Region(new Location(170, 0, 0), new Location(-170, 10, 0));
        Region rGap = new Region(new Location(-5, 0, 0), new Location(5, 10, 0));
        pass &= logCheck("Wrap vs Gap (Should Fail)", rWrap.intersects(rGap), false);

        // Wrap Touching
        Region rTouchGap = new Region(new Location(160.0, 0.0, 0), new Location(170.0, 10.0, 0));
        pass &= logCheck("Wrap Touching Edge", rWrap.intersects(rTouchGap), true);

        return pass;
    }

    private boolean testExpandLogic() {
        logger.info("--- Testing Expand Logic ---");
        boolean pass = true;
        Region r1 = new Region(new Location(10.1, 10.1, 0), new Location(11.1, 11.1, 0));
        double oldArea = r1.getArea();

        r1.expand(new Location(12.2, 12.2, 0));
        double newArea = r1.getArea();

        logger.info(String.format("   Expand: Old Area=%.2f -> New Area=%.2f", oldArea, newArea));
        pass &= assertDouble("Expand Standard X", r1.getTopRight().getX(), 12.2);

        // Expand Wraparound
        Region r2 = new Region(new Location(170.5, 0.0, 0), new Location(175.5, 10.0, 0));
        r2.expand(new Location(-170.5, 5.0, 0));
        pass &= assertDouble("Expand Wrap Min", r2.getBottomLeft().getX(), 175.5);
        pass &= assertDouble("Expand Wrap Max", r2.getTopRight().getX(), -170.5);

        return pass;
    }

    private boolean testIntersectionMethodLogic() {
        logger.info("--- Testing Intersection (Region Result) Logic ---");
        boolean pass = true;
        Region r1 = new Region(new Location(0, 0, 0), new Location(10, 10, 0)); // 10x10 = 100
        Region r2 = new Region(new Location(5, 5, 0), new Location(15, 15, 0)); // Overlap is 5x5

        SpatialRegion intersection = r1.intersection(r2);
        if (intersection == null) {
            logger.severe("   [FAIL] Intersection result was null");
            return false;
        }

        pass &= logCheckDouble("Intersection Area", intersection.getArea(), 25.0);
        pass &= assertDouble("Inter Std MinX", intersection.getBottomLeft().getX(), 5.0);

        // Wrapping Intersection
        Region rWrap = new Region(new Location(170, 0, 0), new Location(-170, 10, 0));
        Region rCross = new Region(new Location(-175, 0, 0), new Location(-165, 10, 0));
        SpatialRegion res = rWrap.intersection(rCross);

        if (res == null) {
            logger.severe("   [FAIL] Wrap Intersection result was null");
            return false;
        }
        pass &= assertDouble("Inter Wrap MinX", res.getBottomLeft().getX(), -175.0);
        pass &= assertDouble("Inter Wrap MaxX", res.getTopRight().getX(), -170.0);

        return pass;
    }

    private boolean testUnionAreaLogic() {
        logger.info("--- Testing Union Area Logic ---");
        boolean pass = true;

        // Case 1: Disjoint
        // R1: 10x10 = 100. R2: 10x10 = 100.
        Region r1 = new Region(new Location(0, 0, 0), new Location(10, 10, 0));
        Region r2 = new Region(new Location(20, 0, 0), new Location(30, 10, 0));
        double unionDisjoint = r1.getUnionArea(r2);
        pass &= logCheckDouble("Union Disjoint (100+100)", unionDisjoint, 200.0);

        // Case 2: Overlapping
        // R1: 0,0 to 10,10 (Area 100)
        // R3: 5,0 to 15,10 (Area 100)
        // Overlap: 5,0 to 10,10 (Width 5, Height 10 = Area 50)
        // Union = 100 + 100 - 50 = 150
        Region r3 = new Region(new Location(5, 0, 0), new Location(15, 10, 0));
        double unionOverlap = r1.getUnionArea(r3);

        logger.info(String.format("   Debug: Area1=%.1f, Area2=%.1f, Intersection=%.1f",
                r1.getArea(), r3.getArea(), r1.getIntersectionArea(r3)));

        pass &= logCheckDouble("Union Overlap (100+100-50)", unionOverlap, 150.0);

        // Case 3: Fully Contained
        // R1 (100) contains R4 (1x1=1)
        Region r4 = new Region(new Location(1, 1, 0), new Location(2, 2, 0));
        double unionContained = r1.getUnionArea(r4);
        pass &= logCheckDouble("Union Contained (Max=100)", unionContained, 100.0);

        return pass;
    }

    private boolean testRegionAggregationLogic() {
        logger.info("--- Testing Region Aggregation (MBR Expansion) ---");
        boolean pass = true;

        // 1. Disjoint Merge
        Region r1 = new Region(new Location(-100, 0, 0), new Location(-50, 10, 0));
        Region r2 = new Region(new Location(50, 0, 0), new Location(100, 10, 0));
        r1.expand(r2);
        pass &= assertDouble("Agg Disjoint Min", r1.getBottomLeft().getX(), -100.0);
        pass &= assertDouble("Agg Disjoint Max", r1.getTopRight().getX(), 100.0);

        // 2. Overlap Merge
        Region r3 = new Region(new Location(-100, 0, 0), new Location(50, 10, 0));
        Region r4 = new Region(new Location(-50, 0, 0), new Location(100, 10, 0));
        r3.expand(r4);
        pass &= assertDouble("Agg Overlap Min", r3.getBottomLeft().getX(), -100.0);
        pass &= assertDouble("Agg Overlap Max", r3.getTopRight().getX(), 100.0);

        // 3. Wrap Merge (Disjoint)
        Region r5 = new Region(new Location(-100, 0, 0), new Location(-50, 10, 0));
        Region r6 = new Region(new Location(150, 0, 0), new Location(-150, 10, 0));
        r5.expand(r6);
        pass &= assertDouble("Agg Wrap Min", r5.getBottomLeft().getX(), 150.0);
        pass &= assertDouble("Agg Wrap Max", r5.getTopRight().getX(), -50.0);

        // 4. Wrap Merge (Overlap)
        Region r7 = new Region(new Location(-150, 0, 0), new Location(-50, 10, 0));
        Region r8 = new Region(new Location(150, 0, 0), new Location(-100, 10, 0));
        r7.expand(r8);
        pass &= assertDouble("Agg Wrap Overlap Min", r7.getBottomLeft().getX(), 150.0);
        pass &= assertDouble("Agg Wrap Overlap Max", r7.getTopRight().getX(), -50.0);

        // 5. Full Globe
        Region r9 = new Region(new Location(-150, 0, 0), new Location(0, 10, 0));
        Region r10 = new Region(new Location(-50, 0, 0), new Location(-100, 10, 0));
        r9.expand(r10);
        pass &= assertDouble("Agg Full Globe Width", r9.getWidth(), 360.0);

        return pass;
    }

    private boolean testStaticVsInstanceEquivalence() {
        logger.info("--- Testing Static vs Instance Optimization Parity ---");
        boolean pass = true;

        // Test Case: Standard Overlap
        Region rStd1 = new Region(new Location(10, 10, 0), new Location(20, 20, 0));
        Region rStd2 = new Region(new Location(15, 15, 0), new Location(25, 25, 0));

        // Test Case: Touching Edge (Critical for Grid topologies - Area is 0.0,
        // Intersects is TRUE)
        Region rTouch = new Region(new Location(20.0, 10.0, 0), new Location(21.0, 20.0, 0));

        // Test Case: Wrapping & Gaps
        Region rWrap = new Region(new Location(170, 0, 0), new Location(-170, 10, 0));
        Region rInGap = new Region(new Location(0, 0, 0), new Location(5, 5, 0));

        // 1. PREDICATE PARITY (Fixes the Grid Cross-Corner Failure)
        // Ensures boolean intersects() and fastIntersects() match, especially on
        // boundaries.
        logger.info("  [1/3] Verifying Predicate Parity (Boolean Intersects)...");

        pass &= logPredicateParity("Std Overlap", rStd1, rStd2);
        pass &= logPredicateParity("Boundary Touch", rStd1, rTouch);
        pass &= logPredicateParity("Wrap vs Gap", rWrap, rInGap);

        // 2. AREA PARITY
        // Ensures quantitative intersection areas match.
        logger.info("  [2/3] Verifying Area Parity (getIntersectionArea)...");

        pass &= logAreaParity("Std Area", rStd1, rStd2);
        pass &= logAreaParity("Touch Area (Should be 0.0)", rStd1, rTouch);
        pass &= logAreaParity("Wrap Area", rWrap, rInGap);

        // 3. MBR PARITY
        // Ensures the Minimum Bounding Rectangle calculation matches.
        logger.info("  [3/3] Verifying MBR Parity (fastMBRArea)...");

        pass &= checkMBREquivalence("MBR Std", rStd1, rStd2);
        pass &= checkMBREquivalence("MBR Wrap", rWrap, rInGap);
        pass &= checkMBREquivalence("MBR Bridge",
                new Region(new Location(-175, 0, 0), new Location(-170, 10, 0)),
                new Region(new Location(170, 0, 0), new Location(175, 10, 0)));

        return pass;
    }

    /**
     * Helper to ensure the static fastIntersects matches the instance intersects()
     * method.
     */
    private boolean logPredicateParity(String label, Region r1, Region r2) {
        boolean instanceRes = r1.intersects(r2);
        boolean staticRes = Region.fastIntersects(
                (float) r1.getMinLon(), (float) r1.getMaxLon(), (float) r1.getMinLat(), (float) r1.getMaxLat(),
                (float) r2.getMinLon(), (float) r2.getMaxLon(), (float) r2.getMinLat(), (float) r2.getMaxLat());

        boolean match = (instanceRes == staticRes);
        String status = match ? "PASS" : "FAIL";
        logger.info(String.format("     [%s] %s: Instance=%b, Static=%b", status, label, instanceRes, staticRes));
        return match;
    }

    /**
     * Helper to ensure the static fastIntersectionArea matches the instance
     * getIntersectionArea() method.
     */
    private boolean logAreaParity(String label, Region r1, Region r2) {
        double instanceArea = r1.getIntersectionArea(r2);
        float staticArea = Region.fastIntersectionArea(
                (float) r1.getMinLon(), (float) r1.getMaxLon(), (float) r1.getMinLat(), (float) r1.getMaxLat(),
                (float) r2.getMinLon(), (float) r2.getMaxLon(), (float) r2.getMinLat(), (float) r2.getMaxLat());

        return logCheckDouble("Parity " + label, instanceArea, (double) staticArea);
    }

    // --- Helpers ---

    private boolean checkMBREquivalence(String label, Region r1, Region r2) {
        Region clone = new Region(r1.getBottomLeft(), r1.getTopRight());
        clone.expand(r2);
        double expectedArea = clone.getArea();

        float calculatedArea = Region.fastMBRArea(
                (float) r1.getMinLon(), (float) r1.getMaxLon(), (float) r1.getMinLat(), (float) r1.getMaxLat(),
                (float) r2.getMinLon(), (float) r2.getMaxLon(), (float) r2.getMinLat(), (float) r2.getMaxLat());

        boolean match = Math.abs(expectedArea - calculatedArea) < TOLERANCE;
        String status = match ? "PASS" : "FAIL";
        logger.info(String.format("   [%s] %s (MBR Area): Instance=%.4f, Static=%.4f", status, label, expectedArea,
                calculatedArea));

        return match;
    }

    private boolean logCheck(String label, boolean actual, boolean expected) {
        String status = (actual == expected) ? "PASS" : "FAIL";
        logger.info(String.format("   [%s] %s: Got %b", status, label, actual));
        return actual == expected;
    }

    private boolean logCheckDouble(String label, double actual, double expected) {
        boolean match = Math.abs(actual - expected) < TOLERANCE;
        String status = match ? "PASS" : "FAIL";
        logger.info(String.format("   [%s] %s: Expected %.4f, Got %.4f", status, label, expected, actual));
        return match;
    }

    // Wrapper for legacy compatibility with existing test methods
    private boolean assertDouble(String name, double actual, double expected) {
        return logCheckDouble(name, actual, expected);
    }
}