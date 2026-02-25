package marketplace.topology.store;

import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import marketplace.common.MetricHyperCube;
import marketplace.common.MarketplaceMetricSchema;
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
            return new MergeEvaluation(result, result ? "Merged" : "Threshold Exceeded", 0.0); 
        }

        double maxSlaProjectedPenalty = calculateSlaProjectedDilation(cubeA, cubeB);
        boolean canMerge = maxSlaProjectedPenalty <= this.mergeThreshold;
        
        if (canMerge) {
            return new MergeEvaluation(true, "Merged", maxSlaProjectedPenalty);
        } else {
            return new MergeEvaluation(false, String.format("SLA-Projected Dilation %.5f > %.5f", maxSlaProjectedPenalty, this.mergeThreshold), maxSlaProjectedPenalty);
        }
    }

    /**
     * Calculates the aggregation penalty by linking absolute SLA error with 
     * the geographic spatial dilation of the node providing the better capability.
     * Evaluates across all dimensions using the Chebyshev Distance (L-infinity norm).
     */
    private double calculateSlaProjectedDilation(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        double areaA = Math.max(cubeA.getArea(), 1e-9);
        double areaB = Math.max(cubeB.getArea(), 1e-9);
        
        // 1. Calculate the physical area of the hypothetical merged bounding box
        double minX = Math.min(cubeA.getBottomLeft().getX(), cubeB.getBottomLeft().getX());
        double minY = Math.min(cubeA.getBottomLeft().getY(), cubeB.getBottomLeft().getY());
        double maxX = Math.max(cubeA.getTopRight().getX(), cubeB.getTopRight().getX());
        double maxY = Math.max(cubeA.getTopRight().getY(), cubeB.getTopRight().getY());
        double areaMerged = Math.max((maxX - minX) * (maxY - minY), 1e-9);

        // Track the worst-case projected penalty across all dimensions (L-infinity norm)
        double maxPenalty = 0.0;
        
        double[] minA = cubeA.getMinValues();
        double[] maxA = cubeA.getMaxValues();
        double[] minB = cubeB.getMinValues();
        double[] maxB = cubeB.getMaxValues();

        for (int i = 0; i < minA.length; i++) {
            String dimKey = MarketplaceMetricSchema.KEYS[i];
            
            // Step A: Calculate Absolute Shift
            double diffMin = Math.abs(minA[i] - minB[i]);
            double diffMax = Math.abs(maxA[i] - maxB[i]);
            double activeDiff = Math.max(diffMin, diffMax);

            if (activeDiff > 1e-9) {
                // Step B: Normalize against Global System Max (Base SLA Error)
                double systemMax = MarketplaceMetricSchema.getSystemMax(dimKey);
                double baseSlaError = activeDiff / Math.max(systemMax, 1e-9);

                // Step C: Identify the area of the node that provided the strictly "better" bound
                double bestNodeArea;
                if (diffMin > diffMax) {
                    bestNodeArea = (minA[i] < minB[i]) ? areaA : areaB;
                } else {
                    bestNodeArea = (maxA[i] > maxB[i]) ? areaA : areaB;
                }

                // Step D: Calculate Spatial Dilation multiplier
                double spatialDilation = areaMerged / bestNodeArea;
                
                // Step E: Projected Penalty
                double projectedPenalty = baseSlaError * spatialDilation;
                maxPenalty = Math.max(maxPenalty, projectedPenalty);
            }
        }

        // Geometric Fallback: Also check pure geographic dilation just in case QoS is identical 
        // but the nodes are physically extremely far apart.
        double pureSpatialDilation = Math.max(0.0, (areaMerged / (areaA + areaB)) - 1.0);
        
        return Math.max(maxPenalty, pureSpatialDilation);
    }

    @Override
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        return evaluateMerge(accumulator, existing).canMerge;
    }
}