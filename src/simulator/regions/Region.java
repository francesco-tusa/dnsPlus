package simulator.regions;

import java.text.DecimalFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import simulator.core.Location;
import java.util.logging.Logger;
import utils.CustomLogger;

public class Region extends BaseRegion {

    private static final Logger logger = CustomLogger.getLogger(Region.class.getName());
    private static final double EPSILON = 1e-9;
    private static final DecimalFormat df = new DecimalFormat("#.####");

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

    /**
     * Calculates the geometric center (centroid) of the region.
     * Handles longitude wrapping correctly.
     * @return The center Location.
     */
    @JsonIgnore
    public Location getCenter() {
        if (bottomLeft == null || topRight == null) {
            return new Location(0, 0, 0);
        }

        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();
        double minLat = bottomLeft.getY();
        double maxLat = topRight.getY();

        // Latitude simply averages (clamped -90 to 90 in world terms)
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon;

        if (minLon <= maxLon) {
            // Standard case: Region does not cross the dateline
            centerLon = (minLon + maxLon) / 2.0;
        } else {
            // Wrapped case: Region crosses 180/-180
            // Total width spans across the dateline
            double width = (180.0 - minLon) + (maxLon - (-180.0));
            double midOffset = width / 2.0;
            
            centerLon = minLon + midOffset;
            // Normalize if it crosses past 180
            if (centerLon > 180.0) {
                centerLon -= 360.0;
            }
        }
        
        return new Location(centerLon, centerLat, 0);
    }

    private boolean containsLongitude(double lon) {
        if (bottomLeft == null || topRight == null) return false;
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();

        boolean gteMin = (lon - minLon) > -EPSILON; 
        boolean lteMax = (lon - maxLon) < EPSILON;  

        if ((minLon - maxLon) < EPSILON) { 
            return gteMin && lteMax;
        } else { 
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
    
    public boolean contains(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null) return false;
        return this.contains(r.getBottomLeft()) && this.contains(r.getTopRight());
    }

    @Override
       public boolean intersects(BaseRegion r) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null || this.bottomLeft == null || this.topRight == null) {
            return false;
        }
        
        boolean noOverlapY = (r.getBottomLeft().getY() - this.topRight.getY()) > -EPSILON ||
                             (this.bottomLeft.getY() - r.getTopRight().getY()) > -EPSILON;

        if (noOverlapY) { 
            return false; 
        }

        double minLon1 = this.bottomLeft.getX();
        double maxLon1 = this.topRight.getX();
        double minLon2 = r.getBottomLeft().getX();
        double maxLon2 = r.getTopRight().getX();

        boolean wraps1 = (minLon1 - maxLon1) > EPSILON; 
        boolean wraps2 = (minLon2 - maxLon2) > EPSILON; 

        boolean noOverlapX;
        
        if (!wraps1 && !wraps2) {
            noOverlapX = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON ||
                         (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            
        } else if (wraps1 && !wraps2) {
            boolean rMaxLessEqThisMin = (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            boolean rMinMoreEqThisMax = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON;
            noOverlapX = rMaxLessEqThisMin && rMinMoreEqThisMax;

        } else if (!wraps1 && wraps2) {
            boolean thisMaxLessEqRMin = (r.getBottomLeft().getX() - this.topRight.getX()) > -EPSILON;
            boolean thisMinMoreEqRMax = (this.bottomLeft.getX() - r.getTopRight().getX()) > -EPSILON;
            noOverlapX = thisMaxLessEqRMin && thisMinMoreEqRMax;
            
        } else {
            noOverlapX = false;
        }
        
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
    
    @JsonIgnore
    public String toLogString() {
        if (bottomLeft == null || topRight == null) {
            return "[]";
        }
        return String.format("[%.4f,%.4f:%.4f,%.4f]", 
                             bottomLeft.getX(), 
                             bottomLeft.getY(), 
                             topRight.getX(), 
                             topRight.getY());
    }
    
    @Override
    public String toString() {
        boolean wraps = (bottomLeft != null && topRight != null && (bottomLeft.getX() - topRight.getX()) > EPSILON);
        return "Region{" + "bl=" + bottomLeft + ", tr=" + topRight + (wraps ? " [Wraps]" : "") + '}';
    }
}