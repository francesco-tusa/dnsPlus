package simulator.core;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.logging.Logger; // Import Logger
import utils.CustomLogger; // Import CustomLogger

/**
 * Memory-Optimized Location class using single-precision (float).
 */
public final class Location implements Comparable<Location> {
    private final float x;
    private final float y;
    private final float z;

    /**
     * Constructor accepting double for compatibility, but casts to float.
     */
    public Location(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    public Location(Location l) {
        Objects.requireNonNull(l, "Location to copy cannot be null.");
        this.x = l.x;
        this.y = l.y;
        this.z = l.z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double distanceSquared(Location other) {
        // 1. Calculate the raw difference for Longitude (X)
        double dx = Math.abs((double) this.x - other.x);

        // 2. WRAPPING LOGIC:
        // If the distance is greater than 180 degrees, the shorter path
        // is the other way around the globe (360 - diff).
        if (dx > 180.0) {
            dx = 360.0 - dx;
        }

        // 3. Calculate difference for Latitude (Y) - Latitude does not wrap
        double dy = (double) this.y - other.y;

        // 4. Calculate difference for Z (preserved for consistency)
        double dz = (double) this.z - other.z;

        // 5. Standard Euclidean sum
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f, %.4f)", x, y, z);
    }

    public String toShortString() {
        DecimalFormat df = new DecimalFormat("#.####");
        return String.format("[%s,%s]", df.format(this.x), df.format(this.y));
    }

    @Override
    public int compareTo(Location o) {
        Objects.requireNonNull(o, "Cannot compare to a null Location.");
        int cmp = Float.compare(this.x, o.x);
        if (cmp != 0)
            return cmp;
        cmp = Float.compare(this.y, o.y);
        if (cmp != 0)
            return cmp;
        return Float.compare(this.z, o.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Location location = (Location) obj;
        return Float.compare(location.x, x) == 0 &&
                Float.compare(location.y, y) == 0 &&
                Float.compare(location.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}