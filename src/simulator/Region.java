package simulator;

public class Region implements Comparable<Region> {

    private Location bottomLeft;
    private Location topRight;

    // creates an empty region
    public Region() {
    }

    public Region(Location location) {
        this(location, location);
    }

    public Region(Location bottomLeft, Location topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public Region(Region r) {
        this.bottomLeft = new Location(r.bottomLeft);
        this.topRight = new Location(r.topRight);
    }

    public Location getBottomLeft() {
        return bottomLeft;
    }

    public void setBottomLeft(Location bottomLeft) {
        this.bottomLeft = bottomLeft;
    }

    public Location getTopRight() {
        return topRight;
    }

    public void setTopRight(Location topRight) {
        this.topRight = topRight;
    }

    public boolean contains(Location l) {
        if (l == null) {
            return false;
        }
        // Uses inclusive boundaries (>=, <=)
        return l.getX() >= bottomLeft.getX() && l.getX() <= topRight.getX()
                && l.getY() >= bottomLeft.getY() && l.getY() <= topRight.getY()
                && l.getZ() >= bottomLeft.getZ() && l.getZ() <= topRight.getZ();
    }

    public Boolean contains(Region r) {
        if (bottomLeft == null || topRight == null || r.getBottomLeft() == null || r.getTopRight() == null) {
            return false;
        }
        return contains(r.bottomLeft) && contains(r.topRight);
    }

    public Boolean intersects(Region r) {
        if (bottomLeft == null || topRight == null || r.getBottomLeft() == null || r.getTopRight() == null) {
            return false;
        }
        boolean intersectsX = r.getBottomLeft().getX() <= topRight.getX() && r.getTopRight().getX() >= bottomLeft.getX();
        boolean intersectsY = r.getBottomLeft().getY() <= topRight.getY() && r.getTopRight().getY() >= bottomLeft.getY();
        boolean intersectsZ = r.getBottomLeft().getZ() <= topRight.getZ() && r.getTopRight().getZ() >= bottomLeft.getZ();

        return intersectsX && intersectsY && intersectsZ;
    }

    public boolean expand(Location l) {
        boolean updated = false;

        if (bottomLeft == null || topRight == null) {
            bottomLeft = new Location(l.getX(), l.getY(), l.getZ());
            topRight = new Location(l.getX(), l.getY(), l.getZ());
            updated = true;
        } else {
            Location newBottomLeft = new Location(
                    Math.min(bottomLeft.getX(), l.getX()),
                    Math.min(bottomLeft.getY(), l.getY()),
                    Math.min(bottomLeft.getZ(), l.getZ())
            );

            Location newTopRight = new Location(
                    Math.max(topRight.getX(), l.getX()),
                    Math.max(topRight.getY(), l.getY()),
                    Math.max(topRight.getZ(), l.getZ())
            );

            if (!newBottomLeft.equals(bottomLeft) || !newTopRight.equals(topRight)) {
                bottomLeft = newBottomLeft;
                topRight = newTopRight;
                updated = true;
            }
        }

        return updated;
    }

    public boolean expand(Region r) {
        boolean updated = false;

        if (bottomLeft == null || topRight == null) {
            bottomLeft = r.getBottomLeft() != null
                    ? new Location(r.getBottomLeft().getX(), r.getBottomLeft().getY(), r.getBottomLeft().getZ()) : null;
            topRight = r.getTopRight() != null
                    ? new Location(r.getTopRight().getX(), r.getTopRight().getY(), r.getTopRight().getZ()) : null;
            updated = true;
        } else {
            if (r.getBottomLeft() != null) {
                updated |= expand(r.getBottomLeft());
            }
            if (r.getTopRight() != null) {
                updated |= expand(r.getTopRight());
            }
        }

        return updated;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Region region = (Region) obj;

        return (bottomLeft != null ? bottomLeft.equals(region.bottomLeft) : region.bottomLeft == null)
                && (topRight != null ? topRight.equals(region.topRight) : region.topRight == null);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (bottomLeft != null ? bottomLeft.hashCode() : 0);
        result = 31 * result + (topRight != null ? topRight.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{" + "bottomLeft=" + bottomLeft + ", topRight=" + topRight + '}';
    }

    @Override
    public int compareTo(Region o) {
        if (this.bottomLeft == null || this.topRight == null || o.bottomLeft == null || o.topRight == null) {
            throw new NullPointerException("Regions must have defined bottomLeft and topRight locations.");
        }

        int bottomLeftComparison = this.bottomLeft.compareTo(o.bottomLeft);

        if (bottomLeftComparison != 0) {
            return bottomLeftComparison;
        }

        return this.topRight.compareTo(o.topRight);
    }
}
