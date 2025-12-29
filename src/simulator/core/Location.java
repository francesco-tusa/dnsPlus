package simulator.core;

import java.text.DecimalFormat;
import java.util.Objects;

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

    /**
     * Copy constructor.
     */
    public Location(Location l) {
        Objects.requireNonNull(l, "Location to copy cannot be null.");
        this.x = l.x;
        this.y = l.y;
        this.z = l.z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public double distanceSquared(Location other) {
        // Cast to double for calculation to preserve precision during squaring
        double dx = (double)this.x - other.x;
        double dy = (double)this.y - other.y;
        double dz = (double)this.z - other.z;
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
        if (cmp != 0) return cmp;
        cmp = Float.compare(this.y, o.y);
        if (cmp != 0) return cmp;
        return Float.compare(this.z, o.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Location location = (Location) obj;
        return Float.compare(location.x, x) == 0 &&
               Float.compare(location.y, y) == 0 &&
               Float.compare(location.z, z) == 0;
    }

    @Override
    public int hashCode() {
        // Updated to hash floats directly
        return Objects.hash(x, y, z);
    }
}