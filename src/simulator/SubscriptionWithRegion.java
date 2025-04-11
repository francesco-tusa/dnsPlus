package simulator;

public final class SubscriptionWithRegion extends SimulationSubscription implements Comparable<SubscriptionWithRegion> {
    private Region region;

    public SubscriptionWithRegion(Region region) {
        this.region = region;
    }

    /*
     * Creates a copy of this subscription
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

    @Override
    public int compareTo(SubscriptionWithRegion o) {
        return this.region.compareTo(o.region);
    }
}