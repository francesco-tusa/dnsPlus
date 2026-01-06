package simulator.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.store.BasicSubscriptionStore;
import simulator.core.TreeNode;
import utils.CustomLogger;

public class ProximityRoutingBroker extends BoundedBroker {

    private static final Logger logger = CustomLogger.getLogger(ProximityRoutingBroker.class.getName());
    protected final BasicSubscriptionStore inputStore = new BasicSubscriptionStore();
    private final Map<Location, SimulationPublication> bestPublicationCache = new HashMap<>();
    private final Map<Location, Boolean> propagatedSubscriptions = new HashMap<>();
    private List<Location> keyPointsCache = null;

    public ProximityRoutingBroker(String name) { super(name); }
    public ProximityRoutingBroker(String name, Location p1, Location p2) { super(name, p1, p2); }
    
    @Override public int getInputSubscriptionCount() { return inputStore.size(); }
    @Override public int getOutputSubscriptionCount() { return propagatedSubscriptions.size(); }
    @Override public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        if (getParentBroker() == null || propagatedSubscriptions.isEmpty()) return Collections.emptyMap();
        List<SimulationSubscription> sentSubs = new ArrayList<>();
        for (Location loc : propagatedSubscriptions.keySet()) sentSubs.add(new SubscriptionWithLocation(loc));
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        result.put(getParentBroker(), sentSubs);
        return result;
    }

    private List<Location> getOrCalculateKeyPoints() {
        if (this.keyPointsCache == null) this.keyPointsCache = getRegion().getKeyPoints();
        return this.keyPointsCache;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        addSubscription(s);
        if (s instanceof SubscriptionWithLocation sub) {
            Location proxyLocation = sub.getLocation();
            if (propagatedSubscriptions.containsKey(proxyLocation)) return;
            propagatedSubscriptions.put(proxyLocation, true);
            
            if (getParentBroker() != null) {
                s.setSource(this);
                getParentBroker().processSubscription(s);
            }
        } else if (getParentBroker() != null) {
            getParentBroker().processSubscription(s);
        }
    }

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) throw new IllegalArgumentException("Subscription source cannot be null");
        inputStore.add(s);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p.getSource() != getParentBroker()) propagatePublicationUpward(p);
        if (p instanceof PublicationWithLocation pub) processPublicationDownward(pub);
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BoundedBroker parentBroker = getParentBroker();
        if (parentBroker != null) {
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            
            // FIX: Copy State & Increment Hops (int)
            forwardedCopy.copyStateFrom(p);
            forwardedCopy.incrementHops();
            
            parentBroker.processPublication(forwardedCopy);
        }
    }

    protected void processPublicationDownward(PublicationWithLocation pub) {
        if (getRegion() == null) return;

        boolean isImprovement = false;
        for (Location keyPoint : getOrCalculateKeyPoints()) {
            PublicationWithLocation cachedPub = (PublicationWithLocation) bestPublicationCache.get(keyPoint);
            if (cachedPub == null || pub.getLocation().distanceSquared(keyPoint) < cachedPub.getLocation().distanceSquared(keyPoint)) {
                bestPublicationCache.put(keyPoint, pub);
                isImprovement = true;
            }
        }
        
        if (isImprovement) {
            for (TreeNode child : getChildren()) {
                if (child instanceof BoundedBroker childBroker && child != pub.getSource()) {
                    SimulationPublication forwardedCopy = pub.getPublication();
                    forwardedCopy.setSource(this);
                    
                    // FIX: Copy State & Increment Hops (int)
                    forwardedCopy.copyStateFrom(pub);
                    forwardedCopy.incrementHops();
                    
                    childBroker.processPublication(forwardedCopy);
                }
            }
        }
    }
}