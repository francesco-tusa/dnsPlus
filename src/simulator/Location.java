package simulator;

public class Location implements Comparable<Location> {
    private final int x;
    private final int y;
    private final int z;

    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
}
