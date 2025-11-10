package simulator.regions;

import simulator.events.SimulationSubscription;

public class SubscriptionWithRegion extends SimulationSubscription {

    private Region region;

    public SubscriptionWithRegion(Region region) {
        super();
        this.region = region;
    }

    /**
     * Copy constructor (deep copies the region but shares the metrics).
     */
    public SubscriptionWithRegion(SubscriptionWithRegion other) {
        super();
        this.region = new Region(other.region);
        super.metrics = other.metrics;
    }

    /**
     * Creates a deep copy of this subscription, but shares the
     * underlying EventMetrics object for tracking hops and path.
     */
    @Override
    public SimulationSubscription getSubscription() {
        // This now calls the modified copy constructor above
        return new SubscriptionWithRegion(this);
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public String toDisplayString() {
        if (region != null) {
            return region.toShortString();
        }
        return "N/A";
    }
}