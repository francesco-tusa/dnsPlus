package simulator.regions.store;

import simulator.regions.SubscriptionWithRegion;

public class StoreUpdate {
    private final StoreOpResult result;
    private final SubscriptionWithRegion region;

    public StoreUpdate(StoreOpResult result, SubscriptionWithRegion region) {
        this.result = result;
        this.region = region;
    }

    public StoreOpResult getResult() {
        return result;
    }

    public SubscriptionWithRegion getRegion() {
        return region;
    }

    public boolean isChange() {
        return result != StoreOpResult.NO_CHANGE;
    }
}