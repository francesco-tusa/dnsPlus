
package simulator;

import subscribing.Subscription;


public class SubscriptionWithLocation extends Subscription {
    Point location;
    TreeNode source;

    public SubscriptionWithLocation(Point location) {
        this.location = location;
    }

    public Point getLocation() {
        return location;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }
    
    
    
}
