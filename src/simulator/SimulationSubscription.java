package simulator;

import subscribing.Subscription;

public abstract class SimulationSubscription extends Subscription {
    private TreeNode source;
    private boolean shouldForwardUpwards;

    public SimulationSubscription() {
        shouldForwardUpwards = true;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    public void disableUpwardsForwarding() {
        shouldForwardUpwards = false;
    }

    public boolean isUpwardsForwardingEnabled() {
        return shouldForwardUpwards;
    }

    public abstract SubscriptionTableEntry getTableEntry();
}
