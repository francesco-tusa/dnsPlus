package simulator.regions;

import simulator.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a basic rectangular region in 3D space defined by two corner Locations.
 * Uses double-precision coordinates. Does NOT handle antimeridian wrap-around.
 * This serves as the base class for the Region class that adds wrap-around logic.
 */
public class BaseRegion implements Comparable<BaseRegion> {

    // Changed visibility to protected so subclass can access them directly
    protected Location bottomLeft;
    protected Location topRight;

    /**
     * Creates an empty region (both corners null).
     */
    public BaseRegion() {
        this.bottomLeft = null;
        this.topRight = null;
    }

    /**
     * Creates a point region where bottomLeft and topRight are the same.
     * @param location The location defining the point region. Must not be null.
     */
    public BaseRegion(Location location) {
        Objects.requireNonNull(location, "Location for point region cannot be null.");
        this.bottomLeft = location; // Assuming Location is immutable
        this.topRight = location;
    }

    /**
     * Creates a region defined by two corner locations.
     * Does not enforce corner order (e.g., minLon <= maxLon).
     * @param bottomLeft The bottom-left-front corner. Must not be null.
     * @param topRight The top-right-back corner. Must not be null.
     */
    public BaseRegion(Location bottomLeft, Location topRight) {
        Objects.requireNonNull(bottomLeft, "Bottom-left location cannot be null.");
        Objects.requireNonNull(topRight, "Top-right location cannot be null.");
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    /**
     * Copy constructor.
     * @param r The BaseRegion to copy. Must not be null.
     */
    public BaseRegion(BaseRegion r) {
        Objects.requireNonNull(r, "Region to copy cannot be null.");
        // Assuming Location has a copy constructor or is immutable
        this.bottomLeft = (r.bottomLeft != null) ? new Location(r.bottomLeft) : null;
        this.topRight = (r.topRight != null) ? new Location(r.topRight) : null;
    }

    // --- Getters ---
    public Location getBottomLeft() {
        return bottomLeft;
    }

    public Location getTopRight() {
        return topRight;
    }

    // --- Setters (Consider removing for immutability) ---
    public void setBottomLeft(Location bottomLeft) {
        Objects.requireNonNull(bottomLeft, "Bottom-left location cannot be null.");
        this.bottomLeft = bottomLeft;
    }

    public void setTopRight(Location topRight) {
        Objects.requireNonNull(topRight, "Top-right location cannot be null.");
        this.topRight = topRight;
    }


    /**
     * Calculates a set of 9 key points representing the region's 2D bounding box.
     * These points (corners, edge midpoints, center) are used by the location-based
     * routing algorithm.
     * @return A list of 9 key point Locations.
     */
    public List<Location> getKeyPoints() {
        if (bottomLeft == null || topRight == null) {
            return Collections.emptyList();
        }
        
        List<Location> points = new ArrayList<>(9);
        double minX = bottomLeft.getX();
        double minY = bottomLeft.getY();
        double maxX = topRight.getX();
        double maxY = topRight.getY();
        double midX = minX + (maxX - minX) / 2.0;
        double midY = minY + (maxY - minY) / 2.0;
        
        // Corners
        points.add(new Location(minX, minY, 0)); // Bottom-left
        points.add(new Location(maxX, minY, 0)); // Bottom-right
        points.add(new Location(minX, maxY, 0)); // Top-left
        points.add(new Location(maxX, maxY, 0)); // Top-right
        
        // Midpoints of edges
        points.add(new Location(midX, minY, 0)); // Mid-bottom
        points.add(new Location(midX, maxY, 0)); // Mid-top
        points.add(new Location(minX, midY, 0)); // Mid-left
        points.add(new Location(maxX, midY, 0)); // Mid-right
        
        // Center
        points.add(new Location(midX, midY, 0));
        
        return points;
    }

    // --- Basic Logic Methods (No Wrap Handling) ---

    /**
     * Checks if this region contains the given location (linear check).
     * @param l The location to check.
     * @return true if the location is within the region, false otherwise.
     */
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null || topRight == null) {
            return false;
        }
        // Simple linear check - incorrect for longitude wrap
        return l.getX() >= bottomLeft.getX() && l.getX() <= topRight.getX()
            && l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY()
            && l.getZ() >= bottomLeft.getZ() && l.getZ() <= topRight.getZ();
    }

    /**
     * Checks if this region fully contains another region (linear check).
     * @param r The other region.
     * @return true if this region contains the other region, false otherwise.
     */
    public boolean contains(BaseRegion r) {
        if (r == null || r.bottomLeft == null || r.topRight == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        // Checks corners using the basic contains(Location)
        return contains(r.bottomLeft) && contains(r.topRight);
    }

    /**
     * Checks if this region intersects with another region (linear check).
     * @param r The other region.
     * @return true if the regions intersect, false otherwise.
     */
    public boolean intersects(BaseRegion r) {
        if (r == null || r.bottomLeft == null || r.topRight == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        // Simple linear check - incorrect for longitude wrap
        boolean noOverlapX = this.topRight.getX() < r.bottomLeft.getX() || this.bottomLeft.getX() > r.topRight.getX();
        boolean noOverlapY = this.topRight.getY() < r.bottomLeft.getY() || this.bottomLeft.getY() > r.topRight.getY();
        boolean noOverlapZ = this.topRight.getZ() < r.bottomLeft.getZ() || this.bottomLeft.getZ() > r.topRight.getZ();

        return !(noOverlapX || noOverlapY || noOverlapZ);
    }

    /**
     * Expands this region to include the given location (linear expansion).
     * WARNING: Does NOT correctly handle antimeridian wrapping for longitude.
     * @param l The location to include.
     * @return true if the region was modified, false otherwise.
     */
    public boolean expand(Location l) {
        if (l == null) return false;

        boolean updated = false;

        if (bottomLeft == null || topRight == null) {
            bottomLeft = new Location(l);
            topRight = new Location(l);
            updated = true;
        } else {
            double newBlX = Math.min(bottomLeft.getX(), l.getX());
            double newBlY = Math.min(bottomLeft.getY(), l.getY());
            double newBlZ = Math.min(bottomLeft.getZ(), l.getZ());
            double newTrX = Math.max(topRight.getX(), l.getX());
            double newTrY = Math.max(topRight.getY(), l.getY());
            double newTrZ = Math.max(topRight.getZ(), l.getZ());

            if (Double.compare(newBlX, bottomLeft.getX()) != 0 ||
                Double.compare(newBlY, bottomLeft.getY()) != 0 ||
                Double.compare(newBlZ, bottomLeft.getZ()) != 0 ||
                Double.compare(newTrX, topRight.getX()) != 0 ||
                Double.compare(newTrY, topRight.getY()) != 0 ||
                Double.compare(newTrZ, topRight.getZ()) != 0)
            {
                bottomLeft = new Location(newBlX, newBlY, newBlZ);
                topRight = new Location(newTrX, newTrY, newTrZ);
                updated = true;
            }
        }
        return updated;
    }

    /**
     * Expands this region to include the other region (linear expansion).
     * WARNING: Does NOT correctly handle antimeridian wrapping for longitude.
     * @param r The region to include.
     * @return true if the region was modified, false otherwise.
     */
    public boolean expand(BaseRegion r) {
        if (r == null) return false;
        boolean updated = false;
        if (r.bottomLeft != null) {
            updated |= expand(r.bottomLeft);
        }
        if (r.topRight != null) {
            if (!r.topRight.equals(r.bottomLeft)) {
                 updated |= expand(r.topRight);
            } else if (bottomLeft == null || topRight == null) {
                 updated |= expand(r.topRight);
            }
        }
        return updated;
    }

    // --- Standard Methods (Unchanged) ---
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseRegion region = (BaseRegion) obj;
        return Objects.equals(bottomLeft, region.bottomLeft) &&
               Objects.equals(topRight, region.topRight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bottomLeft, topRight);
    }

    @Override
    public String toString() {
        return "BaseRegion{" + "bl=" + bottomLeft + ", tr=" + topRight + '}';
    }

    @Override
    public int compareTo(BaseRegion o) {
        Objects.requireNonNull(o, "Cannot compare to a null Region.");
        Objects.requireNonNull(this.bottomLeft, "Cannot compare Region with null bottomLeft.");
        Objects.requireNonNull(this.topRight, "Cannot compare Region with null topRight.");
        Objects.requireNonNull(o.bottomLeft, "Cannot compare with Region with null bottomLeft.");
        Objects.requireNonNull(o.topRight, "Cannot compare with Region with null topRight.");

        int blComparison = this.bottomLeft.compareTo(o.bottomLeft);
        if (blComparison != 0) {
            return blComparison;
        }
        return this.topRight.compareTo(o.topRight);
    }
}