package simulator;

import publishing.Publication;

public class SimulationPublication extends Publication {
    private final Location location;
    private TreeNode source;

    public SimulationPublication(Location location) {
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