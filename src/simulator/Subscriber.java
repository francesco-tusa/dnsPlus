package simulator;

public class Subscriber extends TreeNode {
    
    private String name;
    
    public Subscriber(String name) {
        this.name = name;
    }
    
    public void send(SubscriptionWithLocation s) {
        s.setSource(this);
        if (parent != null) {
            System.out.println(name + " is sending subscription to " + ((BrokerWithRegion)parent).getName());
            ((BrokerWithRegion)parent).processSubscription(s);
        } else {
            System.out.println(name + " has no broker to send subscription to");
        }
    }
    
}
