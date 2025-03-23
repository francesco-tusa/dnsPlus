package simulator;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author f.tusa
 */
public class BrokerWithRegion extends SimulationBroker {

    private Region region;
    Map<TreeNode, SubscriptionWithLocation> subscriptionTable = new HashMap<>();
    
    
    public BrokerWithRegion(String name) {
        super(name);
        region = new Region(new Location(0,0,0), new Location(10,10,10));
    }
    
    public BrokerWithRegion(String name, Location p1, Location p2) {
        super(name);
        region = new Region(p1, p2);
    }
   
    
    /*
        Add the subscription to the Map if not already there
        If a subscription from that child exists check and compare
        the locations of the two subscriptions
    */
    
    @Override
    public void addSubscription(SubscriptionWithLocation s) {
        TreeNode source = s.getSource();

        SubscriptionWithLocation entry = subscriptionTable.get(source);
        if (entry == null) {
            System.out.println("Adding subscription to the table");
            subscriptionTable.put(source, s);
            return;
        }
        
        System.out.println("Subscription already in the table");
        if (region.contains(s.getLocation())) {
            System.out.println("Subscription location is within broker region");
        } else {
            System.out.println("Subscription location is outside broker region");
        }
    }
    

    @Override
    public SubscriptionWithLocation matchPublication(PublicationWithLocation p) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void processPublication(PublicationWithLocation p) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
