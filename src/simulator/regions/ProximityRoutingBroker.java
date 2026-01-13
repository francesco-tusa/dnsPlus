package simulator.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.events.metrics.EventMetrics;
import simulator.regions.policy.BrakeStrategy;
import simulator.regions.policy.DecayingCounterBrakeStrategy;
import simulator.regions.policy.NoOpBrakeStrategy;
import simulator.regions.store.BasicSubscriptionStore;
import utils.CsvMetricWriter;

public class ProximityRoutingBroker extends BoundedBroker {

    protected final BasicSubscriptionStore inputStore = new BasicSubscriptionStore();
    private BrakeStrategy brakeStrategy;
    private long brakeFilteredCount = 0;
    protected final Map<TreeNode, Location[]> childTopologicalTargets = new HashMap<>();
    protected final Map<TreeNode, double[]> childBestDistances = new HashMap<>();
    protected long totalMessagesForwarded = 0;
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
                    config.proximityBrakeLimit, config.proximityBrakeIntervalMs);
        } else {
            this.brakeStrategy = new NoOpBrakeStrategy();
        }
    }

    public void setBrakeStrategy(BrakeStrategy strategy) {
        this.brakeStrategy = strategy;
    }

    public long getBrakeFilteredCount() {
        return brakeFilteredCount;
    }

    public long getTotalMessagesForwarded() {
        return totalMessagesForwarded;
    }

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
            if (r != null && r.getBottomLeft() != null)
                targets = calculateQuadrants(r);
            else if (r != null && r.getCenter() != null)
                targets = new Location[] { r.getCenter() };
        } else if (child instanceof SubscriberWithLocation) {
            targets = new Location[] { ((SubscriberWithLocation) child).getLocation() };
        }
        if (targets != null) {
            childTopologicalTargets.put(child, targets);
            childBestDistances.putIfAbsent(child, initializeDistances(targets.length));
        }
    }

    @Override
    public int getInputSubscriptionCount() {
        return inputStore.size();
    }

    @Override
    public int getOutputSubscriptionCount() {
        return isSubscribedToParent ? 1 : 0;
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() {
        return inputStore.getAllSubscriptions();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        if (getParentBroker() == null || !isSubscribedToParent)
            return Collections.emptyMap();
        Region r = getRegion();
        Location center = (r != null) ? r.getCenter() : null;
        if (center == null)
            return Collections.emptyMap();

        SubscriptionWithLocation proxySub = new SubscriptionWithLocation(center);
        proxySub.setSource(this);
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        result.put(getParentBroker(), Collections.singletonList(proxySub));
        return result;
    }

    @Override
    protected void handleSubscriptionProcessing(SimulationSubscription s) {
        s.incrementHops();
        inputStore.add(s);

        if (SimConfiguration.get().paths.enableEventTracing) {
            logSubscriptionInput(s);
        }

        TreeNode child = s.getSource();
        if (!childTopologicalTargets.containsKey(child))
            updateTopologicalTargets(child);

        Location[] targets = childTopologicalTargets.get(child);
        if (targets != null)
            childBestDistances.put(child, initializeDistances(targets.length));

        propagateSubscriptionUpward(s);
    }

    private void propagateSubscriptionUpward(SimulationSubscription originalSub) {
        if (getParentBroker() == null || isSubscribedToParent)
            return;
        Region r = getRegion();
        Location myCenter = (r != null) ? r.getCenter() : null;
        if (myCenter == null)
            return;

        SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(myCenter);
        proxySubscription.setSource(this);
        proxySubscription.setHops(originalSub.getHops());
        if (originalSub.getMetrics() != null) {
            proxySubscription.setMetrics(new EventMetrics(originalSub.getMetrics().getTraceId()));
        }

        if (SimConfiguration.get().paths.enableEventTracing) {
            logSubscriptionOutput(proxySubscription);
        }

        getParentBroker().processSubscription(proxySubscription);
        isSubscribedToParent = true;
        recordOutSubAdded();
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p.getSource() != getParentBroker())
            propagatePublicationUpward(p);
        if (p instanceof PublicationWithLocation pub)
            processPublicationDownward(pub);
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BoundedBroker parentBroker = getParentBroker();
        if (parentBroker == null)
            return;
        if (p instanceof PublicationWithLocation) {
            PublicationWithLocation pub = (PublicationWithLocation) p;
            Region r = getRegion();
            if (r != null && r.getCenter() != null) {
                long now = System.currentTimeMillis();
                boolean allowed = brakeStrategy.shouldPropagate(pub.getLocation(), r.getCenter(), now);
                if (!allowed) {
                    brakeFilteredCount++;
                    return;
                }
            }
        }
        SimulationPublication forwardedCopy = p.getPublication();
        forwardedCopy.setSource(this);
        forwardedCopy.copyStateFrom(p);
        forwardedCopy.incrementHops();
        parentBroker.processPublication(forwardedCopy);
    }

    private void processPublicationDownward(PublicationWithLocation pub) {
        boolean forwardedToAny = false;
        boolean tracingEnabled = SimConfiguration.get().paths.enableEventTracing && pub.getMetrics() != null;
        double minDistSqToInterestedChild = tracingEnabled ? Double.MAX_VALUE : -1.0;
        int potentialRecipients = 0;
        int actualRecipients = 0;

        for (Map.Entry<TreeNode, Location[]> entry : childTopologicalTargets.entrySet()) {
            TreeNode neighbor = entry.getKey();
            if (neighbor == pub.getSource())
                continue;
            if (neighbor == getParentBroker())
                continue;
            if (inputStore.get(neighbor) == null)
                continue;

            if (tracingEnabled)
                potentialRecipients++;
            Location[] targets = entry.getValue();
            double[] bestDists = childBestDistances.get(neighbor);

            if (bestDists == null || targets == null || bestDists.length != targets.length)
                continue;
            boolean shouldSend = false;
            double distanceForThisNeighbor = -1.0;

            for (int i = 0; i < targets.length; i++) {
                this.totalMatchingComputations++;
                double newDistSq = pub.getLocation().distanceSquared(targets[i]);
                double currentBest = bestDists[i];
                if (tracingEnabled && newDistSq < minDistSqToInterestedChild)
                    minDistSqToInterestedChild = newDistSq;
                if (targets.length == 1)
                    distanceForThisNeighbor = newDistSq;
                if (newDistSq < currentBest) {
                    bestDists[i] = newDistSq;
                    shouldSend = true;
                }
            }
            if (shouldSend) {
                forwardPublication(neighbor, pub, distanceForThisNeighbor);
                forwardedToAny = true;
                if (tracingEnabled)
                    actualRecipients++;
            }
        }
        if (!forwardedToAny)
            this.totalFalsePositiveEvents++;
        if (tracingEnabled)
            logPublicationTrace(pub, forwardedToAny, minDistSqToInterestedChild, actualRecipients, potentialRecipients);
    }

    private void forwardPublication(TreeNode neighbor, PublicationWithLocation pub, double distSq) {
        this.totalMessagesForwarded++;
        SimulationPublication abstractCopy = pub.getPublication();
        if (abstractCopy instanceof PublicationWithLocation) {
            PublicationWithLocation copy = (PublicationWithLocation) abstractCopy;
            copy.setSource(this);
            copy.copyStateFrom(pub);
            copy.incrementHops();
            if (distSq >= 0)
                copy.setCachedDistanceSquared(distSq);
            if (neighbor instanceof BoundedBroker)
                ((BoundedBroker) neighbor).processPublication(copy);
            else if (neighbor instanceof SubscriberWithLocation)
                ((SubscriberWithLocation) neighbor).receive(copy);
        }
    }

    // --- Logging Helpers ---

    private void logSubscriptionInput(SimulationSubscription s) {
        Location loc = (s instanceof SubscriptionWithLocation sl) ? sl.getLocation() : null;
        boolean willPropagate = (getParentBroker() != null && !isSubscribedToParent);
        String decision = willPropagate ? "Forward" : "Covered";

        CsvMetricWriter.getInstance().logSubscription(
                s,
                getName(),
                "INPUT",
                loc,
                decision);
    }

    private void logSubscriptionOutput(SubscriptionWithLocation proxySub) {
        Location center = proxySub.getLocation();

        CsvMetricWriter.getInstance().logSubscription(
                proxySub,
                getName(),
                "OUTPUT",
                center,
                "Proxy_Forward");
    }

    private void logPublicationTrace(PublicationWithLocation pub, boolean forwardedToAny, double minDistSq, int actual,
            int potential) {
        String status = forwardedToAny ? "FORWARDED" : "PRUNED";
        if (!forwardedToAny && getParentBroker() == null && childTopologicalTargets.isEmpty())
            status = "RECEIVED_ROOT";
        String payload;
        if (minDistSq < Double.MAX_VALUE) {
            double distKm = Math.sqrt(minDistSq) * 111.1;
            payload = String.format("MinDist:%.0fkm Upd:%d/%d", distKm, actual, potential);
        } else {
            payload = "No Interest";
        }
        CsvMetricWriter.getInstance().logPublication(pub, getName(), payload, status);
    }

    private double[] initializeDistances(int size) {
        double[] dists = new double[size];
        for (int i = 0; i < size; i++)
            dists[i] = Double.MAX_VALUE;
        return dists;
    }

    private Location[] calculateQuadrants(Region r) {
        if (r == null)
            return new Location[0];
        double minLon = r.getMinLon();
        double maxLon = r.getMaxLon();
        double minLat = r.getMinLat();
        double maxLat = r.getMaxLat();
        double midLon = (minLon + maxLon) / 2.0;
        double midLat = (minLat + maxLat) / 2.0;
        return new Location[] {
                new Location((minLon + midLon) / 2, (minLat + midLat) / 2, 0),
                new Location((midLon + maxLon) / 2, (minLat + midLat) / 2, 0),
                new Location((minLon + midLon) / 2, (midLat + maxLat) / 2, 0),
                new Location((midLon + maxLon) / 2, (midLat + maxLat) / 2, 0)
        };
    }
}