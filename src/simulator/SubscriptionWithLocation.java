
package simulator;

import subscribing.Subscription;


public class SubscriptionWithLocation extends Subscription {
    private Location location;
    private TreeNode source;
    private boolean shouldForward;

    public SubscriptionWithLocation(Location location) {
        this.location = location;
        shouldForward = true;
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

    public void disableForwarding() {
        shouldForward = false;
    }

    public boolean isForwardingEnabled() {
        return shouldForward;
    }
}
