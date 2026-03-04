package simulator.regions.store;

import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * Base class for Multi-Region stores. 
 * Handles configuration (thresholds) and common reporting logic.
 */
public abstract class AbstractMultiRegionStore implements RegionSubscriptionStore {

    protected final double mergeThreshold;

    protected AbstractMultiRegionStore(double threshold) {
        this.mergeThreshold = threshold;
    }

    // Common logic for both implementations to determine the result format
    protected StoreOpResult determineResult(int absorbed, int merged, boolean isReplacement) {
        if (absorbed == 0 && merged == 0) return StoreOpResult.ADDED;
        if (isReplacement) return StoreOpResult.NO_CHANGE;
        return StoreOpResult.EXPANDED;
    }

    // Common logic for logging explanations
    protected String createCleanExplanation(StoreOpResult result, int merged, int absorbed, boolean isReplacement) {
        switch (result) {
            case ADDED: return "New";
            case NO_CHANGE: return isReplacement ? String.format("Internal Structure Change: Absorbed %d", absorbed) : "Filtered";
            case EXPANDED: return merged > 0 ? String.format("Merged %d, Absorbed %d", merged, absorbed) : String.format("Absorbed %d", absorbed);
            default: return "Unknown State";
        }
    }

    protected boolean shouldMerge(Region acc, SubscriptionWithRegion existing) {
        if (mergeThreshold <= 0.0) return false;

        Region extRegion = existing.getRegion();

        double area1 = acc.getArea();
        double area2 = extRegion.getArea();
        double interArea = acc.getIntersectionArea(extRegion);
        double mergedArea = acc.getMergedArea(extRegion);

        if (mergedArea <= 0.0) return true; // Perfect overlap

        double geometricUnion = area1 + area2 - interArea;
        double fpr = (mergedArea - geometricUnion) / mergedArea;

        return fpr < mergeThreshold;
    }
}