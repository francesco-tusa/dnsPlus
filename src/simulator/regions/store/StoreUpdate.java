package simulator.regions.store;

import simulator.regions.SubscriptionWithRegion;

public class StoreUpdate {
    private final StoreOpResult result;
    private final SubscriptionWithRegion region;
    private final String additionalInfo;

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region, String additionalInfo) {
        this.result = result;
        this.region = region;
        this.additionalInfo = additionalInfo;
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

    public boolean isChange() {
        return result != StoreOpResult.NO_CHANGE;
    }
}