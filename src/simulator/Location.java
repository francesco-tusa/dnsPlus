package simulator;

import java.util.Objects;

public class Location implements Comparable<Location> {
    private final int x;
    private final int y;
    private final int z;

    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location(Location l) {
        this.x = l.x;
        this.y = l.y;
        this.z = l.z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public int compareTo(Location o) {
        if (x != o.getX()) {
            return Integer.compare(x, o.getX());
        } else if (y != o.getY()) {
            return Integer.compare(y, o.getY());
        } else {
            return Integer.compare(z, o.getZ());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Location location = (Location) obj;
        return x == location.x && y == location.y && z == location.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

}
