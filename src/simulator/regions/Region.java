package simulator.regions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import simulator.core.Location;
import utils.CustomLogger;

public class Region extends AbstractRegion {

    private static final Logger logger = CustomLogger.getLogger(Region.class.getName());
    private static final double EPSILON = 1e-9;
    private static final DecimalFormat df = new DecimalFormat("#.####");

    public Region() { super(); }
    public Region(Location location) { super(location); }
    public Region(Location bottomLeft, Location topRight) { super(bottomLeft, topRight); }
    public Region(SpatialRegion r) { super(r); }

    @Override
    @JsonIgnore
    public double getWidth() {
        if (bottomLeft == null || topRight == null) return 0.0;
        double minLon = bottomLeft.getX(); 
        double maxLon = topRight.getX();
        if (minLon <= maxLon) { return maxLon - minLon; }
        else { return (180.0 - minLon) + (maxLon + 180.0); }
    }

    @Override
    @JsonIgnore
    public double getHeight() {
        if (bottomLeft == null || topRight == null) return 0.0;
        return topRight.getY() - bottomLeft.getY();
    }

    @Override
    @JsonIgnore
    public double getArea() {
        return getWidth() * getHeight();
    }

    @Override
    @JsonIgnore
    public Location getCenter() {
        if (bottomLeft == null || topRight == null) return new Location(0, 0, 0);

        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();
        double centerLat = (bottomLeft.getY() + topRight.getY()) / 2.0;
        double centerLon;

        if (minLon <= maxLon) {
            centerLon = (minLon + maxLon) / 2.0;
        } else {
            double width = getWidth();
            double midOffset = width / 2.0;
            centerLon = minLon + midOffset;
            if (centerLon > 180.0) centerLon -= 360.0;
        }
        return new Location(centerLon, centerLat, 0);
    }

    private boolean containsLongitude(double lon) {
        if (bottomLeft == null) return false;
        double min = bottomLeft.getX(); double max = topRight.getX();
        boolean gteMin = (lon - min) > -EPSILON;
        boolean lteMax = (lon - max) < EPSILON;
        return (min <= max) ? (gteMin && lteMax) : (gteMin || lteMax);
    }

    @Override
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null) return false;
        boolean latOk = l.getY() >= bottomLeft.getY() - EPSILON && l.getY() <= topRight.getY() + EPSILON;
        return latOk && containsLongitude(l.getX());
    }

    @Override
    public boolean contains(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;
        if (this.getWidth() >= 360.0 - EPSILON) return true;
        return contains(r.getBottomLeft()) && contains(r.getTopRight());
    }

    @Override
    public boolean intersects(SpatialRegion r) {
        if (r == null || bottomLeft == null || r.getBottomLeft() == null) return false;
        
        boolean noOverlapY = (r.getBottomLeft().getY() > topRight.getY() + EPSILON) ||
                             (bottomLeft.getY() > r.getTopRight().getY() + EPSILON);
        if (noOverlapY) return false;

        double min1 = bottomLeft.getX(), max1 = topRight.getX();
        double min2 = r.getBottomLeft().getX(), max2 = r.getTopRight().getX();
        boolean wraps1 = min1 > max1 + EPSILON;
        boolean wraps2 = min2 > max2 + EPSILON;

        boolean noOverlapX;
        if (!wraps1 && !wraps2) {
            noOverlapX = (min2 > max1 + EPSILON) || (min1 > max2 + EPSILON);
        } else if (wraps1 && !wraps2) {
            noOverlapX = (min2 > max1 + EPSILON) && (min1 > max2 + EPSILON);
        } else if (!wraps1 && wraps2) {
            noOverlapX = (min1 > max2 + EPSILON) && (min2 > max1 + EPSILON);
        } else {
            noOverlapX = false; 
        }
        return !noOverlapX;
    }

    @Override
    public boolean expand(Location l) {
        if (l == null) return false;
        if (bottomLeft == null) { set(new Region(l)); return true; }
        
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
        
        // Normalize 180/-180 if needed, but preserve full globe
        // Relax check with EPSILON to avoid jitter
        if (newMinLon < -180.0 - EPSILON) newMinLon += 360.0;
        if (newMinLon > 180.0 + EPSILON) newMinLon -= 360.0;
        if (newMaxLon < -180.0 - EPSILON) newMaxLon += 360.0;
        if (newMaxLon > 180.0 + EPSILON) newMaxLon -= 360.0;

        // CRITICAL FIX: If we have a full globe [-180, 180], do NOT normalize it to [180, 180]
        if (Math.abs(newMinLon - (-180.0)) < EPSILON && Math.abs(newMaxLon - 180.0) < EPSILON) {
            // Keep as is
        } else {
            // Standard clamp
            if (newMinLon > 180.0) newMinLon = 180.0;
            if (newMaxLon > 180.0) newMaxLon = 180.0;
            if (newMinLon < -180.0) newMinLon = -180.0;
            if (newMaxLon < -180.0) newMaxLon = -180.0;
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
    public boolean expand(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;

        if (this.bottomLeft == null) {
            this.set(new Region(r)); // Adopt the incoming region's bounds
            return true;
        }
        
        boolean changed = false;
        double minLat = Math.min(this.bottomLeft.getY(), r.getBottomLeft().getY());
        double maxLat = Math.max(this.topRight.getY(), r.getTopRight().getY());
        double minAlt = Math.min(this.bottomLeft.getZ(), r.getBottomLeft().getZ());
        double maxAlt = Math.max(this.topRight.getZ(), r.getTopRight().getZ());
        
        if (Math.abs(minLat - this.bottomLeft.getY()) > EPSILON ||
            Math.abs(maxLat - this.topRight.getY()) > EPSILON ||
            Math.abs(minAlt - this.bottomLeft.getZ()) > EPSILON ||
            Math.abs(maxAlt - this.topRight.getZ()) > EPSILON) {
            changed = true;
        }

        List<double[]> intervals = new ArrayList<>();
        intervals.addAll(getLonIntervals(this.bottomLeft.getX(), this.topRight.getX()));
        intervals.addAll(getLonIntervals(r.getBottomLeft().getX(), r.getTopRight().getX()));
        
        intervals = mergeIntervals(intervals);
        
        double newMinLon, newMaxLon;
        
        if (intervals.size() == 1) {
            double[] i = intervals.get(0);
            if (Math.abs(i[1] - i[0] - 360.0) < EPSILON) {
                newMinLon = -180.0;
                newMaxLon = 180.0;
            } else {
                newMinLon = i[0];
                newMaxLon = i[1];
            }
        } else {
            double maxGapSize = -1.0;
            double[] maxGap = null;
            
            for (int i = 0; i < intervals.size(); i++) {
                double[] current = intervals.get(i);
                double[] next = intervals.get((i + 1) % intervals.size());
                
                double gapStart = current[1];
                double gapEnd = next[0];
                double gapSize;
                
                if (gapEnd >= gapStart) {
                    gapSize = gapEnd - gapStart;
                } else {
                    gapSize = (180.0 - gapStart) + (gapEnd - (-180.0));
                }
                
                if (gapSize > maxGapSize) {
                    maxGapSize = gapSize;
                    maxGap = new double[]{gapStart, gapEnd};
                }
            }
            newMinLon = maxGap[1];
            newMaxLon = maxGap[0];
        }
        
        // Normalize
        if (newMinLon < -180.0 - EPSILON) newMinLon += 360.0;
        if (newMinLon > 180.0 + EPSILON) newMinLon -= 360.0;
        if (newMaxLon < -180.0 - EPSILON) newMaxLon += 360.0;
        if (newMaxLon > 180.0 + EPSILON) newMaxLon -= 360.0;

        // Protect Full Globe State
        boolean isFullGlobe = (Math.abs(newMinLon - (-180.0)) < EPSILON && Math.abs(newMaxLon - 180.0) < EPSILON);
        if (!isFullGlobe) {
             // Basic clamping to ensure we stay within valid range if not wrapping
             if (newMinLon < -180.0) newMinLon = -180.0;
             if (newMinLon > 180.0) newMinLon = 180.0; 
             // Note: do not forcefully clamp Max < Min here or we break wrapping.
        }

        if (Math.abs(newMinLon - this.bottomLeft.getX()) > EPSILON ||
            Math.abs(newMaxLon - this.topRight.getX()) > EPSILON) {
            changed = true;
        }

        if (changed) {
            this.bottomLeft = new Location(newMinLon, minLat, minAlt);
            this.topRight = new Location(newMaxLon, maxLat, maxAlt);
        }
        
        return changed;
    }
    
    private List<double[]> mergeIntervals(List<double[]> intervals) {
        if (intervals.isEmpty()) return intervals;
        Collections.sort(intervals, (a, b) -> Double.compare(a[0], b[0]));
        
        List<double[]> merged = new ArrayList<>();
        double[] current = intervals.get(0);
        merged.add(current);
        
        for (int i = 1; i < intervals.size(); i++) {
            double[] next = intervals.get(i);
            if (next[0] <= current[1] + EPSILON) { 
                current[1] = Math.max(current[1], next[1]);
            } else {
                current = next;
                merged.add(current);
            }
        }
        // Check for wrapping merge: If the last interval ends at 180 and first starts at -180,
        // and they are effectively the same boundary, we merge them?
        // No, standard intervals are bounded by -180/180. The gap logic handles the connection.
        return merged;
    }

    @Override
    public SpatialRegion intersection(SpatialRegion other) {
        if (!this.intersects(other)) return null;

        double maxMinY = Math.max(this.bottomLeft.getY(), other.getBottomLeft().getY());
        double minMaxY = Math.min(this.topRight.getY(), other.getTopRight().getY());
        double maxMinZ = Math.max(this.bottomLeft.getZ(), other.getBottomLeft().getZ());
        double minMaxZ = Math.min(this.topRight.getZ(), other.getTopRight().getZ());

        List<double[]> thisIntervals = getLonIntervals(this.bottomLeft.getX(), this.topRight.getX());
        List<double[]> otherIntervals = getLonIntervals(other.getBottomLeft().getX(), other.getTopRight().getX());
        
        List<double[]> overlaps = new ArrayList<>();
        for (double[] i1 : thisIntervals) {
            for (double[] i2 : otherIntervals) {
                double start = Math.max(i1[0], i2[0]);
                double end = Math.min(i1[1], i2[1]);
                if (end - start > -EPSILON) { 
                    overlaps.add(new double[]{start, end});
                }
            }
        }
        
        if (overlaps.isEmpty()) return null;
        
        double finalMinLon, finalMaxLon;
        if (overlaps.size() == 1) {
            finalMinLon = overlaps.get(0)[0];
            finalMaxLon = overlaps.get(0)[1];
        } else {
            double[] leftPart = null; 
            double[] rightPart = null; 
            for (double[] ov : overlaps) {
                if (Math.abs(ov[0] - (-180.0)) < EPSILON) leftPart = ov;
                if (Math.abs(ov[1] - 180.0) < EPSILON) rightPart = ov;
            }
            if (leftPart != null && rightPart != null) {
                finalMinLon = rightPart[0]; 
                finalMaxLon = leftPart[1];  
            } else {
                finalMinLon = overlaps.get(0)[0];
                finalMaxLon = overlaps.get(0)[1];
            }
        }
        
        return new Region(
            new Location(finalMinLon, maxMinY, maxMinZ),
            new Location(finalMaxLon, minMaxY, minMaxZ)
        );
    }

    private List<double[]> getLonIntervals(double min, double max) {
        List<double[]> intervals = new ArrayList<>();
        if ((min - max) < EPSILON) { 
            intervals.add(new double[]{min, max});
        } else { 
            intervals.add(new double[]{min, 180.0});
            intervals.add(new double[]{-180.0, max});
        }
        return intervals;
    }

    @Override
    public double getIntersectionArea(SpatialRegion other) {
        SpatialRegion inter = intersection(other);
        return (inter == null) ? 0.0 : inter.getArea();
    }

    @Override
    public double getUnionArea(SpatialRegion other) {
        return this.getArea() + other.getArea() - getIntersectionArea(other);
    }

    @Override
    @JsonIgnore
    public List<Location> getKeyPoints() {
        if (bottomLeft == null) return Collections.emptyList();
        List<Location> points = new ArrayList<>();
        
        double minLon = bottomLeft.getX();
        double maxLon = topRight.getX();
        double minLat = bottomLeft.getY();
        double maxLat = topRight.getY();
        Location center = getCenter();
        
        points.add(bottomLeft);
        points.add(topRight);
        points.add(new Location(minLon, maxLat, 0));
        points.add(new Location(maxLon, minLat, 0));
        
        double midLat = center.getY();
        double midLon = center.getX();
        
        points.add(new Location(midLon, minLat, 0)); 
        points.add(new Location(midLon, maxLat, 0)); 
        points.add(new Location(minLon, midLat, 0)); 
        points.add(new Location(maxLon, midLat, 0)); 
        points.add(center); 
        
        return points;
    }

    @Override
    @JsonIgnore
    public Location getRandomLocation() {
        Random random = new Random();
        if (bottomLeft == null) return new Location(0, 0, 0); 
        double minX = bottomLeft.getX();
        double maxX = topRight.getX();
        double minY = bottomLeft.getY();
        double maxY = topRight.getY();
        
        double width = getWidth();
        double xOffset = width * random.nextDouble();
        double x = minX + xOffset;
        if (x > 180.0) x -= 360.0;
        
        double y = minY + (maxY - minY) * random.nextDouble();
        return new Location(x, y, 0);
    }

    @Override
    @JsonIgnore
    public String toShortString() {
        if (bottomLeft == null) return "[]";
        return String.format("[%s,%s:%s,%s]", 
             df.format(bottomLeft.getX()), df.format(bottomLeft.getY()), 
             df.format(topRight.getX()), df.format(topRight.getY()));
    }

    @Override
    @JsonIgnore
    public String toLogString() { return toShortString(); }
}