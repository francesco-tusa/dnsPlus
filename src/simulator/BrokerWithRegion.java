package simulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import broker.Broker;

/**
 *
 * @author f.tusa
 */
public abstract class BrokerWithRegion extends SimulationBroker {
    private Region region;
    private Map<TreeNode, SimulationSubscription> subscriptionTable = new HashMap<>();
    private long numOfRegionUpdates;
    
    /*
     * implements the logic to process a new subscription based on the location or the region
     * of an existing subscription
     */
    public abstract boolean matchingSubscriptionLocationOrRegion(SimulationSubscription existingSubscription, SimulationSubscription newSubscription);

    /*
     * implements the logic to propagate a new subscription to the children of this broker
     */
    protected abstract void sendSubscriptionToChildren(SimulationSubscription s);

    /*
     * implements
     */
    public abstract void processPublicationLocation(SimulationPublication p, TreeNode child);


    
    public BrokerWithRegion(String name) {
        super(name);
        region = new Region();
        numOfRegionUpdates = 0;
    }
    
    public BrokerWithRegion(String name, Location p1, Location p2) {
        super(name);
        region = new Region(p1, p2);
        numOfRegionUpdates = 0;
    }
    
    @Override
    public BrokerWithRegion getParentBroker() {
        SimulationBroker parentBroker = super.getParentBroker();
        if (parentBroker != null && parentBroker instanceof BrokerWithRegion parentBrokerWithRegion) {
            return parentBrokerWithRegion;
        } else {
            return null;
        }
    }

    public Region getRegion() {
        return region;
    }

    public long getNumOfRegionUpdates() {
        return numOfRegionUpdates;
    }

    protected void increaseNumOfRegionUpdates() {
        numOfRegionUpdates++;
    }

    public SimulationSubscription getChildSubscriptionEntry(TreeNode child) {
        return subscriptionTable.get(child);
    }

    protected void updateRegion(TreeNode child) {
        if (child instanceof BrokerWithRegion broker) {
            if (region.expand(broker.region)) {
                System.out.println(getName() + ": updated region");
                numOfRegionUpdates++;
                BrokerWithRegion parentBroker= getParentBroker();
                if (parentBroker != null) {
                    parentBroker.updateRegion(this);
            
                }
            }
        }
    }

    /*
     *   Add the subscription to the Map if not already there
     *   If a subscription from that child exists check and compare
     *   the locations of the two subscriptions calling matchingSubscriptionLocationOrRegion
     */
    @Override
    public void addSubscription(SimulationSubscription s) {
        TreeNode source = s.getSource();

        SimulationSubscription tableEntry = subscriptionTable.get(source);
        if (tableEntry == null) {
            System.out.println(getName() + ": adding subscription " + s + "to the table");
            subscriptionTable.put(source, s);
            sendSubscriptionToChildren(s);
        } else {
            System.out.println(getName() + ": subscription " + s + " already in the table");
            if (matchingSubscriptionLocationOrRegion(tableEntry, s)) {
                s.disableForwarding();
            } else {
                subscriptionTable.put(source, s);
                sendSubscriptionToChildren(s);
            }
        }
    }
    

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {

        SimulationSubscription s = null;

        
        for (TreeNode child : subscriptionTable.keySet()) {
            s = subscriptionTable.get(child);

            if (child ==  p.getSource()) {
                continue;
            }

            processPublicationLocation(p, child);
        }

        return s;
    }


    public void printSubscriptionsTable() {
        System.out.println("\n------------------------------");
        System.out.println(getName() + "'s subscription table:");
        for (TreeNode subscriber : subscriptionTable.keySet()) {
            SimulationSubscription sub = subscriptionTable.get(subscriber);
            System.out.println(subscriber.getName() + " -> " + sub);
        }
        System.out.println("------------------------------");
    }
}
