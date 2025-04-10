package simulator;

public class SubscriptionWithLocation extends SimulationSubscription {
    private Location location;

    public SubscriptionWithLocation(Location location) {
        this.location = location;
    }

    private SubscriptionWithLocation(SubscriptionWithLocation s) {
        setSource(new TreeNode(s.getSource()));
        this.location = new Location(s.getLocation());
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "SubscriptionWithLocation [" + location + "]";
    }

    @Override
    public SimulationSubscription getTableEntry() {
        return new SubscriptionWithLocation(this);
    }
}
