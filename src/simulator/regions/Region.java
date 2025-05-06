package simulator.regions;

// Ensure this imports the updated simulator.Location
import simulator.Location;
import java.util.Objects; // Import Objects for null checks

/**
 * Represents a rectangular region in 3D space defined by two corner Locations.
 * Uses double-precision coordinates via the Location class.
 */
public class Region implements Comparable<Region> {

    private Location bottomLeft;
    private Location topRight;

    /**
     * Creates an empty region (both corners null).
     */
    public Region() {
        this.bottomLeft = null;
        this.topRight = null;
    }

    /**
     * Creates a point region where bottomLeft and topRight are the same.
     * @param location The location defining the point region. Must not be null.
     */
    public Region(Location location) {
        // Use Objects.requireNonNull for constructor argument validation
        Objects.requireNonNull(location, "Location for point region cannot be null.");
        // Create defensive copies if Location is mutable, but our updated Location is immutable.
        this.bottomLeft = location;
        this.topRight = location;
    }

    /**
     * Creates a region defined by two corner locations.
     * It's recommended practice to ensure bottomLeft coordinates are <= topRight coordinates,
     * but this constructor doesn't enforce it. Methods like contains assume standard orientation.
     * @param bottomLeft The bottom-left-front corner. Must not be null.
     * @param topRight The top-right-back corner. Must not be null.
     */
    public Region(Location bottomLeft, Location topRight) {
        Objects.requireNonNull(bottomLeft, "Bottom-left location cannot be null.");
        Objects.requireNonNull(topRight, "Top-right location cannot be null.");
        // Create defensive copies if Location were mutable.
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        // Optional: Add validation/normalization to ensure bl.x <= tr.x etc.
        // normalizeCorners();
    }

    /**
     * Copy constructor.
     * @param r The Region to copy. Must not be null.
     */
    public Region(Region r) {
        Objects.requireNonNull(r, "Region to copy cannot be null.");
        // Create defensive copies using the Location copy constructor
        this.bottomLeft = (r.bottomLeft != null) ? new Location(r.bottomLeft) : null;
        this.topRight = (r.topRight != null) ? new Location(r.topRight) : null;
    }

    public Location getBottomLeft() {
        return bottomLeft; // Returning immutable Location is safe
    }

    // Consider making Region immutable by removing setters or having them return new instances
    public void setBottomLeft(Location bottomLeft) {
        Objects.requireNonNull(bottomLeft, "Bottom-left location cannot be null.");
        this.bottomLeft = bottomLeft; // Or new Location(bottomLeft) if Location were mutable
    }

    public Location getTopRight() {
        return topRight; // Returning immutable Location is safe
    }

    // Consider making Region immutable
    public void setTopRight(Location topRight) {
        Objects.requireNonNull(topRight, "Top-right location cannot be null.");
        this.topRight = topRight; // Or new Location(topRight) if Location were mutable
    }


    /**
     * Checks if this region contains the given location.
     * Assumes the region is valid (e.g., bottomLeft.x <= topRight.x).
     * Uses inclusive boundaries (>=, <=).
     * @param l The location to check.
     * @return true if the location is within or on the boundary of the region, false otherwise or if region is invalid.
     */
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null || topRight == null) {
            return false; // Cannot contain null or if region is undefined
        }
        // Logic remains the same, works with doubles
        return l.getX() >= bottomLeft.getX() && l.getX() <= topRight.getX()
            && l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY()
            && l.getZ() >= bottomLeft.getZ() && l.getZ() <= topRight.getZ();
    }


    /**
     * Checks if this region fully contains another region.
     * @param r The other region.
     * @return true if this region contains the other region, false otherwise.
     */
    public boolean contains(Region r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false; // Cannot contain null or undefined regions
        }
        // Check if both corners of the other region are within this region
        return contains(r.bottomLeft) && contains(r.topRight);
    }


    /**
     * Checks if this region intersects with another region.
     * @param r The other region.
     * @return true if the regions intersect, false otherwise.
     */
    public boolean intersects(Region r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false; // Cannot intersect null or undefined regions
        }
        // Check for overlap on each axis
        // Assumes bl.x <= tr.x, etc.
        boolean noOverlapX = this.topRight.getX() < r.getBottomLeft().getX() || this.bottomLeft.getX() > r.getTopRight().getX();
        boolean noOverlapY = this.topRight.getY() < r.getBottomLeft().getY() || this.bottomLeft.getY() > r.getTopRight().getY();
        boolean noOverlapZ = this.topRight.getZ() < r.getBottomLeft().getZ() || this.bottomLeft.getZ() > r.getTopRight().getZ();

        // If there is no overlap on *any* axis, they don't intersect
        return !(noOverlapX || noOverlapY || noOverlapZ);
    }


    /**
     * Expands this region to include the given location. Modifies this region instance.
     * @param l The location to include.
     * @return true if the region was modified, false otherwise.
     */
    public boolean expand(Location l) {
        if (l == null) return false; // Cannot expand with null

        boolean updated = false;

        if (bottomLeft == null || topRight == null) {
            // Initialize region with the location if it was empty
            bottomLeft = new Location(l); // Use copy constructor
            topRight = new Location(l);
            updated = true;
        } else {
            // Calculate potential new corners
            double newBlX = Math.min(bottomLeft.getX(), l.getX());
            double newBlY = Math.min(bottomLeft.getY(), l.getY());
            double newBlZ = Math.min(bottomLeft.getZ(), l.getZ());

            double newTrX = Math.max(topRight.getX(), l.getX());
            double newTrY = Math.max(topRight.getY(), l.getY());
            double newTrZ = Math.max(topRight.getZ(), l.getZ());

            // Check if update is needed (using direct double comparison here)
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
     * Expands this region to include the other region. Modifies this region instance.
     * @param r The region to include.
     * @return true if the region was modified, false otherwise.
     */
    public boolean expand(Region r) {
        if (r == null) return false; // Cannot expand with null region

        boolean updated = false;

        // Expand by the other region's corners if they exist
        if (r.getBottomLeft() != null) {
            updated |= expand(r.getBottomLeft());
        }
        if (r.getTopRight() != null) {
            // Avoid expanding twice with the same point if it's a point region
            if (!r.getTopRight().equals(r.getBottomLeft())) {
                 updated |= expand(r.getTopRight());
            }
        }
        return updated;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Region region = (Region) obj;
        // Use Objects.equals to handle potential nulls gracefully
        return Objects.equals(bottomLeft, region.bottomLeft) &&
               Objects.equals(topRight, region.topRight);
    }


    @Override
    public int hashCode() {
        // Use Objects.hash for consistency with equals
        return Objects.hash(bottomLeft, topRight);
    }


    @Override
    public String toString() {
        return "Region{" + "bl=" + bottomLeft + ", tr=" + topRight + '}';
    }


    /**
     * Compares this region to another region.
     * Comparison is based first on the bottom-left location, then on the top-right location.
     * @param o The Region to compare against.
     * @return a negative integer, zero, or a positive integer.
     * @throws NullPointerException if either region or its corners are null.
     */
    @Override
    public int compareTo(Region o) {
        Objects.requireNonNull(o, "Cannot compare to a null Region.");
        Objects.requireNonNull(this.bottomLeft, "Cannot compare Region with null bottomLeft.");
        Objects.requireNonNull(this.topRight, "Cannot compare Region with null topRight.");
        Objects.requireNonNull(o.bottomLeft, "Cannot compare with Region with null bottomLeft.");
        Objects.requireNonNull(o.topRight, "Cannot compare with Region with null topRight.");


        int blComparison = this.bottomLeft.compareTo(o.bottomLeft);
        if (blComparison != 0) {
            return blComparison;
        }
        // If bottom-left corners are equal, compare top-right corners
        return this.topRight.compareTo(o.topRight);
    }
}
