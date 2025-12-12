package simulator.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import simulator.core.Location;

/**
 * A standard Cartesian region implementation.
 * Does NOT handle longitude wrapping (Anti-Meridian).
 * Ideal for small-scale grids or local calculations where speed is key.
 */
public class BaseRegion extends AbstractRegion {

    public BaseRegion() { super(); }
    public BaseRegion(Location l) { super(l); }
    public BaseRegion(Location bl, Location tr) { super(bl, tr); }
    public BaseRegion(SpatialRegion r) { super(r); }

    @Override
    public double getWidth() {
        if (bottomLeft == null || topRight == null) return 0.0;
        return topRight.getX() - bottomLeft.getX();
    }

    @Override
    public double getHeight() {
        if (bottomLeft == null || topRight == null) return 0.0;
        return topRight.getY() - bottomLeft.getY();
    }

    @Override
    public double getArea() { return getWidth() * getHeight(); }

    @Override
    public Location getCenter() {
        if (bottomLeft == null) return new Location(0,0,0);
        return new Location(
            (bottomLeft.getX() + topRight.getX()) / 2.0,
            (bottomLeft.getY() + topRight.getY()) / 2.0,
            (bottomLeft.getZ() + topRight.getZ()) / 2.0
        );
    }

    @Override
    public boolean contains(Location l) {
        if (l == null || bottomLeft == null) return false;
        return l.getX() >= bottomLeft.getX() && l.getX() <= topRight.getX() &&
               l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY();
    }

    @Override
    public boolean contains(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;
        return contains(r.getBottomLeft()) && contains(r.getTopRight());
    }

    @Override
    public boolean intersects(SpatialRegion r) {
        if (r == null || bottomLeft == null || r.getBottomLeft() == null) return false;
        return !(topRight.getX() < r.getBottomLeft().getX() || 
                 bottomLeft.getX() > r.getTopRight().getX() ||
                 topRight.getY() < r.getBottomLeft().getY() || 
                 bottomLeft.getY() > r.getTopRight().getY());
    }

    @Override
    public boolean expand(Location l) {
        if (l == null) return false;
        if (bottomLeft == null) {
            bottomLeft = new Location(l);
            topRight = new Location(l);
            return true;
        }
        boolean changed = false;
        double minX = Math.min(bottomLeft.getX(), l.getX());
        double minY = Math.min(bottomLeft.getY(), l.getY());
        double maxX = Math.max(topRight.getX(), l.getX());
        double maxY = Math.max(topRight.getY(), l.getY());
        
        if (minX != bottomLeft.getX() || minY != bottomLeft.getY() ||
            maxX != topRight.getX() || maxY != topRight.getY()) {
            bottomLeft = new Location(minX, minY, bottomLeft.getZ());
            topRight = new Location(maxX, maxY, topRight.getZ());
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean expand(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;
        boolean c1 = expand(r.getBottomLeft());
        boolean c2 = expand(r.getTopRight());
        return c1 || c2;
    }

    @Override
    public SpatialRegion intersection(SpatialRegion other) {
        if (!intersects(other)) return null;
        double x1 = Math.max(bottomLeft.getX(), other.getBottomLeft().getX());
        double y1 = Math.max(bottomLeft.getY(), other.getBottomLeft().getY());
        double x2 = Math.min(topRight.getX(), other.getTopRight().getX());
        double y2 = Math.min(topRight.getY(), other.getTopRight().getY());
        return new BaseRegion(new Location(x1, y1, 0), new Location(x2, y2, 0));
    }

    @Override
    public double getIntersectionArea(SpatialRegion other) {
        SpatialRegion i = intersection(other);
        return (i == null) ? 0.0 : i.getArea();
    }

    @Override
    public double getUnionArea(SpatialRegion other) {
        return this.getArea() + other.getArea() - getIntersectionArea(other);
    }

    @Override
    public List<Location> getKeyPoints() {
        if (bottomLeft == null) return Collections.emptyList();
        List<Location> points = new ArrayList<>();
        double minX = bottomLeft.getX(); double maxX = topRight.getX();
        double minY = bottomLeft.getY(); double maxY = topRight.getY();
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;

        points.add(bottomLeft);
        points.add(new Location(maxX, minY, 0));
        points.add(new Location(minX, maxY, 0));
        points.add(topRight);
        points.add(new Location(midX, minY, 0));
        points.add(new Location(midX, maxY, 0));
        points.add(new Location(minX, midY, 0));
        points.add(new Location(maxX, midY, 0));
        points.add(new Location(midX, midY, 0));
        return points;
    }

    @Override
    public Location getRandomLocation() {
        if (bottomLeft == null) return new Location(0,0,0);
        double x = bottomLeft.getX() + Math.random() * getWidth();
        double y = bottomLeft.getY() + Math.random() * getHeight();
        return new Location(x, y, 0);
    }

    @Override
    public String toShortString() {
        if (bottomLeft == null) return "[]";
        return String.format("[%s,%s]", bottomLeft.toShortString(), topRight.toShortString());
    }

    @Override
    public String toLogString() { return toShortString(); }
}