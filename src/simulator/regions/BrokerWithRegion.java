package simulator.regions;

import simulator.Location;
import simulator.SimulationBroker;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.TreeNode;

/**
 *
 * @author f.tusa
 */
public abstract class BrokerWithRegion extends SimulationBroker {
    private Region region;
    private long numOfRegionUpdates;

    /*
     * implements the logic to process a new subscription based on the location or
     * the region
     * of an existing subscription
     */
    protected abstract boolean regionsOrLocationsMatch(SimulationSubscription existingSubscription,
            SimulationSubscription newSubscription);

    /*
     * implements the logic to propagate a new subscription to the children of this
     * broker
     */
    protected abstract void sendSubscriptionToChildren(SimulationSubscription s);

    /*
     * implements the logic to update an existing entry in the subscription table
     * subclasses can also update the new subscription being processed if needed
     */
    protected abstract void updateSubscriptions(SimulationSubscription existingSubscription,
            SimulationSubscription newSubscription);


    /*
     * implements the logic to check whether a publication matches the specific location features
     * of the type of subscriptions processed by the broker
     */
    protected abstract void processPublicationLocation(SimulationPublication p, TreeNode child);


    /*
     * implements the logic to forward a publication to the next broker or a subscriber
     */
    protected abstract void forwardPublicationToNode(SimulationPublication p, TreeNode next);



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

    public SimulationSubscription getSubscriptionEntry(TreeNode source) {
        return getSubscriptionsTable().get(source);
    }

    protected void updateRegion(TreeNode child) {
        if (child instanceof BrokerWithRegion broker) {
            if (region.expand(broker.region)) {
                System.out.println(getName() + ": updated region");
                numOfRegionUpdates++;
                BrokerWithRegion parentBroker = getParentBroker();
                if (parentBroker != null) {
                    parentBroker.updateRegion(this);

                }
            }
        }
    }

    /*
     * Add the subscription to the Map if not already there
     * If a subscription from that child exists check and compare
     * the locations of the two subscriptions calling regionsOrLocationsMatch
     */
    @Override
    public void addSubscription(SimulationSubscription s) {

        System.out.println(getName() + ": processing a subscription received from " + s.getSource().getName());

        TreeNode source = s.getSource();
        SimulationSubscription tableEntry = getSubscriptionsTable().get(source);

        if (tableEntry != null && regionsOrLocationsMatch(tableEntry, s)) {
            System.out.println(
                    getName() + ": a subscription entry from " + s.getSource().getName() + " is already in the table");
            if (source != getParent()) {
                System.out.println(getName() + ": subscriptions matched, disabling upwards propagation");
                s.disableUpwardsForwarding();
                return;
            }
        }

        if (tableEntry != null) {
            System.out.println(
                    getName() + ": a subscription entry from " + s.getSource().getName() + " is already in the table");
            System.out.println(getName() + ": subscriptions did not match");
            updateSubscriptions(tableEntry, s);
        } else {
            System.out.println(getName() + ": adding new subscription to the table");
            getSubscriptionsTable().put(source, s.getTableEntry());
        }

        if (source == getParent()) {
            System.out.println(getName() + ": subscription received from my parent");
        }

        System.out.println(
                getName() + ": sending subscription received from " + s.getSource().getName() + " to my children");
        sendSubscriptionToChildren(s);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());
        for (TreeNode nextBroker : getSubscriptionsTable().keySet()) {
            if (nextBroker == p.getSource()) {
                System.out.println(getName() + ": skipping " + nextBroker.getName() + " as it sent the publication");
                continue;
            }
            processPublicationLocation(p, nextBroker);
        }
        // FIXME: existing design forces us to return a subscription but this is not used here.
        return null;
    }
}