package simulator;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author f.tusa
 */
public abstract class BrokerWithRegion extends SimulationBroker {
    private Region region;
    private Map<TreeNode, SubscriptionTableEntry> subscriptionsTable = new HashMap<>();
    //private Map<TreeNode, SubscriptionTableEntry> sentSubscriptionsTable = new HashMap<>();
    private long numOfRegionUpdates;
    
    /*
     * implements the logic to process a new subscription based on the location or the region
     * of an existing subscription
     */
    protected abstract boolean regionsOrLocationsMatch(SubscriptionTableEntry existingSubscription, SimulationSubscription newSubscription);

    /*
     * implements the logic to propagate a new subscription to the children of this broker
     */
    protected abstract void sendSubscriptionToChildren(SimulationSubscription s);

    /*
     * implements the logic to update an existing subscription region considering the region of a new subscription
     */
    protected abstract void updateSubscriptionEntry(SubscriptionTableEntry existingSubscription, SimulationSubscription newSubscription);

    /*
     * implements
     */
    protected abstract void processPublicationLocation(SimulationPublication p, TreeNode child);


    
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

    public SubscriptionTableEntry getChildSubscriptionEntry(TreeNode child) {
        return subscriptionsTable.get(child);
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
     *   the locations of the two subscriptions calling regionsOrLocationsMatch
     */
    @Override
    public void addSubscription(SimulationSubscription s) {

        System.out.println(getName() + ": executing addSubscription on a subscription from " + s.getSource().getName());

        TreeNode source = s.getSource();
        SubscriptionTableEntry tableEntry = subscriptionsTable.get(source);

        if (tableEntry != null && regionsOrLocationsMatch(tableEntry, s)) {
            System.out.println(getName() + ": subscription already in the table");
            if (source != getParent()) {
                System.out.println(getName() + ": subscriptions matched, disabling upwards propagation");
                s.disableUpwardsForwarding();
                return;
            }
        }

        if (tableEntry != null) {
            System.out.println(getName() + ": subscription already in the table");
            updateSubscriptionEntry(tableEntry, s);
            System.out.println(getName() + ": subscriptions did not match, updated table with " + tableEntry);
            
        } else {
            System.out.println(getName() + ": adding new subscription to the table");
            subscriptionsTable.put(source, s.getTableEntry());
        }

        if (source == getParent()) {
            System.out.println(getName() + ": subscription received from my parent");
        }

        System.out.println(getName() + ": sending subscription received from " + s.getSource().getName() + " to my children");
        sendSubscriptionToChildren(s);
    }

    
    

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {

        SimulationSubscription s = null;

        
        for (TreeNode child : subscriptionsTable.keySet()) {

            if (child ==  p.getSource()) {
                continue;
            }

            processPublicationLocation(p, child);
        }

        // FIXME: the current design forces us to return a subscription, however this is not currently used.
        return s;
    }


    public void printSubscriptionsTable() {
        System.out.println("\n------------------------------");
        System.out.println(getName() + "'s subscription table:");
        for (TreeNode subscriber : subscriptionsTable.keySet()) {
            SubscriptionTableEntry tableEntry = subscriptionsTable.get(subscriber);
            System.out.println(subscriber.getName() + " -> " + tableEntry);
        }
        System.out.println("------------------------------");
    }
}