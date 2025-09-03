package simulator.events;

import publishing.Publication;
import simulator.core.TreeNode;

import java.util.ArrayList;
import java.util.List;

public abstract class SimulationPublication extends Publication {
    private TreeNode source; // The immediate sender of this message copy
    private List<String> path; // To trace the route for visualisation

    public SimulationPublication() {
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

    public abstract SimulationPublication getPublication();
}
