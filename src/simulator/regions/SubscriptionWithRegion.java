package simulator.regions;

import simulator.events.SimulationSubscription;

public class SubscriptionWithRegion extends SimulationSubscription {

    private float minLon, maxLon, minLat, maxLat;

    public SubscriptionWithRegion(Region region) {
        super();
        setRegion(region);
    }

    public SubscriptionWithRegion(SubscriptionWithRegion other) {
        super();
        this.minLon = other.minLon;
        this.maxLon = other.maxLon;
        this.minLat = other.minLat;
        this.maxLat = other.maxLat;
        super.metrics = other.metrics;
    }

    // --- Primitive Getters ---
    public float getMinLon() {
        return minLon;
    }

    public float getMaxLon() {
        return maxLon;
    }

    public float getMinLat() {
        return minLat;
    }

    public float getMaxLat() {
        return maxLat;
    }

    // --- Delegate Methods (Spatial Predicates) ---

    public boolean contains(SubscriptionWithRegion other) {
        return Region.fastContains(minLon, maxLon, minLat, maxLat,
                other.minLon, other.maxLon, other.minLat, other.maxLat);
    }

    public boolean contains(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        return Region.fastContains(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public boolean isContainedIn(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        return Region.fastContains(oMinLon, oMaxLon, oMinLat, oMaxLat,
                this.minLon, this.maxLon, this.minLat, this.maxLat);
    }

    public boolean intersects(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        return Region.fastIntersects(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public float getIntersectionArea(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        return Region.fastIntersectionArea(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public float getArea() {
        return Region.fastArea(minLon, maxLon, minLat, maxLat);
    }

    // --- Existing Lifecycle Methods ---
    @Override
    public SimulationSubscription getSubscription() {
        return new SubscriptionWithRegion(this);
    }

    public Region getRegion() {
        return new Region(minLon, minLat, maxLon, maxLat);
    }

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
        return String.format("[%.4f,%.4f:%.4f,%.4f]", minLon, minLat, maxLon, maxLat);
    }
}