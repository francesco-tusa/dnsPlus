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
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {

        if (!(accumulator instanceof MetricHyperCube cubeA) || !(existing.getRegion() instanceof MetricHyperCube cubeB)) {
            return super.shouldMerge(accumulator, existing);
        }

        // 2. TIER ISOLATION: Prevent Macro regions from swallowing Micro regions
        double area1 = Math.max(accumulator.getArea(), 1e-9);
        double area2 = Math.max(existing.getRegion().getArea(), 1e-9);
        double areaRatio = Math.max(area1 / area2, area2 / area1);

        // Protect cross-tier merging (matching the 25.0 factor we placed in the Abstract subsumption logic)
        if (areaRatio > 25.0) {
            return false; 
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
        return combinedFpr <= this.mergeThreshold;
    }
}