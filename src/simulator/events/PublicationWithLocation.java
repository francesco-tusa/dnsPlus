package simulator.events;

import simulator.core.Location;

public class PublicationWithLocation extends SimulationPublication {
    private final Location location;

    public PublicationWithLocation(Location location) {
        this.location = location;
    }

    private PublicationWithLocation(PublicationWithLocation p) {
        setSource(p.getSource());
        this.location = new Location(p.location);
        copyStateFrom(p); // <--- The Critical Fix
    }
    
    public Location getLocation() { return location; }

    @Override
    public String toString() {
        return "[location=" + location + ", source=" + getSource() + "]";
    }
    
    public String toDisplayString() {
        return (location != null) ? location.toShortString() : "N/A_Location";
    }

    @Override
    public SimulationPublication getPublication() {
        return new PublicationWithLocation(this);
    }
}