package marketplace.topology.store;

import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import marketplace.common.MetricHyperCube;

public class HypercubeRegionStore extends AbstractMarketplaceRegionStore {

    public HypercubeRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    protected MergeEvaluation evaluateMerge(Region accumulator, SubscriptionWithRegion existing) {
        if (!(accumulator instanceof MetricHyperCube cubeA) || !(existing.getRegion() instanceof MetricHyperCube cubeB)) {
            boolean result = super.shouldMerge(accumulator, existing);
            return new MergeEvaluation(result, result ? "Merged" : "Threshold Exceeded", 0.0); 
        }

        double maxFpr = calculateMaxFprPenalty(cubeA, cubeB);
        boolean canMerge = maxFpr <= this.mergeThreshold;
        
        if (canMerge) {
            return new MergeEvaluation(true, "Merged", maxFpr);
        } else {
            return new MergeEvaluation(false, String.format("Max FPR %.2f > %.2f", maxFpr, this.mergeThreshold), maxFpr);
        }
    }

    /**
     * Calculates the maximum False Positive Rate (FPR) across all dimensions,
     * unifying the 2D Spatial Substrate with the N-Dimensional QoS Capability Space.
     * Evaluates the worst-case penalty using the Chebyshev Distance (L-infinity norm).
     */
    private double calculateMaxFprPenalty(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        
        // 1. SPATIAL FPR (2D DNS++ Substrate Logic)
        // Initialize the L-infinity tracker with the Spatial penalty
        double maxFpr = calculateSpatialFpr(cubeA, cubeB);

        // 2. QoS DIMENSIONAL FPR (1D Tri-State Capability Logic)
        double[] capMinA = cubeA.getCapabilityMinValues();
        double[] capMaxA = cubeA.getCapabilityMaxValues();
        double[] capMinB = cubeB.getCapabilityMinValues();
        double[] capMaxB = cubeB.getCapabilityMaxValues();

        for (int i = 0; i < capMinA.length; i++) {
            double combinedMin = Math.min(capMinA[i], capMinB[i]);
            double combinedMax = Math.max(capMaxA[i], capMaxB[i]);
            double totalCapabilityRange = combinedMax - combinedMin;

            if (totalCapabilityRange > 1e-9) {
                // 1D Gap: Empty space between the intervals. Overlaps yield <= 0.
                double gap = Math.max(0.0, Math.max(capMinA[i], capMinB[i]) - Math.min(capMaxA[i], capMaxB[i]));
                
                // 1D FPR: The percentage of the new capability interval that is "dead space"
                double dimensionFpr = gap / totalCapabilityRange;
                
                // L-infinity norm: Track the worst-case dimension
                maxFpr = Math.max(maxFpr, dimensionFpr);
            }
        }

        return maxFpr;
    }

    /**
     * Helper method to evaluate the geometric 2D False Positive Rate.
     * Reuses the established DNS++ substrate spherical intersection math 
     * which correctly handles longitude wrapping.
     */
    private double calculateSpatialFpr(Region cubeA, Region cubeB) {
        // Polymorphic calls: These route to the highly optimized fastArea/fastMBRArea 
        // methods in the base Region class.
        double areaA = cubeA.getArea();
        double areaB = cubeB.getArea();
        double mergedArea = cubeA.getMergedArea(cubeB);
        
        // If the merged area is virtually zero (perfect overlap of points), there is no dead space.
        if (mergedArea <= 1e-9) {
            return 0.0; 
        }

        double intersectionArea = cubeA.getIntersectionArea(cubeB);
        double unionArea = areaA + areaB - intersectionArea;
        
        // Spatial FPR: The percentage of the new bounding box that contains no nodes
        double spatialDeadSpace = Math.max(0.0, mergedArea - unionArea);
        return spatialDeadSpace / mergedArea;
    }

    @Override
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        return evaluateMerge(accumulator, existing).canMerge;
    }
}