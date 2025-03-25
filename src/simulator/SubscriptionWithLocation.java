
package simulator;

import subscribing.Subscription;


public class SubscriptionWithLocation extends Subscription {
    private Location location;
    private TreeNode source;

    public SubscriptionWithLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }
}
