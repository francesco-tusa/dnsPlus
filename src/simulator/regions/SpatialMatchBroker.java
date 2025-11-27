package simulator.regions;

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
import simulator.events.metrics.EventMetrics;
import simulator.regions.store.MultiRegionStore;
import simulator.regions.store.RegionSubscriptionStore;
import simulator.regions.store.SimpleRegionStore;
import simulator.regions.store.StoreOpResult;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public class SpatialMatchBroker extends BoundedBroker {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchBroker.class.getName());
    private final RegionSubscriptionStore inputStore;
    private final RegionSubscriptionStore outputStore;

    public SpatialMatchBroker(String name, boolean forceSingleRegion, double threshold) {
        super(name);
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(threshold);
            this.outputStore = new MultiRegionStore(threshold);
        }
    }
    
    public SpatialMatchBroker(String name, Location p1, Location p2, boolean force, double thresh) {
        super(name, p1, p2);
        if (force) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(thresh);
            this.outputStore = new MultiRegionStore(thresh);
        }
    }

    @Override
    public int getSubscriptionCount() { return inputStore.size(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() { return outputStore.getAllSubscriptions(); }

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) throw new IllegalArgumentException("Source null");
        if (s.getMetrics() != null) {
            String rStr = (s instanceof SubscriptionWithRegion swr) ? swr.getRegion().toLogString() : "N/A";
            CsvMetricWriter.getInstance().logSubscription(s.getMetrics().getTraceId(), getName(), s.getSource().getName(), s.getMetrics().getHops(), rStr);
        }

        if (s instanceof SubscriptionWithRegion sub) {
            // Update Input Store & Counters
            StoreOpResult result = inputStore.addOrUpdate(s.getSource(), sub);
            switch (result) {
                case NO_CHANGE -> recordSubCovered();
                case EXPANDED -> recordSubExpanded();
                case ADDED -> recordSubAdded();
            }
        }
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        if (!(s instanceof SubscriptionWithRegion newSub)) return;
        addSubscription(s);
        if (s.getSource() != getParentBroker()) propagateSubscriptionUpward(newSub);
        propagateSubscriptionDownward(newSub);
    }

    private void propagateSubscriptionUpward(SubscriptionWithRegion newSub) {
        BoundedBroker parent = getParentBroker();
        if (parent == null) return;

        SubscriptionWithRegion candidate = new SubscriptionWithRegion(new Region(newSub.getRegion()));
        StoreOpResult result = outputStore.addOrUpdate(parent, candidate);

        // Propagate only if state changed (Added or Expanded)
        if (result != StoreOpResult.NO_CHANGE) {
            List<SimulationSubscription> outputs = outputStore.getOutputFor(parent);
            for(SimulationSubscription out : outputs) {
                 SubscriptionWithRegion toSend = (SubscriptionWithRegion) out.getSubscription();
                 toSend.setSource(this);
                 if (newSub.getMetrics() != null) {
                     EventMetrics m = new EventMetrics(newSub.getMetrics());
                     m.incrementHops();
                     toSend.setMetrics(m);
                 }
                 parent.processSubscription(toSend);
            }
        }
    }

    private void propagateSubscriptionDownward(SubscriptionWithRegion newSub) {
        for (TreeNode child : getChildren()) {
             if (child == newSub.getSource()) continue;
             if (!(child instanceof BoundedBroker childBroker)) continue;

             if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                 
                 SubscriptionWithRegion candidate = new SubscriptionWithRegion(new Region(newSub.getRegion()));
                 StoreOpResult result = outputStore.addOrUpdate(childBroker, candidate);
                 
                 if (result != StoreOpResult.NO_CHANGE) {
                     List<SimulationSubscription> outputs = outputStore.getOutputFor(childBroker);
                     for(SimulationSubscription out : outputs) {
                         SubscriptionWithRegion toSend = (SubscriptionWithRegion) out.getSubscription();
                         toSend.setSource(this);
                         if (newSub.getMetrics() != null) {
                             EventMetrics m = new EventMetrics(newSub.getMetrics());
                             m.incrementHops();
                             toSend.setMetrics(m);
                         }
                         childBroker.processSubscription(toSend);
                     }
                 }
             }
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p instanceof PublicationWithLocation pub) {
            
            // Deterministic Cost Metric: 
            // We assume cost = number of disjoint regions checked.
            // This is equivalent to the size of the store (O(N) scan).
            this.totalMatchingComputations += inputStore.size();
            
            List<TreeNode> matches = inputStore.findMatches(pub.getLocation());
            for (TreeNode target : matches) {
                if (target == p.getSource()) continue;
                forwardPublicationToNode(p, target);
            }
        }
        return null;
    }

    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof BoundedBroker broker) {
             SimulationPublication forwardedCopy = p.getPublication();
             forwardedCopy.setSource(this);
             if (p.getMetrics() != null) {
                EventMetrics copiedMetrics = new EventMetrics(p.getMetrics());
                copiedMetrics.incrementHops();
                forwardedCopy.setMetrics(copiedMetrics);
            }
            broker.processPublication(forwardedCopy);
            
        } else if (next instanceof SubscriberWithLocation subscriber) {
            subscriber.receive(p);
        }
    }
}