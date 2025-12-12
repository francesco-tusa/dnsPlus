package simulator.regions;

import java.util.Objects;
import simulator.core.Location;

/**
 * Abstract base class holding shared state for SpatialRegions.
 * Contains NO geometric logic to avoid assumptions about coordinate systems.
 */
public abstract class AbstractRegion implements SpatialRegion {

    protected Location bottomLeft;
    protected Location topRight;

    public AbstractRegion() {
        this.bottomLeft = null;
        this.topRight = null;
    }

    public AbstractRegion(Location bottomLeft, Location topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }
    
    public AbstractRegion(Location point) {
        this.bottomLeft = point;
        this.topRight = point;
    }

    public AbstractRegion(SpatialRegion other) {
        if (other != null && other.getBottomLeft() != null) {
            this.bottomLeft = new Location(other.getBottomLeft());
            this.topRight = new Location(other.getTopRight());
        }
    }

    @Override
    public Location getBottomLeft() { return bottomLeft; }

    @Override
    public Location getTopRight() { return topRight; }

    @Override
    public void set(SpatialRegion other) {
        if (other == null || other.getBottomLeft() == null || other.getTopRight() == null) {
            this.bottomLeft = null; 
            this.topRight = null; 
            return;
        }
        this.bottomLeft = new Location(other.getBottomLeft());
        this.topRight = new Location(other.getTopRight());
    }

    @Override
    public int compareTo(SpatialRegion o) {
        Objects.requireNonNull(o, "Cannot compare to null Region");
        // Handle null bounds gracefully for comparison
        if (this.bottomLeft == null && o.getBottomLeft() == null) return 0;
        if (this.bottomLeft == null) return -1;
        if (o.getBottomLeft() == null) return 1;
        
        int blComp = this.bottomLeft.compareTo(o.getBottomLeft());
        if (blComp != 0) return blComp;
        return this.topRight.compareTo(o.getTopRight());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bl=" + bottomLeft + ", tr=" + topRight + '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpatialRegion)) return false;
        SpatialRegion other = (SpatialRegion) obj;
        return Objects.equals(bottomLeft, other.getBottomLeft()) &&
               Objects.equals(topRight, other.getTopRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(bottomLeft, topRight);
    }
}