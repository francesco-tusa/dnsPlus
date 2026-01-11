package simulator.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.policy.BrakeStrategy;
import simulator.regions.policy.DecayingCounterBrakeStrategy;
import simulator.regions.policy.NoOpBrakeStrategy;
import simulator.regions.store.BasicSubscriptionStore;
import utils.CustomLogger;

public class ProximityRoutingBroker extends BoundedBroker {

    private static final Logger logger = CustomLogger.getLogger(ProximityRoutingBroker.class.getName());

    protected final BasicSubscriptionStore inputStore = new BasicSubscriptionStore();
    
    // --- Brake Strategy ---
    private BrakeStrategy brakeStrategy;
    
    // --- High-Performance Routing State ---
    protected final Map<TreeNode, Location[]> childTopologicalTargets = new HashMap<>();
    protected final Map<TreeNode, double[]> childBestDistances = new HashMap<>();

    private boolean isSubscribedToParent = false;

    public ProximityRoutingBroker(String name) { 
        super(name); 
        init();
    }
    
    public ProximityRoutingBroker(String name, Location p1, Location p2) { 
        super(name, p1, p2); 
        init();
    }

    private void init() {
        BrokerConfig config = SimConfiguration.get().broker;
        if (config.proximityBrakeEnabled) {
            this.brakeStrategy = new DecayingCounterBrakeStrategy(
                config.proximityBrakeLimit,
                config.proximityBrakeIntervalMs
            );
        } else {
            this.brakeStrategy = new NoOpBrakeStrategy();
        }
    }
    
    public void setBrakeStrategy(BrakeStrategy strategy) {
        this.brakeStrategy = strategy;
    }

    // --- Topology & Quadrant Management ---

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child); 
        updateTopologicalTargets(child);
    }

    @Override
    public void updateRegion(TreeNode child) {
        super.updateRegion(child);
        updateTopologicalTargets(child);
    }

    private void updateTopologicalTargets(TreeNode child) {
        Location[] targets = null;

        if (child instanceof BoundedBroker) {
            Region r = ((BoundedBroker) child).getRegion();
            if (r != null && r.getBottomLeft() != null) {
                targets = calculateQuadrants(r);
            } else if (r != null && r.getCenter() != null) {
                targets = new Location[] { r.getCenter() };
            }
        } else if (child instanceof SubscriberWithLocation) {
            targets = new Location[] { ((SubscriberWithLocation) child).getLocation() };
        }

        if (targets != null) {
            childTopologicalTargets.put(child, targets);
            // Only reset distances if strictly necessary to avoid thrashing
            childBestDistances.putIfAbsent(child, initializeDistances(targets.length));
        }
    }

    // --- Subscription Handling ---

    @Override
    public int getInputSubscriptionCount() { return inputStore.size(); }
    @Override
    public int getOutputSubscriptionCount() { return isSubscribedToParent ? 1 : 0; }
    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        if (getParentBroker() == null || !isSubscribedToParent) return Collections.emptyMap();
        
        Region r = getRegion();
        Location center = (r != null) ? r.getCenter() : null;
        // If region is not ready, we cannot report a valid subscription yet
        if (center == null) return Collections.emptyMap();

        SubscriptionWithLocation proxySub = new SubscriptionWithLocation(center);
        proxySub.setSource(this);
        
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        result.put(getParentBroker(), Collections.singletonList(proxySub));
        return result;
    }

    @Override
    protected void handleSubscriptionProcessing(SimulationSubscription s) {        
        // 1. Store the subscription (Crucial for Downward Routing)
        inputStore.add(s);
        
        // 2. Update Topological Routing Tables
        TreeNode child = s.getSource();
        if (!childTopologicalTargets.containsKey(child)) {
            updateTopologicalTargets(child);
        }

        // Reset distances to ensure the new subscription captures the next relevant publication
        Location[] targets = childTopologicalTargets.get(child);
        if (targets != null) {
            childBestDistances.put(child, initializeDistances(targets.length));
        }

        // 3. Propagate Upward (if not already done)
        if (getParentBroker() != null && !isSubscribedToParent) {
            Region r = getRegion();
            Location myLocation = (r != null) ? r.getCenter() : null;
            
            // Cannot subscribe upwards without a valid location/region
            if (myLocation == null) return; 

            SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(myLocation);
            proxySubscription.setSource(this);
            
            getParentBroker().processSubscription(proxySubscription);
            isSubscribedToParent = true;
        }
    }

    // --- Publication Routing ---

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p.getSource() != getParentBroker()) propagatePublicationUpward(p);
        if (p instanceof PublicationWithLocation pub) processPublicationDownward(pub);
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BoundedBroker parentBroker = getParentBroker();
        if (parentBroker == null) return;

        // --- BRAKE LOGIC ---
        if (p instanceof PublicationWithLocation) {
            PublicationWithLocation pub = (PublicationWithLocation) p;
            Region r = getRegion();
            
            // Only apply brake if we have a valid region context
            if (r != null && r.getCenter() != null) {
                long now = System.currentTimeMillis();
                boolean allowed = brakeStrategy.shouldPropagate(pub.getLocation(), r.getCenter(), now);
                if (!allowed) {
                    return; // Dropped by Brake
                }
            }
        }

        SimulationPublication forwardedCopy = p.getPublication();
        forwardedCopy.setSource(this);
        forwardedCopy.copyStateFrom(p);
        forwardedCopy.incrementHops();
        parentBroker.processPublication(forwardedCopy);
    }

    protected void processPublicationDownward(PublicationWithLocation pub) {
        for (Map.Entry<TreeNode, Location[]> entry : childTopologicalTargets.entrySet()) {
            TreeNode neighbor = entry.getKey();
            
            // 1. Don't send back to source
            if (neighbor == pub.getSource()) continue;

            // 2. Don't send "downward" to the Parent.
            // The parent path is handled exclusively by propagatePublicationUpward.
            if (neighbor == getParentBroker()) continue;
            
            // 3. Only route if they have an active subscription
            // (This now works because propagateSubscription correctly populated inputStore)
            if (inputStore.get(neighbor) == null) continue;

            Location[] targets = entry.getValue();
            double[] bestDists = childBestDistances.get(neighbor);

            if (bestDists == null || targets == null || bestDists.length != targets.length) continue;

            boolean shouldSend = false;

            for (int i = 0; i < targets.length; i++) {
                double newDistSq = pub.getLocation().distanceSquared(targets[i]);
                if (newDistSq < bestDists[i]) {
                    bestDists[i] = newDistSq;
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

    private double[] initializeDistances(int size) {
        double[] dists = new double[size];
        for (int i = 0; i < size; i++) dists[i] = Double.MAX_VALUE;
        return dists;
    }

    private Location[] calculateQuadrants(Region r) {
        if (r == null) return new Location[0];
        double minLon = r.getMinLon(); double maxLon = r.getMaxLon();
        double minLat = r.getMinLat(); double maxLat = r.getMaxLat();
        double midLon = (minLon + maxLon) / 2.0; 
        double midLat = (minLat + maxLat) / 2.0;

        return new Location[] {
            new Location((minLon + midLon)/2, (minLat + midLat)/2, 0), 
            new Location((midLon + maxLon)/2, (minLat + midLat)/2, 0),
            new Location((minLon + midLon)/2, (midLat + maxLat)/2, 0),
            new Location((midLon + maxLon)/2, (midLat + maxLat)/2, 0)
        };
    }
}