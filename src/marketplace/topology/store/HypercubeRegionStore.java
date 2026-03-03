package marketplace.topology.store;

import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import marketplace.common.MetricHyperCube;
import marketplace.common.aggregation.AggregationStrategy;

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


    private double calculateMaxFprPenalty(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        double maxFpr = calculateSpatialFpr(cubeA, cubeB);

        double[] lowA = cubeA.getCapabilityMinValues();
        double[] highA = cubeA.getCapabilityMaxValues();
        double[] meanA = cubeA.getQosCenterOfMass();
        double weightA = cubeA.getProviderWeight();

        double[] lowB = cubeB.getCapabilityMinValues();
        double[] highB = cubeB.getCapabilityMaxValues();
        double[] meanB = cubeB.getQosCenterOfMass();
        double weightB = cubeB.getProviderWeight();

        double epsilon = 1e-9;

        AggregationStrategy strategy = marketplace.config.MarketplaceConfig.getAggregationStrategy();

        for (int i = 0; i < lowA.length; i++) {

            double spanA = highA[i] - lowA[i];
            double spanB = highB[i] - lowB[i];

            double minLow = Math.min(lowA[i], lowB[i]);
            double maxHigh = Math.max(highA[i], highB[i]);
            double spanAgg = maxHigh - minLow;

            // --- GATE 1: 1D GAP PENALTY (Strictly your formula) ---
            double gap = Math.max(0.0, Math.max(lowA[i], lowB[i]) - Math.min(highA[i], highB[i]));
            double gapFpr = gap / Math.max(spanAgg, epsilon);
            maxFpr = Math.max(maxFpr, gapFpr);

            // --- GATE 2: MEAN SIMILARITY PENALTY ---

            // 1. Simulate the new Center of Mass using the polymorphic strategy
            double meanAgg = strategy.calculateAggregatedValue(
                    meanA[i], weightA,
                    meanB[i], weightB);

            // 2. Calculate individual shifts
            double shiftA = Math.abs(meanAgg - meanA[i]);
            double shiftB = Math.abs(meanAgg - meanB[i]);

            // 3. Aggregate the shift penalty using the same strategy
            double aggregatedShift = strategy.calculateAggregatedValue(
                    shiftA, weightA,
                    shiftB, weightB);

            // 4. Aggregate the denominator (average span tolerance) using the same strategy
            double aggregatedDenominator = strategy.calculateAggregatedValue(
                    spanA, weightA,
                    spanB, weightB);

            if (aggregatedDenominator < epsilon) {
                aggregatedDenominator = spanAgg;
            }

            double meanShiftFpr = aggregatedShift / Math.max(aggregatedDenominator, epsilon);
            maxFpr = Math.max(maxFpr, meanShiftFpr);
        }

        return maxFpr;
    }

    /**
     * Calculates the maximum False Positive Rate (FPR) across all dimensions,
     * unifying the 2D Spatial Substrate with the N-Dimensional QoS Capability Space.
    
    private double calculateMaxFprPenalty(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        
        // 1. SPATIAL FPR (2D DNS++ Substrate Logic)
        double maxFpr = calculateSpatialFpr(cubeA, cubeB);

        // 2. QoS DIMENSIONAL FPR (N-Dimensional Hyper-Volume Logic)
        double[] capMinA = cubeA.getCapabilityMinValues();
        double[] capMaxA = cubeA.getCapabilityMaxValues();
        double[] capMinB = cubeB.getCapabilityMinValues();
        double[] capMaxB = cubeB.getCapabilityMaxValues();

        double volA = 1.0;
        double volB = 1.0;
        double volInter = 1.0;
        double volMerged = 1.0;

        for (int i = 0; i < capMinA.length; i++) {
            // Use a minimal epsilon width (1e-4) to prevent zero-volume collapse 
            // for exact-point capabilities (e.g., fixed cost values).
            double sizeA = Math.max(1e-4, capMaxA[i] - capMinA[i]);
            double sizeB = Math.max(1e-4, capMaxB[i] - capMinB[i]);
            
            double combinedMin = Math.min(capMinA[i], capMinB[i]);
            double combinedMax = Math.max(capMaxA[i], capMaxB[i]);
            double mergedSize = Math.max(1e-4, combinedMax - combinedMin);
            
            double interMin = Math.max(capMinA[i], capMinB[i]);
            double interMax = Math.min(capMaxA[i], capMaxB[i]);
            // If intervals do not overlap in this dimension, intersection size is 0
            double interSize = interMax > interMin ? (interMax - interMin) : 0.0;
            
            volA *= sizeA;
            volB *= sizeB;
            volMerged *= mergedSize;
            volInter *= interSize;
        }

        double qosFpr = 0.0;
        if (volMerged > 1e-9) {
            // Geometric Union = Area A + Area B - Intersection Area
            double unionVol = volA + volB - volInter;
            
            // Dead Space = Bounding Box Volume - Union Volume
            double deadSpace = Math.max(0.0, volMerged - unionVol);
            
            qosFpr = deadSpace / volMerged;
        }

        // Return the strictest penalty (Spatial OR QoS)
        return Math.max(maxFpr, qosFpr);
    }
    */

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