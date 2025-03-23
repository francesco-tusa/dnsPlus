package simulator;


public class Region {
    private Location bottomLeft;
    private Location topRight;

    public Region(Location bottomLeft, Location topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
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
    
    public Boolean contains(Location p) {
        return p.compareTo(bottomLeft) > 0 && p.compareTo(topRight) < 0;
    }

    @Override
    public String toString() {
        return "Region{" + "bottomLeft=" + bottomLeft + ", topRight=" + topRight + '}';
    }
    
}
