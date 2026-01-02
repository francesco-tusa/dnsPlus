package simulator.regions;

import simulator.events.SimulationSubscription;

/**
 * Memory-Optimized Subscription implementation.
 * Instead of storing a reference to a 'Region' object (which holds references to two 'Location' objects),
 * we store the 4 coordinates as primitive floats.
 */
public class SubscriptionWithRegion extends SimulationSubscription {

    // Store coordinates directly as primitives
    private float minLon;
    private float maxLon;
    private float minLat;
    private float maxLat;

    public SubscriptionWithRegion(Region region) {
        super();
        setRegion(region);
    }

    /**
     * Copy constructor.
     */
    public SubscriptionWithRegion(SubscriptionWithRegion other) {
        super();
        this.minLon = other.minLon;
        this.maxLon = other.maxLon;
        this.minLat = other.minLat;
        this.maxLat = other.maxLat;
        // Share metrics (reference copy)
        super.metrics = other.metrics;
    }

    @Override
    public SimulationSubscription getSubscription() {
        return new SubscriptionWithRegion(this);
    }

    /**
     * Reconstructs the Region object on-the-fly (Ephemeral).
     * The GC collects this extremely quickly (Young Generation), 
     * causing minimal performance impact compared to the memory savings.
     */
    public Region getRegion() {
        // Uses the new constructor added to Region.java
        return new Region(minLon, minLat, maxLon, maxLat);
    }

    /**
     * Updates the internal primitive coordinates from a Region object.
     * This acts as the "Commit" phase for any changes.
     */
    public void setRegion(Region region) {
        if (region != null && region.getBottomLeft() != null && region.getTopRight() != null) {
            this.minLon = (float) region.getBottomLeft().getX();
            this.minLat = (float) region.getBottomLeft().getY();
            this.maxLon = (float) region.getTopRight().getX();
            this.maxLat = (float) region.getTopRight().getY();
        }
    }

    @Override
    public String toDisplayString() {
        // Formats directly from primitives to avoid object creation during logging
        return String.format("[%.4f,%.4f:%.4f,%.4f]", minLon, minLat, maxLon, maxLat);
    }
}