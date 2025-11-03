package simulator.regions;

import java.text.DecimalFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import simulator.core.Location;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 * Represents a rectangular region that handles longitude wrap-around at the
 * antimeridian (-180/180) for contains(), intersects(), and expand() methods.
 * Extends BaseRegion. Assumes X coordinate is longitude.
 * NOTE: Expanding a region that *already* wraps the antimeridian might not always
 * produce the geographically smallest bounding box with this implementation.
 */
public class Region extends BaseRegion {

    private static final Logger logger = CustomLogger.getLogger(Region.class.getName());

    // Create the formatter only once and make it static to improve performance  ---
    private static final DecimalFormat df = new DecimalFormat("#.##");

    // --- Constructors ---
    public Region() {
        super(); // Calls BaseRegion()
    }
    public Region(Location location) {
        super(location); // Calls BaseRegion(Location)
    }
    public Region(Location bottomLeft, Location topRight) {
        super(bottomLeft, topRight); // Calls BaseRegion(Location, Location)
    }
    public Region(Region r) {
        super(r); // Calls BaseRegion(BaseRegion) copy constructor
    }
    public Region(BaseRegion r) {
        super(r); // Calls BaseRegion(BaseRegion) copy constructor
    }
    public void set(BaseRegion other) {
        if (other == null || other.getBottomLeft() == null || other.getTopRight() == null) {
            this.bottomLeft = null;
            this.topRight = null;
            return;
        }
        this.bottomLeft = new Location(other.getBottomLeft());
        this.topRight = new Location(other.getTopRight());
    }

    /**
     * Gets the width (X-axis span) of the region.
     * Correctly handles regions that wrap the antimeridian (e.g., longitude from +170 to -170).
     * @return The width of the region.
     */
    @JsonIgnore
    public double getWidth() {
        if (bottomLeft == null || topRight == null) {
            return 0.0;
        }
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();

        if (minLon <= maxLon) {
            // Standard case: e.g., -10 to +20. Width = 20 - (-10) = 30
            return maxLon - minLon;
        } else {
            // Wrap-around case: e.g., +170 to -170.
            // Width = (180 - 170) + (180 - 170) = 10 + 10 = 20 (assuming -180 to +180)
            // A simpler generic calculation: (180 - minLon) + (maxLon - (-180))
            return (180.0 - minLon) + (maxLon + 180.0);
        }
    }

    /**
     * Gets the height (Y-axis span) of the region.
     * Does not handle polar wrap-around.
     * @return The height of the region.
     */
    @JsonIgnore
    public double getHeight() {
        if (bottomLeft == null || topRight == null) {
            return 0.0;
        }
        // Latitude (Y) is a simple subtraction
        return topRight.getY() - bottomLeft.getY();
    }


    // --- Helper for contains/intersects ---
    private boolean containsLongitude(double lon) {
        if (bottomLeft == null || topRight == null) return false;
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();

        if (minLon <= maxLon) { // Doesn't wrap
            return lon >= minLon && lon <= maxLon;
        } else { // Wraps
            return lon >= minLon || lon <= maxLon;
        }
    }

    @Override
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null || topRight == null) {
            return false;
        }
        boolean latOk = l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY();
        boolean altOk = l.getZ() >= bottomLeft.getZ() && l.getZ() <= topRight.getZ();
        if (!latOk || !altOk) {
            return false;
        }
        return containsLongitude(l.getX());
    }
    @Override
    public boolean contains(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        return contains(r.getBottomLeft()) && contains(r.getTopRight());
    }
    @Override
    public boolean intersects(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        boolean noOverlapY = this.topRight.getY() < r.getBottomLeft().getY() || this.bottomLeft.getY() > r.getTopRight().getY();
        boolean noOverlapZ = this.topRight.getZ() < r.getBottomLeft().getZ() || this.bottomLeft.getZ() > r.getTopRight().getZ();
        if (noOverlapY || noOverlapZ) {
            return false;
        }
        double minLon1 = this.bottomLeft.getX();
        double maxLon1 = this.topRight.getX();
        double minLon2 = r.getBottomLeft().getX();
        double maxLon2 = r.getTopRight().getX();
        boolean wraps1 = minLon1 > maxLon1;
        boolean wraps2 = minLon2 > maxLon2;
        boolean noOverlapX;
        if (!wraps1 && !wraps2) {
            noOverlapX = maxLon1 < minLon2 || minLon1 > maxLon2;
        } else if (wraps1 && !wraps2) {
            noOverlapX = maxLon2 < minLon1 && minLon2 > maxLon1;
        } else if (!wraps1 && wraps2) {
            noOverlapX = maxLon1 < minLon2 && minLon1 > maxLon2;
        } else {
            noOverlapX = false;
        }
        return !noOverlapX;
    }
    @Override
    public boolean expand(Location l) {
        if (l == null) return false;
        if (bottomLeft == null || topRight == null) {
            bottomLeft = new Location(l);
            topRight = new Location(l);
            return true;
        }
        double originalMinLon = bottomLeft.getX();
        double originalMaxLon = topRight.getX();
        double originalMinLat = bottomLeft.getY();
        double originalMaxLat = topRight.getY();
        double originalMinAlt = bottomLeft.getZ();
        double originalMaxAlt = topRight.getZ();
        double newBlY = Math.min(originalMinLat, l.getY());
        double newBlZ = Math.min(originalMinAlt, l.getZ());
        double newTrY = Math.max(originalMaxLat, l.getY());
        double newTrZ = Math.max(originalMaxAlt, l.getZ());
        double lon2 = l.getX();
        double newMinLon, newMaxLon;
        if (containsLongitude(lon2)) {
            newMinLon = originalMinLon;
            newMaxLon = originalMaxLon;
        } else {
            boolean currentlyWraps = originalMinLon > originalMaxLon;
            if (currentlyWraps) {
                double distToMin = (originalMinLon - lon2 + 360) % 360;
                double distToMax = (lon2 - originalMaxLon + 360) % 360;
                if (distToMin < distToMax) {
                    newMinLon = lon2;
                    newMaxLon = originalMaxLon;
                } else {
                    newMinLon = originalMinLon;
                    newMaxLon = lon2;
                }
                if (newMinLon <= newMaxLon) {
                     logger.warning("Expanding wrapped region resulted in unwrap. Assuming full longitude coverage [-180, 180].");
                     newMinLon = -180.0;
                     newMaxLon = 180.0;
                }
            } else {
                double testMin = Math.min(originalMinLon, lon2);
                double testMax = Math.max(originalMaxLon, lon2);
                double directWidth = testMax - testMin;
                double wrapWidth = 360.0 - directWidth;
                if (directWidth <= 180.0 || directWidth <= wrapWidth) {
                    newMinLon = testMin;
                    newMaxLon = testMax;
                } else {
                    newMinLon = testMax;
                    newMaxLon = testMin;
                }
            }
        }
        boolean updated = ( Double.compare(newMinLon, originalMinLon) != 0 ||
                            Double.compare(newMaxLon, originalMaxLon) != 0 ||
                            Double.compare(newBlY, originalMinLat) != 0 ||
                            Double.compare(newBlZ, originalMinAlt) != 0 ||
                            Double.compare(newTrY, originalMaxLat) != 0 ||
                            Double.compare(newTrZ, originalMaxAlt) != 0);
        if (updated) {
            this.bottomLeft = new Location(newMinLon, newBlY, newBlZ);
            this.topRight = new Location(newMaxLon, newTrY, newTrZ);
        }
        return updated;
    }
    @Override
    public boolean expand(BaseRegion r) {
        if (r == null) return false;
        boolean updated = false;
        if (r.getBottomLeft() != null) {
             updated |= this.expand(r.getBottomLeft());
        }
        if (r.getTopRight() != null) {
            if (!r.getTopRight().equals(r.getBottomLeft())) {
                 updated |= this.expand(r.getTopRight());
            } else if (this.bottomLeft == null || this.topRight == null) {
                 updated |= this.expand(r.getTopRight());
            }
        }
        return updated;
    }
    public Location getRandomLocation() {
        if (bottomLeft == null || topRight == null) {
            return new Location(0, 0, 0); 
        }
        double lon = bottomLeft.getX() + (topRight.getX() - bottomLeft.getX()) * Math.random();
        double lat = bottomLeft.getY() + (topRight.getY() - bottomLeft.getY()) * Math.random();
        double alt = bottomLeft.getZ() + (topRight.getZ() - bottomLeft.getZ()) * Math.random();
        
        return new Location(lon, lat, alt);
    }
    @JsonIgnore
    public String toShortString() {
        if (bottomLeft == null || topRight == null) {
            return "[]";
        }
        // Use the static final formatter to improve performance ---
        return String.format("[%s,%s:%s,%s]", 
                             df.format(bottomLeft.getX()), 
                             df.format(bottomLeft.getY()), 
                             df.format(topRight.getX()), 
                             df.format(topRight.getY()));
    }
    @Override
    public String toString() {
        boolean wraps = (bottomLeft != null && topRight != null && bottomLeft.getX() > topRight.getX());
        return "Region{" + "bl=" + bottomLeft + ", tr=" + topRight + (wraps ? " [Wraps]" : "") + '}';
    }
}
