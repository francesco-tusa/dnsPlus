package simulator;

public class SubscriptionWithRegion extends SimulationSubscription {
    private Region region;

    public SubscriptionWithRegion(Region region) {
        this.region = region;
    }

    /*
     * This private constructors is used to create a copy of this subscription
     * to be stored in a broker's subscription table.
     */
    private SubscriptionWithRegion(SubscriptionWithRegion s) {
        setSource(new TreeNode(s.getSource()));
        this.region = new Region(s.getRegion());
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return "SubscriptionWithRegion [" + region + "]";
    }

    @Override
    public SimulationSubscription getTableEntry() {
        return new SubscriptionWithRegion(this);
    }
}
