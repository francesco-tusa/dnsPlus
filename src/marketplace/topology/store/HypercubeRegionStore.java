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
            // ADDED: 0.0, 0.0
            return new MergeEvaluation(result, result ? "Merged" : "Threshold Exceeded", 0.0, 0.0); 
        }

        // 2. TIER ISOLATION: Prevent Macro regions from swallowing Micro regions
        double area1 = Math.max(accumulator.getArea(), 1e-9);
        double area2 = Math.max(existing.getRegion().getArea(), 1e-9);
        double areaRatio = Math.max(area1 / area2, area2 / area1);

        if (areaRatio > 25.0) {
            // ADDED: 0.0, 0.0
            return new MergeEvaluation(false, String.format("Tier Isolation (Ratio %.1f > 25.0)", areaRatio), 0.0, 0.0); 
        }

        // 3. SPATIAL FPR 
        double spatialFpr = calculateSpatialFpr(accumulator, existing);

        // 4. QoS FPR (1D overlap math)
        double[] minA = cubeA.getMinValues();
        double[] maxA = cubeA.getMaxValues();
        double[] minB = cubeB.getMinValues();
        double[] maxB = cubeB.getMaxValues();

        double totalRelativeExpansion = 0.0;
        int dimensions = minA.length;

        for (int i = 0; i < dimensions; i++) {
            double currentRange = maxA[i] - minA[i];
            double newMin = Math.min(minA[i], minB[i]);
            double newMax = Math.max(maxA[i], maxB[i]);
            double mergedRange = newMax - newMin;
            
            double expansion = mergedRange - currentRange;
            
            if (expansion > 0) {
                double scale = Math.max(Math.abs(newMax), 1e-6); 
                totalRelativeExpansion += (expansion / scale);
            }
        }

        double qosFpr = totalRelativeExpansion / dimensions;
        
        // 5. FINAL DECISION
        double combinedFpr = Math.max(spatialFpr, qosFpr);
        boolean canMerge = combinedFpr <= this.mergeThreshold;
        
        if (canMerge) {
            // UPDATED: Pass "Merged" as the reason, and append the raw doubles for the logger to use later
            return new MergeEvaluation(true, "Merged", spatialFpr, qosFpr);
        } else {
            String bottleneck = (qosFpr > spatialFpr) ? "QoS" : "Spatial";
            double bottleneckVal = Math.max(qosFpr, spatialFpr);
            // UPDATED: Boosted to %.5f and appended the raw doubles
            return new MergeEvaluation(false, String.format("%s FPR %.5f > %.5f", bottleneck, bottleneckVal, this.mergeThreshold), spatialFpr, qosFpr);
        }
    }

    @Override
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        return evaluateMerge(accumulator, existing).canMerge;
    }
}