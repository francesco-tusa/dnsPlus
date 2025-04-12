package simulator;

import publishing.Publication;

public abstract class SimulationPublication extends Publication {
    private TreeNode source;

    

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    public abstract SimulationPublication getPublication();
}