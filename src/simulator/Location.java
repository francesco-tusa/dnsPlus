package simulator;

import java.util.Objects;

/**
 * Represents a location in 3D space using double-precision coordinates.
 * This class is immutable.
 */
public final class Location implements Comparable<Location> {
    // Use double for coordinates
    private final double x;
    private final double y;
    private final double z;

    /**
     * Constructor accepting double coordinates.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    public Location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copy constructor.
     * @param l The Location object to copy. Must not be null.
     */
    public Location(Location l) {
        Objects.requireNonNull(l, "Location to copy cannot be null.");
        this.x = l.x;
        this.y = l.y;
        this.z = l.z;
    }

    /**
     * Gets the x-coordinate.
     * @return The x-coordinate as a double.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the y-coordinate.
     * @return The y-coordinate as a double.
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the z-coordinate.
     * @return The z-coordinate as a double.
     */
    public double getZ() {
        return z;
    }

    @Override
    public String toString() {
        // Format doubles for clarity if desired, otherwise default toString is fine
        return String.format("(%.4f, %.4f, %.4f)", x, y, z); // Example formatting
        // return "(" + x + ", " + y + ", " + z + ")"; // Default double toString
    }

    /**
     * Compares this location to another location lexicographically using double comparison.
     * @param o The Location to compare against.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(Location o) {
        Objects.requireNonNull(o, "Cannot compare to a null Location.");
        int cmp = Double.compare(this.x, o.x);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Double.compare(this.y, o.y);
        if (cmp != 0) {
            return cmp;
        }
        return Double.compare(this.z, o.z);
    }

    /**
     * Checks for equality with another object.
     * Note: Uses direct double comparison (==). This might be sensitive to
     * floating-point inaccuracies if coordinates result from calculations.
     * It's generally suitable if coordinates are read directly (e.g., from JSON).
     * Consider using an epsilon comparison if high precision or calculations are involved.
     *
     * @param obj The object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Location location = (Location) obj;
        // Direct comparison for doubles. Use epsilon comparison if needed:
        // Math.abs(x - location.x) < EPSILON && ...
        return Double.compare(location.x, x) == 0 &&
               Double.compare(location.y, y) == 0 &&
               Double.compare(location.z, z) == 0;
    }

    /**
     * Computes the hash code based on the double coordinates.
     * Uses Double.doubleToLongBits for consistent hashing.
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        // Use Objects.hash with Double.doubleToLongBits for proper double hashing
        return Objects.hash(x, y, z);
    }
}
