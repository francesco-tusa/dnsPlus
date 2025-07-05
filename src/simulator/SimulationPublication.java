package simulator;

import publishing.Publication;

public abstract class SimulationPublication extends Publication {
    private TreeNode source; // The immediate sender of this message copy
    private TreeNode originalSource; // The publisher who created the message

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }

    /**
     * Gets the original publisher of this publication.
     * @return The TreeNode of the original publisher.
     */
    public TreeNode getOriginalSource() {
        return originalSource;
    }

    /**
     * Sets the original publisher of this publication. This should typically
     * only be called once when the publication is created.
     * @param originalSource The TreeNode of the original publisher.
     */
    public void setOriginalSource(TreeNode originalSource) {
        this.originalSource = originalSource;
    }

    public abstract SimulationPublication getPublication();
}