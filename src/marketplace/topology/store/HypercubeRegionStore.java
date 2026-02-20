package marketplace.topology.store;

import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;

public class HypercubeRegionStore extends AbstractMarketplaceRegionStore {

    public HypercubeRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    protected SubscriptionWithRegion wrapAggregatedRegion(SubscriptionWithRegion sub, Region accumulator, int mergedCount, int absorbedCount) {
        if (sub instanceof ServiceOffer offer && accumulator instanceof MetricHyperCube mhc) {
            if (mergedCount == 0 && absorbedCount == 0) {
                return ServiceOffer.createWithUpdatedRegion(offer, mhc);
            } else {
                return ServiceOffer.createAggregated(offer.getServiceId(), mhc);
            }
        } else {
            return new SubscriptionWithRegion(accumulator);
        }
    }

    @Override
    protected MergeEvaluation evaluateMerge(Region accumulator, SubscriptionWithRegion existing) {
        if (!(accumulator instanceof MetricHyperCube cubeA) || !(existing.getRegion() instanceof MetricHyperCube cubeB)) {
            boolean result = super.shouldMerge(accumulator, existing);
            return new MergeEvaluation(result, result ? "Merged" : "Threshold Exceeded", 0.0, 0.0); 
        }

        // 1. TIER ISOLATION: Prevent Macro regions from swallowing Micro regions
        MergeEvaluation isolationCheck = checkTierIsolation(accumulator, existing);
        if (isolationCheck != null) {
            return isolationCheck;
        }

        // 2. SPATIAL FPR (Geographic Dead Space)
        double spatialFpr = calculateSpatialFpr(accumulator, existing);

        // 3. QoS FPR (Similarity-Based Capability Difference)
        double qosFpr = calculateQosFpr(cubeA, cubeB);
        
        // 4. FINAL DECISION
        double combinedFpr = Math.max(spatialFpr, qosFpr);
        boolean canMerge = combinedFpr <= this.mergeThreshold;
        
        if (canMerge) {
            return new MergeEvaluation(true, "Merged", spatialFpr, qosFpr);
        } else {
            String bottleneck = (qosFpr > spatialFpr) ? "QoS" : "Spatial";
            double bottleneckVal = Math.max(qosFpr, spatialFpr);
            return new MergeEvaluation(false, String.format("%s FPR %.5f > %.5f", bottleneck, bottleneckVal, this.mergeThreshold), spatialFpr, qosFpr);
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private MergeEvaluation checkTierIsolation(Region accumulator, SubscriptionWithRegion existing) {
        double area1 = Math.max(accumulator.getArea(), 1e-9);
        double area2 = Math.max(existing.getRegion().getArea(), 1e-9);
        double areaRatio = Math.max(area1 / area2, area2 / area1);

        if (areaRatio > 25.0) {
            return new MergeEvaluation(false, String.format("Tier Isolation (Ratio %.1f > 25.0)", areaRatio), 0.0, 0.0); 
        }
        return null; // Passes the check
    }

    private double calculateQosFpr(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        double[] minA = cubeA.getMinValues();
        double[] maxA = cubeA.getMaxValues();
        double[] minB = cubeB.getMinValues();
        double[] maxB = cubeB.getMaxValues();

        double totalRelativeDifference = 0.0;
        int dimensions = minA.length;

        for (int i = 0; i < dimensions; i++) {
            double diffMin = Math.abs(minA[i] - minB[i]);
            double diffMax = Math.abs(maxA[i] - maxB[i]);
            
            // The active difference is whichever bound actually shifted
            double activeDiff = Math.max(diffMin, diffMax);
            
            if (activeDiff > 0) {
                double scale;
                if (diffMin > diffMax) {
                    // Lower-bounded metric (e.g., Latency, Cost) - provider constraints are at the minimum
                    scale = Math.max(Math.abs(minA[i]), Math.abs(minB[i]));
                } else {
                    // Upper-bounded metric (e.g., Reliability, Bandwidth) - provider constraints are at the maximum
                    scale = Math.max(Math.abs(maxA[i]), Math.abs(maxB[i]));
                }
                
                // Calculate percentage difference relative to the maximum bound (the "worse" node)
                totalRelativeDifference += (activeDiff / Math.max(scale, 1e-6));
            }
        }

        // Average the FPR across all dimensions
        return totalRelativeDifference / dimensions;
    }

    @Override
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        return evaluateMerge(accumulator, existing).canMerge;
    }
}