package simulator.regions;

import simulator.events.SimulationSubscription;

/**
 * Represents a subscription to a geographical region.
 */
public class SubscriptionWithRegion extends SimulationSubscription {

    private final Region region;

    public SubscriptionWithRegion(Region region) {
        super();
        if (region == null) {
            throw new IllegalArgumentException("Region cannot be null for a SubscriptionWithRegion.");
        }
        this.region = region;
    }

    /**
     * Copy constructor.
     */
    public SubscriptionWithRegion(SubscriptionWithRegion s) {
        super();
        this.region = new Region(s.getRegion());
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public SimulationSubscription getSubscription() {
        SubscriptionWithRegion copy = new SubscriptionWithRegion(this.region);
        copy.setSource(this.getSource());
        for (String node : this.getPath()) {
            copy.addToPath(node);
        }
        return copy;
    }

    @Override
    public String toDisplayString() {
        return region.toShortString();
    }

    @Override
    public String toString() {
        return "region=" + region;
    }
}
