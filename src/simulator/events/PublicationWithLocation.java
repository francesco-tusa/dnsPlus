package simulator.events;

import simulator.core.Location;


public class PublicationWithLocation extends SimulationPublication {
    private final Location location;

    /**
     * Public constructor for creating the original event.
     */
    public PublicationWithLocation(Location location) {
        this.location = location;
    }

    /**
     * Copy constructor that preserves the original source and hop count.
     * @param p The publication to copy.
     */
    private PublicationWithLocation(PublicationWithLocation p) {
        setSource(p.getSource());
        this.location = new Location(p.location);
        this.metrics = p.metrics;
    }
    
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        // Now includes the original source for better logging
        return "[location=" + location + ", source=" + getSource() + "]";
    }
    
    public String toDisplayString() {
        if (location != null) {
            return location.toShortString();
        }
        return "N/A_Location";
    }


    /**
     * Prototype Pattern implementation.
     * Allows the broker to clone this publication without knowing its concrete class.
     */
    @Override
    public SimulationPublication getPublication() {
        return new PublicationWithLocation(this);
    }
}