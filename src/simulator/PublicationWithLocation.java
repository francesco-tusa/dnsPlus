package simulator;

public class PublicationWithLocation extends SimulationPublication {
    private final Location location;

    public PublicationWithLocation(Location location) {
        this.location = location;
    }

    private PublicationWithLocation(PublicationWithLocation p) {
        setSource(new TreeNode(p.getSource()));
        this.location = new Location(p.location);
    }
    
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "[location=" + location + ", source=" + getSource() + "]";
    }

    @Override
    public SimulationPublication getPublication() {
        return new PublicationWithLocation(this);
    }
}
