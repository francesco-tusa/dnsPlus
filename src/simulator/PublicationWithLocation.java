package simulator;

import publishing.Publication;

public class PublicationWithLocation extends Publication {
    Location location;
    TreeNode source;

    public PublicationWithLocation(Location location) {
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