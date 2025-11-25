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
    
    public SpatialMatchBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double thresh) {
        super(name, p1, p2);
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(thresh);
            this.outputStore = new MultiRegionStore(thresh);
        }
    }

    public Map<TreeNode, SimulationSubscription> getPropagatedSubscriptions() {
        Map<TreeNode, SimulationSubscription> result = new HashMap<>();
        Map<TreeNode, List<SubscriptionWithRegion>> all = outputStore.getAllSubscriptions();
        
        for (Map.Entry<TreeNode, List<SubscriptionWithRegion>> entry : all.entrySet()) {
            List<SubscriptionWithRegion> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                result.put(entry.getKey(), list.get(0));
            }
        }
        return result;
    }

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) throw new IllegalArgumentException("Source null");
        if (s.getMetrics() != null) {
            String rStr = (s instanceof SubscriptionWithRegion swr) ? swr.getRegion().toLogString() : "N/A";
            CsvMetricWriter.getInstance().logSubscription(s.getMetrics().getTraceId(), getName(), s.getSource().getName(), s.getMetrics().getHops(), rStr);
        }

        if (s instanceof SubscriptionWithRegion sub) {
            boolean changed = inputStore.addOrUpdate(s.getSource(), sub);
            if (changed) incrementMainTableExpansions();
        } else {
            getSubscriptionsTable().put(s.getSource(), s);
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
        boolean changed = outputStore.addOrUpdate(parent, candidate);

        if (changed) {
            incrementPropagationFilterExpansions();
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
                 boolean changed = outputStore.addOrUpdate(childBroker, candidate);
                 
                 if (changed) {
                     incrementPropagationFilterExpansions();
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
        // We only send if there is a matching subscription.
        // Since subscriptions propagate from Root to Leaves (and vice-versa),
        // the inputStore will contain the Parent IF the Parent is interested.
        
        if (p instanceof PublicationWithLocation pub) {
            List<TreeNode> matches = inputStore.findMatches(pub.getLocation());
            for (TreeNode target : matches) {
                // Ensure we don't send it back to where it came from
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