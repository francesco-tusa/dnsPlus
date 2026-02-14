package simulator.regions;

import simulator.core.Location;
import java.util.List;

/**
 * Common interface for all spatial region implementations.
 * Allows polymorphic handling of Flat (Base) vs. Spherical (Wrapping) logic.
 */
public interface SpatialRegion extends Comparable<SpatialRegion> {

    // --- Data Access ---
    Location getBottomLeft();

    Location getTopRight();

    // --- Dimensions ---
    double getWidth();

    double getHeight();

    double getArea();

    Location getCenter();

    /**
     * Returns the 9 key points (corners, midpoints, center) defining the region.
     * Crucial for location-based routing strategies.
     */
    List<Location> getKeyPoints();

    // --- Spatial Logic ---
    boolean contains(Location l);

    boolean contains(SpatialRegion r);

    boolean intersects(SpatialRegion r);

    // --- Mutators ---
    boolean expand(Location l);

    boolean expand(SpatialRegion r);

    void set(SpatialRegion other);

    // --- Geometric Calculations ---
    SpatialRegion intersection(SpatialRegion other);

    double getIntersectionArea(SpatialRegion other);

    double getUnionArea(SpatialRegion other);

    double distanceSquared(Location p);

    // --- Utilities ---
    Location getRandomLocation();

    String toShortString();

    String toLogString();
}