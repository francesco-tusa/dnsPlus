package simulator;

/**
 * Represents a subscription to a specific geographical location.
 */
public class SubscriptionWithLocation extends SimulationSubscription {

    private final Location location;

    public SubscriptionWithLocation(Location location) {
        super();
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null for a SubscriptionWithLocation.");
        }
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public SimulationSubscription getSubscription() {
        SubscriptionWithLocation copy = new SubscriptionWithLocation(this.location);
        copy.setSource(this.getSource());
        return copy;
    }

    @Override
    public String toString() {
        return "location=" + location;
    }
}
