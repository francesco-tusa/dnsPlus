package simulator;

/**
 * Represents a generic subscription in the simulation.
 */
public class SimulationSubscription {
    private TreeNode source;
    private TreeNode originalSource; // Added to track the true origin

    public SimulationSubscription() {
        this.source = null;
        this.originalSource = null;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    public TreeNode getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(TreeNode originalSource) {
        this.originalSource = originalSource;
    }

    /**
     * Creates a shallow copy of the subscription.
     * Subclasses should override to copy their specific fields.
     */
    public SimulationSubscription getSubscription() {
        SimulationSubscription copy = new SimulationSubscription();
        copy.setSource(this.source);
        copy.setOriginalSource(this.originalSource);
        return copy;
    }
}
