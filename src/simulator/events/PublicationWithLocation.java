package simulator.events;

import simulator.core.Location;
import simulator.core.TreeNode;

public class PublicationWithLocation extends SimulationPublication {
    private final Location location;

    public PublicationWithLocation(Location location) {
        this.location = location;
    }

    /**
     * Copy constructor that preserves the original source.
     * @param p The publication to copy.
     */
    private PublicationWithLocation(PublicationWithLocation p) {
        // Copy the immediate source
        setSource(p.getSource() != null ? new TreeNode(p.getSource()) : null);
        this.location = new Location(p.location);
    }
    
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        // Now includes the original source for better logging
        return "[location=" + location + ", source=" + getSource() + "]";
    }

    @Override
    public SimulationPublication getPublication() {
        return new PublicationWithLocation(this);
    }
}
