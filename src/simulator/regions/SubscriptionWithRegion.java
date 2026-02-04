package simulator.regions;

import simulator.events.SimulationSubscription;

public class SubscriptionWithRegion extends SimulationSubscription {

    private float minLon, maxLon, minLat, maxLat;

    private Region complexRegion;

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

        // POLYMORPHIC COPY: Preserves MetricHyperCube if present
        if (other.complexRegion != null) {
            this.complexRegion = other.complexRegion.copy();
        }

        if (other.metrics != null)
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
        if (complexRegion != null) {
            return complexRegion.contains(other.getRegion());
        }
        return Region.fastContains(minLon, maxLon, minLat, maxLat,
                other.minLon, other.maxLon, other.minLat, other.maxLat);
    }

    public boolean contains(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        if (complexRegion != null) {
            return complexRegion.contains(new Region(oMinLon, oMinLat, oMaxLon, oMaxLat));
        }
        return Region.fastContains(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public boolean isContainedIn(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        if (complexRegion != null) {
            return new Region(oMinLon, oMinLat, oMaxLon, oMaxLat).contains(complexRegion);
        }
        return Region.fastContains(oMinLon, oMaxLon, oMinLat, oMaxLat,
                this.minLon, this.maxLon, this.minLat, this.maxLat);
    }

    public boolean intersects(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        if (complexRegion != null) {
            return complexRegion.intersects(new Region(oMinLon, oMinLat, oMaxLon, oMaxLat));
        }
        return Region.fastIntersects(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public float getIntersectionArea(float oMinLon, float oMaxLon, float oMinLat, float oMaxLat) {
        if (complexRegion != null) {
            return (float) complexRegion.getIntersectionArea(new Region(oMinLon, oMinLat, oMaxLon, oMaxLat));
        }
        return Region.fastIntersectionArea(minLon, maxLon, minLat, maxLat,
                oMinLon, oMaxLon, oMinLat, oMaxLat);
    }

    public float getArea() {
        if (complexRegion != null)
            return (float) complexRegion.getArea();
        return Region.fastArea(minLon, maxLon, minLat, maxLat);
    }

    // --- Existing Lifecycle Methods ---
    @Override
    public SimulationSubscription getSubscription() {
        return new SubscriptionWithRegion(this);
    }

    public Region getRegion() {
        if (complexRegion != null) {
            return complexRegion; // Returns the live object (mutable)
        }
        return new Region(minLon, minLat, maxLon, maxLat);
    }

    public void setRegion(Region region) {
        if (region != null) {
            this.complexRegion = region;
            if (region.getBottomLeft() != null && region.getTopRight() != null) {
                this.minLon = (float) region.getBottomLeft().getX();
                this.minLat = (float) region.getBottomLeft().getY();
                this.maxLon = (float) region.getTopRight().getX();
                this.maxLat = (float) region.getTopRight().getY();
            }
        }
    }

    @Override
    public String toDisplayString() {
        if (complexRegion != null)
            return complexRegion.toShortString();
        return String.format("[%.4f,%.4f:%.4f,%.4f]", minLon, minLat, maxLon, maxLat);
    }
}