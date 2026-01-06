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
    
    // REFACTOR: Replaced map with single boolean state
    private boolean isSubscribedToParent = false;
    
    // REFACTOR: Cache for this broker's own center location 
    private Location myProxyLocation = null;
    
    private List<Location> keyPointsCache = null;

    public ProximityRoutingBroker(String name) { super(name); }
    public ProximityRoutingBroker(String name, Location p1, Location p2) { super(name, p1, p2); }
    
    @Override public int getInputSubscriptionCount() { return inputStore.size(); }
    
    // REFACTOR: Output count is now simply 1 (if subscribed) or 0
    @Override public int getOutputSubscriptionCount() { return isSubscribedToParent ? 1 : 0; }
    
    @Override public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        if (getParentBroker() == null || !isSubscribedToParent) return Collections.emptyMap();
        
        // REFACTOR: Return only the single proxy subscription
        List<SimulationSubscription> sentSubs = new ArrayList<>();
        SubscriptionWithLocation proxySub = new SubscriptionWithLocation(getOrCalculateProxyLocation());
        proxySub.setSource(this);
        sentSubs.add(proxySub);
        
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        result.put(getParentBroker(), sentSubs);
        return result;
    }

    private List<Location> getOrCalculateKeyPoints() {
        if (this.keyPointsCache == null) this.keyPointsCache = getRegion().getKeyPoints();
        return this.keyPointsCache;
    }
    
    // REFACTOR: Helper to get this broker's center location
    protected Location getOrCalculateProxyLocation() {
        if (this.myProxyLocation == null) {
            // Using index 8 (Center) as per previous Leaf implementation convention
            // Ensure region is initialized before calling this
            this.myProxyLocation = getRegion().getKeyPoints().get(8);
        }
        return this.myProxyLocation;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        addSubscription(s);
        
        // REFACTOR: Aggregation Logic 
        // If we are already subscribed to the parent, we stop.
        // If not, we send ONE subscription containing OUR location.
        if (getParentBroker() != null && !isSubscribedToParent) {
            
            Location myLocation = getOrCalculateProxyLocation();
            logger.fine(String.format("%s: Aggregating subscription. Sending my proxy location %s to parent.", 
                    getName(), myLocation.toShortString()));
            
            SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(myLocation);
            proxySubscription.setSource(this);
            
            getParentBroker().processSubscription(proxySubscription);
            isSubscribedToParent = true;
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
            
            forwardedCopy.copyStateFrom(p);
            forwardedCopy.incrementHops();
            
            parentBroker.processPublication(forwardedCopy);
        }
    }

    protected void processPublicationDownward(PublicationWithLocation pub) {
        if (getRegion() == null) return;

        boolean isImprovement = false;

        // FIXME
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
                    
                    forwardedCopy.copyStateFrom(pub);
                    forwardedCopy.incrementHops();
                    
                    childBroker.processPublication(forwardedCopy);
                }
            }
        }
    }
}