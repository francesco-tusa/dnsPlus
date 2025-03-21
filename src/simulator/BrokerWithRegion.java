package simulator;

import broker.Broker;
import java.util.HashMap;
import java.util.Map;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author f.tusa
 */
public class BrokerWithRegion extends TreeNode implements Broker {
    
    private String name;
    private Region region;
    
    private int nSubscriptions;
    private int nPublications;
    
    Map<TreeNode, SubscriptionWithLocation> subscriptionTable = new HashMap<>();
    
    
    public BrokerWithRegion(String name) {
        this.name = name;
        region = new Region(new Point(0,0,0), new Point(10,10,10));
    }
    
    public BrokerWithRegion(String name, Point p1, Point p2) {
        this.name = name;
        region = new Region(p1, p2);
    }

    public String getName() {
        return name;
    }
   
    
    @Override
    public void addSubscription(Subscription s) {
        SubscriptionWithLocation subscription = (SubscriptionWithLocation)s;
        TreeNode source = subscription.getSource();
        SubscriptionWithLocation entry = subscriptionTable.get(source);
        if (entry == null) {
            System.out.println("Adding subscription to the table");
            subscriptionTable.put(source, subscription);
            return;
        }
        
        System.out.println("Subscription already in the table");
        if (region.contains(subscription.getLocation())) {
            System.out.println("Subscription location is within broker region");
        } else {
            System.out.println("Subscription location is outside broker region");
        }
    }
    

    @Override
    public Subscription matchPublication(Publication p) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void processPublication(Publication p) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void processSubscription(Subscription s) {
        System.out.println("Processing subscription on node: " + name);
        nSubscriptions++;
        addSubscription(s);
        
        if (parent != null) {
            ((SubscriptionWithLocation)s).setSource(this);
            ((BrokerWithRegion)parent).processSubscription(s);
        }
        
    }
    
}
