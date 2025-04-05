package simulator;

import subscribing.Subscription;

public abstract class SimulationSubscription extends Subscription {
    private TreeNode source;
    private boolean shouldForward;

    public SimulationSubscription() {
        shouldForward = true;
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
