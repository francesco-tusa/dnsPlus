package simulator;

/**
 * Represents a generic subscription in the simulation.
 */
public class SimulationSubscription {
    private TreeNode source;

    public SimulationSubscription() {
        this.source = null;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    /**
     * Creates a shallow copy of the subscription.
     * Subclasses should override to copy their specific fields.
     */
    public SimulationSubscription getSubscription() {
        SimulationSubscription copy = new SimulationSubscription();
        copy.setSource(this.source);
        return copy;
    }
}
