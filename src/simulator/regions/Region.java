package simulator.regions;

import java.text.DecimalFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import simulator.core.Location;

/**
 * Represents a rectangular region that handles longitude wrap-around at the
 * antimeridian (-180/180) for contains(), intersects(), and expand() methods.
 * Extends BaseRegion. Assumes X coordinate is longitude.
 * NOTE: Expanding a region that *already* wraps the antimeridian might not always
 * produce the geographically smallest bounding box with this implementation.
 */
public class Region extends BaseRegion {

    // --- Constructors ---

    /**
     * Creates an empty region.
     */
    public Region() {
        super(); // Calls BaseRegion()
    }

    /**
     * Creates a point region.
     * @param location The location defining the point region. Must not be null.
     */
    public Region(Location location) {
        super(location); // Calls BaseRegion(Location)
    }

    /**
     * Creates a region defined by two corner locations.
     * Allows minLon > maxLon to represent regions crossing the antimeridian.
     * @param bottomLeft The bottom-left-front corner. Must not be null.
     * @param topRight The top-right-back corner. Must not be null.
     */
    public Region(Location bottomLeft, Location topRight) {
        super(bottomLeft, topRight); // Calls BaseRegion(Location, Location)
    }

    /**
     * Copy constructor for Region.
     * @param r The Region to copy. Must not be null.
     */
    public Region(Region r) {
        super(r); // Calls BaseRegion(BaseRegion) copy constructor
    }

    /**
     * Copy constructor from BaseRegion.
     * @param r The BaseRegion to copy. Must not be null.
     */
    public Region(BaseRegion r) {
        super(r); // Calls BaseRegion(BaseRegion) copy constructor
    }

    /**
     * Sets this region's boundaries to be a copy of another region's boundaries.
     * This is the new method that resolves the compilation error.
     * @param other The region to copy the boundaries from.
     */
    public void set(BaseRegion other) {
        if (other == null || other.getBottomLeft() == null || other.getTopRight() == null) {
            this.bottomLeft = null;
            this.topRight = null;
            return;
        }
        // Use copy constructors to avoid aliasing issues.
        this.bottomLeft = new Location(other.getBottomLeft());
        this.topRight = new Location(other.getTopRight());
    }


    // --- Helper for contains/intersects ---
    private boolean containsLongitude(double lon) {
        // Uses inherited bottomLeft and topRight (which are protected in BaseRegion)
        if (bottomLeft == null || topRight == null) return false;
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();

        if (minLon <= maxLon) { // Doesn't wrap
            return lon >= minLon && lon <= maxLon;
        } else { // Wraps
            return lon >= minLon || lon <= maxLon;
        }
    }

    // --- Overridden Methods with Wrap Logic ---

    /**
     * Checks if this region contains the given location, handling antimeridian wrap for longitude (X).
     * @param l The location to check.
     * @return true if the location is within the region, false otherwise.
     */
    @Override
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null || topRight == null) {
            return false;
        }

        // Check Latitude (Y) and Altitude (Z) first (no wrap assumed)
        boolean latOk = l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY();
        boolean altOk = l.getZ() >= bottomLeft.getZ() && l.getZ() <= topRight.getZ();

        if (!latOk || !altOk) {
            return false;
        }

        // Check Longitude (X) using helper that handles wrap
        return containsLongitude(l.getX());
    }

    /**
     * Checks if this region fully contains another region, handling antimeridian wrap.
     * Note: This is a simplified check based on corners.
     * @param r The other region (can be BaseRegion or Region).
     * @return true if this region contains the other region, false otherwise.
     */
    @Override
    public boolean contains(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        // Use the overridden contains(Location) which handles wrap
        return contains(r.getBottomLeft()) && contains(r.getTopRight());
    }

    /**
     * Checks if this region intersects with another region, handling antimeridian wrap for longitude (X).
     * @param r The other region (can be BaseRegion or Region).
     * @return true if the regions intersect, false otherwise.
     */
    @Override
    public boolean intersects(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }

        // Check for non-overlap on Latitude (Y) and Altitude (Z) first
        boolean noOverlapY = this.topRight.getY() < r.getBottomLeft().getY() || this.bottomLeft.getY() > r.getTopRight().getY();
        boolean noOverlapZ = this.topRight.getZ() < r.getBottomLeft().getZ() || this.bottomLeft.getZ() > r.getTopRight().getZ();

        if (noOverlapY || noOverlapZ) {
            return false; // No intersection if they don't overlap on Y or Z
        }

        // Check for non-overlap on Longitude (X), handling wrap-around
        // Uses inherited protected fields bottomLeft, topRight
        double minLon1 = this.bottomLeft.getX();
        double maxLon1 = this.topRight.getX();
        double minLon2 = r.getBottomLeft().getX();
        double maxLon2 = r.getTopRight().getX();

        boolean wraps1 = minLon1 > maxLon1;
        boolean wraps2 = minLon2 > maxLon2;

        boolean noOverlapX;

        if (!wraps1 && !wraps2) { // Case 1: Neither wraps
            noOverlapX = maxLon1 < minLon2 || minLon1 > maxLon2;
        } else if (wraps1 && !wraps2) { // Case 2: Only this region wraps
            noOverlapX = maxLon2 < minLon1 && minLon2 > maxLon1; // No overlap if r is in the gap
        } else if (!wraps1 && wraps2) { // Case 3: Only other region wraps
            noOverlapX = maxLon1 < minLon2 && minLon1 > maxLon2; // No overlap if this is in the gap
        } else { // Case 4: Both wrap
            noOverlapX = false; // They must intersect if both wrap
        }

        return !noOverlapX; // Intersect if they overlap on X (and already passed Y, Z checks)
    }


    /**
     * Expands this region to include the given location. Modifies this region instance.
     * Attempts to handle antimeridian wrapping for longitude.
     * @param l The location to include.
     * @return true if the region was modified, false otherwise.
     */
    @Override
    public boolean expand(Location l) {
        if (l == null) return false;

        // Use inherited protected fields bottomLeft, topRight
        if (bottomLeft == null || topRight == null) {
            // Initialize region with the location if it was empty
            bottomLeft = new Location(l); // Use copy constructor
            topRight = new Location(l);
            return true; // Was updated
        }

        // Store original values for comparison later
        double originalMinLon = bottomLeft.getX();
        double originalMaxLon = topRight.getX();
        double originalMinLat = bottomLeft.getY();
        double originalMaxLat = topRight.getY();
        double originalMinAlt = bottomLeft.getZ();
        double originalMaxAlt = topRight.getZ();

        // --- Latitude (Y) and Altitude (Z) expansion (simple min/max) ---
        double newBlY = Math.min(originalMinLat, l.getY());
        double newBlZ = Math.min(originalMinAlt, l.getZ());
        double newTrY = Math.max(originalMaxLat, l.getY());
        double newTrZ = Math.max(originalMaxAlt, l.getZ());

        // --- Longitude (X) expansion with wrap handling ---
        double lon2 = l.getX();
        double newMinLon, newMaxLon;

        if (containsLongitude(lon2)) { // Use helper which checks wrap
            // Point is already contained, longitude bounds don't change
            newMinLon = originalMinLon;
            newMaxLon = originalMaxLon;
        } else {
            // Point is outside, need to expand longitude range
            boolean currentlyWraps = originalMinLon > originalMaxLon;

            if (currentlyWraps) {
                // Region currently wraps. Expanding might make it unwrap.
                // Simple approach: Assume it stays wrapped and adjust the closer boundary.
                double distToMin = (originalMinLon - lon2 + 360) % 360; // Counter-clockwise distance
                double distToMax = (lon2 - originalMaxLon + 360) % 360; // Clockwise distance

                if (distToMin < distToMax) {
                    newMinLon = lon2; // Expand counter-clockwise
                    newMaxLon = originalMaxLon;
                } else {
                    newMinLon = originalMinLon;
                    newMaxLon = lon2; // Expand clockwise
                }
                // Check if this expansion accidentally unwrapped the region
                if (newMinLon <= newMaxLon) {
                     System.out.println("Warning: Expanding wrapped region resulted in unwrap. Assuming full longitude coverage [-180, 180].");
                     newMinLon = -180.0;
                     newMaxLon = 180.0;
                }

            } else {
                // Region does not currently wrap. Check if expansion causes wrap.
                double testMin = Math.min(originalMinLon, lon2);
                double testMax = Math.max(originalMaxLon, lon2);
                double directWidth = testMax - testMin;
                double wrapWidth = 360.0 - directWidth; // Width going the other way

                if (directWidth <= 180.0 || directWidth <= wrapWidth) { // Prefer non-wrapping if possible or shorter
                    // The direct expansion is shorter or equal, keep it non-wrapped.
                    newMinLon = testMin;
                    newMaxLon = testMax;
                } else {
                    // Wrapping the other way is shorter. New bounds represent wrap.
                    newMinLon = testMax; // The 'max' becomes the start of the wrapped range
                    newMaxLon = testMin; // The 'min' becomes the end of the wrapped range
                }
            }
        }

        // Check if any coordinate actually changed
        boolean updated = ( Double.compare(newMinLon, originalMinLon) != 0 ||
                            Double.compare(newMaxLon, originalMaxLon) != 0 ||
                            Double.compare(newBlY, originalMinLat) != 0 ||
                            Double.compare(newBlZ, originalMinAlt) != 0 ||
                            Double.compare(newTrY, originalMaxLat) != 0 ||
                            Double.compare(newTrZ, originalMaxAlt) != 0);

        if (updated) {
            // Update location objects only if there was a change
            // Create new Location objects to maintain potential immutability of Location
            this.bottomLeft = new Location(newMinLon, newBlY, newBlZ);
            this.topRight = new Location(newMaxLon, newTrY, newTrZ);
        }

        return updated;
    }

    /**
     * Expands this region to include the other region. Modifies this region instance.
     * Uses the overridden expand(Location) method which handles wrap-around.
     * @param r The region to include (can be BaseRegion or Region).
     * @return true if the region was modified, false otherwise.
     */
    @Override
    public boolean expand(BaseRegion r) {
        if (r == null) return false;
        boolean updated = false;
        // Expand by the other region's corners if they exist
        if (r.getBottomLeft() != null) {
             // Use the overridden expand(Location) which handles longitude
            updated |= this.expand(r.getBottomLeft());
        }
        if (r.getTopRight() != null) {
            // Check if corners are different to avoid redundant expansion for point regions
            if (!r.getTopRight().equals(r.getBottomLeft())) {
                 // Use the overridden expand(Location) which handles longitude
                 updated |= this.expand(r.getTopRight());
            } else if (this.bottomLeft == null || this.topRight == null) {
                 // If this region was empty, expanding by a point region's single corner is needed
                 updated |= this.expand(r.getTopRight());
            }
        }
        return updated;
    }

    /**
     * Generates a random location within the bounds of this region.
     * Note: This is a simplified implementation for non-wrapping regions.
     * @return A new Location object with random coordinates within the region.
     */
    public Location getRandomLocation() {
        if (bottomLeft == null || topRight == null) {
            // Return a default or handle as an error, depending on requirements.
            return new Location(0, 0, 0); 
        }

        // Uses the correct getX(), getY(), getZ() methods from your Location class.
        double lon = bottomLeft.getX() + (topRight.getX() - bottomLeft.getX()) * Math.random();
        double lat = bottomLeft.getY() + (topRight.getY() - bottomLeft.getY()) * Math.random();
        double alt = bottomLeft.getZ() + (topRight.getZ() - bottomLeft.getZ()) * Math.random();
        
        return new Location(lon, lat, alt);
    }

    /**
     * Provides a compact string representation of the region for display on the graph.
     * @return A formatted string like "[x1,y1:x2,y2]".
     */
    @JsonIgnore
    public String toShortString() {
        if (bottomLeft == null || topRight == null) {
            return "[]";
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return String.format("[%s,%s:%s,%s]", 
                             df.format(bottomLeft.getX()), 
                             df.format(bottomLeft.getY()), 
                             df.format(topRight.getX()), 
                             df.format(topRight.getY()));
    }

    /**
     * Provides a string representation, indicating if the region wraps longitudinally.
     * @return String representation of the region.
     */
    @Override
    public String toString() {
        // Use inherited protected fields bottomLeft, topRight
        boolean wraps = (bottomLeft != null && topRight != null && bottomLeft.getX() > topRight.getX());
        return "Region{" + "bl=" + bottomLeft + ", tr=" + topRight + (wraps ? " [Wraps]" : "") + '}';
    }

    // Inherits equals, hashCode, compareTo from BaseRegion.
    // Note: compareTo might behave unexpectedly for wrapped regions.
}