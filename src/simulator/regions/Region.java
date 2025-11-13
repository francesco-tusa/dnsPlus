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
    private static final DecimalFormat df = new DecimalFormat("#.####");
    private static final double EPSILON = 1e-9;

    public Region() { super(); }
    public Region(Location location) { super(location); }
    public Region(Location bottomLeft, Location topRight) { super(bottomLeft, topRight); }
    public Region(Region r) { super(r); }
    public Region(BaseRegion r) { super(r); }
    public void set(BaseRegion other) {
        if (other == null || other.getBottomLeft() == null || other.getTopRight() == null) {
            this.bottomLeft = null; this.topRight = null; return;
        }
        this.bottomLeft = new Location(other.getBottomLeft());
        this.topRight = new Location(other.getTopRight());
    }

    @JsonIgnore
    public double getWidth() {
        if (bottomLeft == null || topRight == null) return 0.0;
        double minLon = bottomLeft.getX(); double maxLon = topRight.getX();
        if (minLon <= maxLon) { return maxLon - minLon; }
        else { return (180.0 - minLon) + (maxLon + 180.0); }
    }

    @JsonIgnore
    public double getHeight() {
        if (bottomLeft == null || topRight == null) {
            return 0.0;
        }
        return topRight.getY() - bottomLeft.getY();
    }


    private boolean containsLongitude(double lon) {
        if (bottomLeft == null || topRight == null) return false;
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();

        boolean gteMin = (lon - minLon) > -EPSILON; // (lon >= minLon)
        boolean lteMax = (lon - maxLon) < EPSILON;  // (lon <= maxLon)

        if ((minLon - maxLon) < EPSILON) { // Standard (minLon <= maxLon)
            return gteMin && lteMax;
        } else { // Wraps (minLon > maxLon)
            return gteMin || lteMax;
        }
    }

    @Override
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null || topRight == null) {
            return false;
        }
        
        boolean latGteMin = (l.getY() - bottomLeft.getY()) > -EPSILON;
        boolean latLteMax = (l.getY() - topRight.getY()) < EPSILON;
        boolean latOk = latGteMin && latLteMax;

        boolean altGteMin = (l.getZ() - bottomLeft.getZ()) > -EPSILON;
        boolean altLteMax = (l.getZ() - topRight.getZ()) < EPSILON;
        boolean altOk = altGteMin && altLteMax;

        if (!latOk || !altOk) {
            return false;
        }
        return containsLongitude(l.getX());
    }


    @Override
    /**
     * Checks if this region intersects with another region.
     * This logic is EXCLUSIVE (>, <) to prevent matching "touching"
     * grid tiles as intersections.
     */
       public boolean intersects(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        

        // "No Overlap" (for exclusive intersection) means:
        // (A.max <= B.min) OR (A.min >= B.max)
        
        // Safe floating-point versions:
        // (B.min - A.max) > -EPSILON  (B.min is greater than or equal to A.max)
        // (A.min - B.max) > -EPSILON  (A.min is greater than or equal to B.max)

        // Check Y-axis for non-overlap
        boolean noOverlapY = (r.getBottomLeft().getY() - this.topRight.getY()) > -EPSILON ||
                             (this.bottomLeft.getY() - r.getTopRight().getY()) > -EPSILON;

        // --- REMOVED Z-AXIS CHECK ---
        // boolean noOverlapZ = (r.getBottomLeft().getZ() - this.topRight().getZ()) > -EPSILON ||
        //                      (this.bottomLeft.getZ() - r.getTopRight().getZ()) > -EPSILON;

        if (noOverlapY) { // --- MODIFIED: Removed || noOverlapZ
            return false; // They do not overlap on the Y-axis
        }

        // --- Check X-axis (Longitude) ---
        double minLon1 = this.bottomLeft.getX();
        double maxLon1 = this.topRight.getX();
        double minLon2 = r.getBottomLeft().getX();
        double maxLon2 = r.getTopRight().getX();

        boolean wraps1 = (minLon1 - maxLon1) > EPSILON; // (minLon1 > maxLon1)
        boolean wraps2 = (minLon2 - maxLon2) > EPSILON; // (minLon2 > maxLon2)

        boolean noOverlapX;
        
        if (!wraps1 && !wraps2) {
            // Standard case: (r.min_x >= this.max_x) OR (this.min_x >= r.max_x)
            noOverlapX = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON ||
                         (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            
        } else if (wraps1 && !wraps2) {
            // Region 1 wraps, Region 2 does not.
            // "No Overlap" if r2 is in the gap of r1.
            // (r2.max_x <= r1.min_x) AND (r2.min_x >= r1.max_x)
            
            // Safe float versions:
            // (r1.min_x - r2.max_x) > -EPSILON  (A.min >= B.max)
            // (r2.min_x - r1.max_x) > -EPSILON  (B.min >= A.max)
            
            boolean rMaxLessEqThisMin = (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            boolean rMinMoreEqThisMax = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON;
            noOverlapX = rMaxLessEqThisMin && rMinMoreEqThisMax;

        } else if (!wraps1 && wraps2) {
            // Region 1 does not wrap, Region 2 wraps.
            // "No Overlap" if r1 is in the gap of r2.
            // (r1.max_x <= r2.min_x) AND (r1.min_x >= r2.max_x)

            // Safe float versions:
            // (r2.min_x - r1.max_x) > -EPSILON  (B.min >= A.max)
            // (r1.min_x - r2.max_x) > -EPSILON  (A.min >= B.max)

            boolean thisMaxLessEqRMin = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON;
            boolean thisMinMoreEqRMax = (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            noOverlapX = thisMaxLessEqRMin && thisMinMoreEqRMax;
            
        } else {
            // Both regions wrap. They must intersect.
            noOverlapX = false;
        }
        
        // An intersection exists *only if* they overlap on ALL axes (X and Y).
        // If there is no overlap on X OR no overlap on Y, return false.
        // Otherwise, return true.
        return !(noOverlapX || noOverlapY);
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
            boolean currentlyWraps = (originalMinLon - originalMaxLon) > EPSILON;
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
                
                if ((newMinLon - newMaxLon) < EPSILON) {
                     logger.warning("Expanding wrapped region resulted in unwrap. Assuming full longitude coverage [-180, 180].");
                     newMinLon = -180.0;
                     newMaxLon = 180.0;
                }
            } else {
                double testMin = Math.min(originalMinLon, lon2);
                double testMax = Math.max(originalMaxLon, lon2);
                
                double directWidth = testMax - testMin;
                double wrapWidth = 360.0 - directWidth;
                
                if (directWidth <= 180.0 || (directWidth - wrapWidth) < EPSILON) {
                    newMinLon = testMin;
                    newMaxLon = testMax;
                } else {
                    newMinLon = testMax;
                    newMaxLon = testMin;
                }
            }
        }
        
        boolean updated = ( Math.abs(newMinLon - originalMinLon) > EPSILON ||
                            Math.abs(newMaxLon - originalMaxLon) > EPSILON ||
                            Math.abs(newBlY - originalMinLat) > EPSILON ||
                            Math.abs(newBlZ - originalMinAlt) > EPSILON ||
                            Math.abs(newTrY - originalMaxLat) > EPSILON ||
                            Math.abs(newTrZ - originalMaxAlt) > EPSILON );

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
        return String.format("[%s,%s:%s,%s]", 
                             df.format(bottomLeft.getX()), 
                             df.format(bottomLeft.getY()), 
                             df.format(topRight.getX()), 
                             df.format(topRight.getY()));
    }
    
    @Override
    public String toString() {
        boolean wraps = (bottomLeft != null && topRight != null && (bottomLeft.getX() - topRight.getX()) > EPSILON);
        return "Region{" + "bl=" + bottomLeft + ", tr=" + topRight + (wraps ? " [Wraps]" : "") + '}';
    }
}