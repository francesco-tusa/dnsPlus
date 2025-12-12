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
     * Crucial for location-based routing.
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
    /**
     * Returns the geometric intersection of this region and another.
     * Returns null if they do not intersect.
     */
    SpatialRegion intersection(SpatialRegion other);
    
    double getIntersectionArea(SpatialRegion other);
    double getUnionArea(SpatialRegion other);
    
    // --- Utilities ---
    Location getRandomLocation();
    String toShortString();
    String toLogString();
}