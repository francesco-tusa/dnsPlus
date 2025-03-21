package simulator;

public class Point implements Comparable<Point> {
    private final int x;
    private final int y;
    private final int z;

    public Point(int x, int y, int z) {
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
    public int compareTo(Point o) {
        if (x == o.getX() && y == o.getY() && z == o.getZ()) {
            return 0;
        } else if (x < o.getX() && y < o.getY() && z <o.getZ())
            return -1;
        else
            return 1;
    }
}
