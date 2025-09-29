package simulator.events;

import java.util.ArrayList;
import java.util.List;

import simulator.core.TreeNode;

/**
 * Represents a generic subscription in the simulation.
 */
public class SimulationSubscription {
    private TreeNode source;
    private List<String> path; // To trace the route for visualisation

    public SimulationSubscription() {
        this.source = null;
        this.path = new ArrayList<>();
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    public List<String> getPath() {
        return path;
    }

    public void addToPath(String nodeName) {
        this.path.add(nodeName);
    }

    /**
     * A method to provide a consistent, short string representation for any subscription type.
     * This will be overridden by subclasses to provide specific details.
     * @return A string formatted for display in the visualiser.
     */
    public String toDisplayString() {
        return ""; // Default implementation returns an empty string.
    }


    /**
     * Creates a shallow copy of the subscription.
     * Subclasses should override to copy their specific fields.
     */
    public SimulationSubscription getSubscription() {
        SimulationSubscription copy = new SimulationSubscription();
        copy.setSource(this.source);
        copy.path = new ArrayList<>(this.path); // Copy the path as well
        return copy;
    }
}
