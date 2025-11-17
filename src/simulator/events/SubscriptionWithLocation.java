package simulator.events;

import java.util.List;

import simulator.core.Location;

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

    /**
     * Overrides the base method to create a shallow copy of this specific subscription type,
     * preserving the location data and ensuring the object's type is not lost during propagation.
     */
    @Override
    public SimulationSubscription getSubscription() {
        SubscriptionWithLocation copy = new SubscriptionWithLocation(this.location);
        copy.setSource(this.getSource());
        copy.metrics = this.metrics;

        return copy;
    }

    @Override
    public String toDisplayString() {
        return location.toShortString();
    }

    @Override
    public String toString() {
        return "location=" + location;
    }
}