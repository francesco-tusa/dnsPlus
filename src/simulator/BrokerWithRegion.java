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
     *   Add the subscription to the Map if not already there
     *   If a subscription from that child exists check and compare
     *   the locations of the two subscriptions calling processLocation
     */
    @Override
    public void addSubscription(SubscriptionWithLocation s) {
        TreeNode source = s.getSource();

        SubscriptionWithLocation tableEntry = subscriptionTable.get(source);
        if (tableEntry == null) {
            System.out.println(getName() + ": adding subscription to the table");
            subscriptionTable.put(source, s);
            return;
        }
        
        System.out.println(getName() + ": subscription already in the table");
        processLocation(s);
    }


    /*
     * implements the logic to process the subscription based on the location
     * of the subscription and the broker region
     */
    private void processLocation(SubscriptionWithLocation s) {
        if (region.contains(s.getLocation())) {
            System.out.println(getName() + ": subscription location is within broker region");
        } else {
            System.out.println(getName() + ": subscription location is outside broker region");
            // TODO: should enlarge the current region if necessary and possibly count the number of times this happens
        }
        
    }
    

    @Override
    public SubscriptionWithLocation matchPublication(PublicationWithLocation p) {
        for (TreeNode subscriber : subscriptionTable.keySet()) {
            SubscriptionWithLocation s = subscriptionTable.get(subscriber);

            if (subscriber ==  p.getSource()) {
                continue;
            }

            // just for testing, should check the region properly
            if (s.getLocation().compareTo(p.getLocation()) > 0) {
                System.out.println(getName() + ": forwarding publication to " + subscriber.getName());
                if (subscriber instanceof BrokerWithRegion) {
                    return ((BrokerWithRegion) subscriber).matchPublication(p);
                } else if (subscriber instanceof Subscriber) {
                    ((Subscriber) subscriber).receive(p);
                    return s;
                }
            } else {
                System.out.println(getName() + ": publication location is outside subscription location");
            }
        }

        return null;
    }


    public void printSubscriptions() {
        System.out.println("\n------------------------------");
        System.out.println(getName() + "'s subscription table:");
        for (TreeNode subscriber : subscriptionTable.keySet()) {
            SubscriptionWithLocation sub = subscriptionTable.get(subscriber);
            System.out.println(subscriber.getName() + " -> " + sub.getLocation());
        }
        System.out.println("------------------------------");
    }
}
