package simulator.regions.store;

import simulator.regions.SubscriptionWithRegion;

public class StoreUpdate {
    private final StoreOpResult result;
    private final SubscriptionWithRegion region;
    private final String additionalInfo;
    
    private final int absorbedCount;
    private final int mergedCount;
    
    // NEW: Track the existing routing table entry we interacted with
    private final SubscriptionWithRegion existingTarget;

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo) {
        this(result, region, additionalInfo, 0, 0, null);
    }

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo, int absorbedCount, int mergedCount) {
        this(result, region, additionalInfo, absorbedCount, mergedCount, null);
    }

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo, int absorbedCount, int mergedCount, SubscriptionWithRegion existingTarget) {
        this.result = result;
        this.region = region;
        this.additionalInfo = additionalInfo;
        this.absorbedCount = absorbedCount;
        this.mergedCount = mergedCount;
        this.existingTarget = existingTarget;
    }

    public StoreOpResult getResult() {
        return result;
    }

    public SubscriptionWithRegion getRegion() {
        return region;
    }
    
    public String getAdditionalInfo() {
        return additionalInfo;
    }
    
    public int getAbsorbedCount() {
        return absorbedCount;
    }

    public int getMergedCount() {
        return mergedCount;
    }

    public SubscriptionWithRegion getExistingTarget() { 
        return existingTarget; 
    }

    public boolean isChange() {
        return result != StoreOpResult.NO_CHANGE;
    }
}