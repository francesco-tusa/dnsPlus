package simulator;


public class Region {
    private Location bottomLeft;
    private Location topRight;

    // creates an empty region
    public Region() {}

    public Region(Location location) {
        this(location, location);
    }

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
    
    public Boolean contains(Location l) {
        return l.compareTo(bottomLeft) > 0 && l.compareTo(topRight) < 0;
    }

    public void expand(Location l) {
        if (bottomLeft == null || topRight == null) {
            bottomLeft = new Location(l.getX(), l.getY(), l.getZ());
            topRight = new Location(l.getX(), l.getY(), l.getZ());
        } else {
            int newBottomLeftX = Math.min(bottomLeft.getX(), l.getX());
            int newBottomLeftY = Math.min(bottomLeft.getY(), l.getY());
            int newBottomLeftZ = Math.min(bottomLeft.getZ(), l.getZ());
    
            int newTopRightX = Math.max(topRight.getX(), l.getX());
            int newTopRightY = Math.max(topRight.getY(), l.getY());
            int newTopRightZ = Math.max(topRight.getZ(), l.getZ());
    
            bottomLeft = new Location(newBottomLeftX, newBottomLeftY, newBottomLeftZ);
            topRight = new Location(newTopRightX, newTopRightY, newTopRightZ);
        }
    }
    
    public void expand(Region r) {
        if (bottomLeft == null || topRight == null) {
            // If the existing region is void, initialise it to the region passed as argument
            bottomLeft = r.getBottomLeft() != null ? 
                new Location(r.getBottomLeft().getX(), r.getBottomLeft().getY(), r.getBottomLeft().getZ()) : null;
            topRight = r.getTopRight() != null ? 
                new Location(r.getTopRight().getX(), r.getTopRight().getY(), r.getTopRight().getZ()) : null;
        } 
        
        else {
            // Expand the existing region to include the region passed as argument
            if (r.getBottomLeft() != null) {
                expand(r.getBottomLeft());
            }
            if (r.getTopRight() != null) {
                expand(r.getTopRight());
            }
        }
    }    
    

    @Override
    public String toString() {
        return "Region{" + "bottomLeft=" + bottomLeft + ", topRight=" + topRight + '}';
    }
    
}
