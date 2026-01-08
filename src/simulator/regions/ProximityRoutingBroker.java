package simulator.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.store.BasicSubscriptionStore;
import utils.CustomLogger;

public class ProximityRoutingBroker extends BoundedBroker {

    private static final Logger logger = CustomLogger.getLogger(ProximityRoutingBroker.class.getName());

    protected final BasicSubscriptionStore inputStore = new BasicSubscriptionStore();
    
    // --- High-Performance Routing State ---
    // Maps a child (Broker or Subscriber) to its geometric targets (1 point or 4 quadrant centers)
    protected final Map<TreeNode, Location[]> childTopologicalTargets = new HashMap<>();
    
    // Tracks the best distance we have seen for each target of a child
    protected final Map<TreeNode, double[]> childBestDistances = new HashMap<>();

    // State for Parent Aggregation
    private boolean isSubscribedToParent = false;

    public ProximityRoutingBroker(String name) { super(name); }
    public ProximityRoutingBroker(String name, Location p1, Location p2) { super(name, p1, p2); }

    // --- Topology & Quadrant Management ---

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child); 
        updateTopologicalTargets(child);
    }

    @Override
    public void updateRegion(TreeNode child) {
        super.updateRegion(child);
        // If a child's region expands, we must update its quadrant definitions
        updateTopologicalTargets(child);
    }

    private void updateTopologicalTargets(TreeNode child) {
        Location[] targets = null;

        if (child instanceof BoundedBroker) {
            // Child is a Broker: Use 4 Quadrants for better accuracy
            Region r = ((BoundedBroker) child).getRegion();
            if (r != null && r.getBottomLeft() != null) {
                targets = calculateQuadrants(r);
            } else if (r != null && r.getCenter() != null) {
                // Fallback: Region exists but might be a single point
                targets = new Location[] { r.getCenter() };
            }
        } else if (child instanceof SubscriberWithLocation) {
            // Child is a Subscriber: Use Exact Location
            targets = new Location[] { ((SubscriberWithLocation) child).getLocation() };
        }

        if (targets != null) {
            childTopologicalTargets.put(child, targets);
            // Initialize distances if new
            childBestDistances.putIfAbsent(child, initializeDistances(targets.length));
        }
    }

    // --- Subscription Handling ---

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s == null || s.getSource() == null) throw new IllegalArgumentException("Source cannot be null");
        
        inputStore.add(s);
        
        TreeNode child = s.getSource();
        
        // Ensure topology is synced (in case subscriber added without addChild)
        if (!childTopologicalTargets.containsKey(child)) {
            updateTopologicalTargets(child);
        }

        // RESET State: A new subscription means "I want the closest thing starting NOW."
        // We reset the best distances so the next publication (whatever it is) is considered an improvement.
        Location[] targets = childTopologicalTargets.get(child);
        if (targets != null) {
            childBestDistances.put(child, initializeDistances(targets.length));
        }
    }

    @Override
    public int getInputSubscriptionCount() { return inputStore.size(); }
    @Override
    public int getOutputSubscriptionCount() { return isSubscribedToParent ? 1 : 0; }
    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        if (getParentBroker() == null || !isSubscribedToParent) return Collections.emptyMap();
        
        SubscriptionWithLocation proxySub = new SubscriptionWithLocation(getRegion().getCenter());
        proxySub.setSource(this);
        
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        result.put(getParentBroker(), Collections.singletonList(proxySub));
        return result;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        addSubscription(s);
        
        // Aggregation: If not yet subscribed to parent, send ONE subscription (My Center)
        if (getParentBroker() != null && !isSubscribedToParent) {
            Location myLocation = getRegion().getCenter();
            if (myLocation == null) return; // Region not initialized

            SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(myLocation);
            proxySubscription.setSource(this);
            
            getParentBroker().processSubscription(proxySubscription);
            isSubscribedToParent = true;
        }
    }

    // --- Publication Routing (The Core Logic) ---

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
        // Iterate over specific targets for each neighbor
        for (Map.Entry<TreeNode, Location[]> entry : childTopologicalTargets.entrySet()) {
            TreeNode neighbor = entry.getKey();
            
            // 1. Don't send back to source
            if (neighbor == pub.getSource()) continue;
            
            // 2. Only route if they have an active subscription
            if (inputStore.get(neighbor) == null) continue;

            Location[] targets = entry.getValue();
            double[] bestDists = childBestDistances.get(neighbor);

            if (bestDists == null || targets == null || bestDists.length != targets.length) continue;

            boolean shouldSend = false;

            // 3. Check Quadrants/Points: Is this pub closer to ANY target than the previous best?
            for (int i = 0; i < targets.length; i++) {
                double newDistSq = pub.getLocation().distanceSquared(targets[i]);
                if (newDistSq < bestDists[i]) {
                    bestDists[i] = newDistSq; // Update state
                    shouldSend = true;
                }
            }

            if (shouldSend) {
                forwardPublication(neighbor, pub);
            }
        }
    }

    private void forwardPublication(TreeNode neighbor, PublicationWithLocation pub) {
        SimulationPublication copy = pub.getPublication();
        copy.setSource(this);
        copy.copyStateFrom(pub);
        copy.incrementHops();
        
        if (neighbor instanceof BoundedBroker) {
            ((BoundedBroker) neighbor).processPublication(copy);
        } else if (neighbor instanceof SubscriberWithLocation) {
            ((SubscriberWithLocation) neighbor).receive(copy);
        }
    }

    // --- Helpers ---

    private double[] initializeDistances(int size) {
        double[] dists = new double[size];
        for (int i = 0; i < size; i++) dists[i] = Double.MAX_VALUE;
        return dists;
    }

    private Location[] calculateQuadrants(Region r) {
        if (r == null) return new Location[0];
        // Simple Quadrant Center Calculation
        double minLon = r.getMinLon(); double maxLon = r.getMaxLon();
        double minLat = r.getMinLat(); double maxLat = r.getMaxLat();
        double midLon = (minLon + maxLon) / 2.0; 
        double midLat = (minLat + maxLat) / 2.0;

        // NOTE: Does not handle IDL wrapping for simplicity, assume standard regions for now
        return new Location[] {
            new Location((minLon + midLon)/2, (minLat + midLat)/2, 0), // Bottom-Left Q
            new Location((midLon + maxLon)/2, (minLat + midLat)/2, 0), // Bottom-Right Q
            new Location((minLon + midLon)/2, (midLat + maxLat)/2, 0), // Top-Left Q
            new Location((midLon + maxLon)/2, (midLat + maxLat)/2, 0)  // Top-Right Q
        };
    }
}