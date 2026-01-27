package simulator.regions.store;

import simulator.regions.SubscriptionWithRegion;

public class StoreUpdate {
    private final StoreOpResult result;
    private final SubscriptionWithRegion region;
    private final String additionalInfo;
    
    // New granular metrics
    private final int absorbedCount;
    private final int mergedCount;

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo) {
        this(result, region, additionalInfo, 0, 0);
    }

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo, int absorbedCount, int mergedCount) {
        this.result = result;
        this.region = region;
        this.additionalInfo = additionalInfo;
        this.absorbedCount = absorbedCount;
        this.mergedCount = mergedCount;
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

    public boolean isChange() {
        return result != StoreOpResult.NO_CHANGE;
    }
}