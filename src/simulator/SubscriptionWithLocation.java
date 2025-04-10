package simulator;

public class SubscriptionWithLocation extends SimulationSubscription {
    private Location location;

    public SubscriptionWithLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "SubscriptionWithLocation [location=" + location + "]";
    }

    @Override
    public SubscriptionTableEntry getTableEntry() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTableEntry'");
    }
}
