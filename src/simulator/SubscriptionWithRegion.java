package simulator;

public class SubscriptionWithRegion extends SimulationSubscription {
    private Region region;

    public SubscriptionWithRegion(Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        //return "SubscriptionWithRegion [region=" + region + "]";
        return "SubscriptionWithRegion [" + region + "]";
    }

    @Override
    public SubscriptionTableEntry getTableEntry() {
        return new SubscriptionTableEntryWithRegion(this);
    }

    

}
