package simulator;


public class Region {
    private Point bottomLeft;
    private Point topRight;

    public Region(Point bottomLeft, Point topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public Point getBottomLeft() {
        return bottomLeft;
    }

    public void setBottomLeft(Point bottomLeft) {
        this.bottomLeft = bottomLeft;
    }

    public Point getTopRight() {
        return topRight;
    }

    public void setTopRight(Point topRight) {
        this.topRight = topRight;
    }
    
    public Boolean contains(Point p) {
        return p.compareTo(bottomLeft) > 0 && p.compareTo(topRight) < 0;
    }
    
}
