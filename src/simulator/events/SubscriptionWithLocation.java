package simulator.events;

import simulator.core.Location;

/**
 * A subscription that targets a specific point location (for Location-Based Routing).
 */
public class SubscriptionWithLocation extends SimulationSubscription {
    
    private final Location location;

    public SubscriptionWithLocation(Location location) {
        super();
        this.location = location;
    }

    /**
     * Copy constructor for cloning.
     */
    private SubscriptionWithLocation(SubscriptionWithLocation s) {
        super(); 
        this.setSource(s.getSource());
        this.location = new Location(s.location);
        this.metrics = s.metrics; 
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Subscription[location=" + location + ", source=" + (getSource() != null ? getSource().getName() : "null") + "]";
    }
    
    @Override
    public String toDisplayString() {
        if (location != null) {
            return location.toShortString();
        }
        return "N/A";
    }

    @Override
    public SimulationSubscription getSubscription() {
        return new SubscriptionWithLocation(this);
    }
}