package simulator.events;

import simulator.core.Location;

public class PublicationWithLocation extends SimulationPublication {
    
    private Location location;
    private transient double cachedDistanceSquared = -1.0;

    public PublicationWithLocation(Location location) {
        super();
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
    
    public double getCachedDistanceSquared() {
        return cachedDistanceSquared;
    }

    public void setCachedDistanceSquared(double cachedDistanceSquared) {
        this.cachedDistanceSquared = cachedDistanceSquared;
    }

    @Override
    public SimulationPublication getPublication() {
        PublicationWithLocation newPub = new PublicationWithLocation(this.location);
        newPub.copyStateFrom(this);
        return newPub;
    }

    @Override
    public String toString() {
        return "PublicationWithLocation [location=" + location + ", id=" + getId() + "]";
    }
}