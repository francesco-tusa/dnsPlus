package simulator;

public class PublicationWithLocation extends SimulationPublication {
    private final Location location;

    public PublicationWithLocation(Location location) {
        this.location = location;
    }

    /**
     * Copy constructor that now also preserves the original source.
     * @param p The publication to copy.
     */
    private PublicationWithLocation(PublicationWithLocation p) {
        // Copy the immediate source
        setSource(p.getSource() != null ? new TreeNode(p.getSource()) : null);
        // **THE FIX**: Preserve the reference to the original source
        setOriginalSource(p.getOriginalSource());
        this.location = new Location(p.location);
    }
    
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        // Now includes the original source for better logging
        String origSourceName = (getOriginalSource() != null) ? getOriginalSource().getName() : "null";
        return "[location=" + location + ", source=" + getSource() + ", originalSource=" + origSourceName + "]";
    }

    @Override
    public SimulationPublication getPublication() {
        return new PublicationWithLocation(this);
    }
}
