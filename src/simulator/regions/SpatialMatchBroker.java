package simulator.regions;

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
import simulator.regions.store.StoreUpdate;
import simulator.regions.policy.PropagationRegionPolicy;
import simulator.regions.policy.StrictPropagationPolicy;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public class SpatialMatchBroker extends BoundedBroker {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchBroker.class.getName());
    private final RegionSubscriptionStore inputStore;
    private final RegionSubscriptionStore outputStore;
    
    private final PropagationRegionPolicy downwardPolicy;

    public SpatialMatchBroker(String name, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name);
        this.downwardPolicy = (policy != null) ? policy : new StrictPropagationPolicy();
        
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(threshold);
            this.outputStore = new MultiRegionStore(threshold);
        }
    }

    public SpatialMatchBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name, p1, p2); 
        this.downwardPolicy = (policy != null) ? policy : new StrictPropagationPolicy();
        
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(threshold);
            this.outputStore = new MultiRegionStore(threshold);
        }
    }
    
    // --- Legacy Constructors ---
    public SpatialMatchBroker(String name, boolean forceSingleRegion, double threshold) {
        this(name, forceSingleRegion, threshold, new StrictPropagationPolicy());
    }
    
    public SpatialMatchBroker(String name, Location p1, Location p2, boolean force, double thresh) {
        this(name, p1, p2, force, thresh, new StrictPropagationPolicy());
    }

    @Override
    public int getInputSubscriptionCount() { return inputStore.size(); }
    @Override
    public int getOutputSubscriptionCount() { return outputStore.size(); }
    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }
    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() { return outputStore.getAllSubscriptions(); }

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) throw new IllegalArgumentException("Source null");

        StoreOpResult resultForLog = StoreOpResult.NO_CHANGE;
        String logDetail = "";

        if (s instanceof SubscriptionWithRegion sub) {
            StoreUpdate update = inputStore.addOrUpdate(s.getSource(), sub);
            resultForLog = update.getResult();
            logDetail = buildLogDetail(sub, update);
            
            updateInputCounters(update);
        }
        
        logToCsv(s, logDetail, resultForLog);
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        if (!(s instanceof SubscriptionWithRegion newSub)) return;

        // 1. Update Input Table
        StoreUpdate inputUpdate = inputStore.addOrUpdate(s.getSource(), newSub);
        SubscriptionWithRegion aggregatedState = inputUpdate.getRegion();

        aggregatedState.setSource(s.getSource());

        // 2. Metrics (Input)
        String logDetail = buildLogDetail(newSub, inputUpdate);
        updateInputCounters(inputUpdate);
        logToCsv(s, logDetail, inputUpdate.getResult());

        // 3. Propagation Logic
        if (inputUpdate.getResult() != StoreOpResult.NO_CHANGE) {
            if (newSub.getMetrics() != null) {
                 EventMetrics m = new EventMetrics(newSub.getMetrics());
                 m.incrementHops();
                 aggregatedState.setMetrics(m);
            }

            if (s.getSource() != getParentBroker()) {
                propagateSubscriptionUpward(aggregatedState);
            }
            propagateSubscriptionDownward(aggregatedState);
        }
    }

    private void propagateSubscriptionUpward(SubscriptionWithRegion aggregatedState) {
        BoundedBroker parent = getParentBroker();
        if (parent == null) return;

        Region regionPayload = new Region(aggregatedState.getRegion());
        SubscriptionWithRegion candidate = new SubscriptionWithRegion(regionPayload);
        
        StoreUpdate update = outputStore.addOrUpdate(parent, candidate);
        updateOutputCounters(update);

        if (update.isChange()) {
             SubscriptionWithRegion finalReg = update.getRegion();
             SubscriptionWithRegion toSend = (SubscriptionWithRegion) finalReg.getSubscription();
             toSend.setSource(this);
             
             if (aggregatedState.getMetrics() != null) {
                 toSend.setMetrics(aggregatedState.getMetrics());
             }
             
             parent.processSubscription(toSend);
        }
    }

    private void propagateSubscriptionDownward(SubscriptionWithRegion aggregatedState) {
        for (TreeNode child : getChildren()) {
             if (child == aggregatedState.getSource()) continue;
             if (!(child instanceof BoundedBroker childBroker)) continue;

             SpatialRegion regionToSend = downwardPolicy.determineRegionToSend(childBroker.getRegion(), aggregatedState.getRegion());
             
             if (regionToSend != null) {
                 Region concretePayload = new Region(regionToSend);
                 SubscriptionWithRegion candidate = new SubscriptionWithRegion(concretePayload);
                 
                 StoreUpdate update = outputStore.addOrUpdate(childBroker, candidate);
                 updateOutputCounters(update);
                 
                 if (update.isChange()) {
                     SubscriptionWithRegion finalReg = update.getRegion();
                     SubscriptionWithRegion toSend = (SubscriptionWithRegion) finalReg.getSubscription();
                     toSend.setSource(this);
                     
                     if (aggregatedState.getMetrics() != null) {
                         toSend.setMetrics(aggregatedState.getMetrics());
                     }
                     
                     childBroker.processSubscription(toSend);
                 }
             }
        }
    }

    private String buildLogDetail(SubscriptionWithRegion sub, StoreUpdate update) {
        return String.format("Broker: %s; Incoming: %s; %s", 
                              this.getRegion().toLogString(), 
                              sub.getRegion().toLogString(), 
                              update.getAdditionalInfo());
    }

    // --- Metric Updaters ---

    private void updateInputCounters(StoreUpdate update) {
        switch (update.getResult()) {
            case NO_CHANGE -> recordSubCovered();
            case EXPANDED -> {
                recordSubExpanded();
                recordSubAbsorbed(update.getAbsorbedCount());
                recordSubMerged(update.getMergedCount());
            }
            case ADDED -> recordSubAdded();
        }
    }

    private void updateOutputCounters(StoreUpdate update) {
        switch (update.getResult()) {
            case NO_CHANGE -> recordOutSubCovered();
            case EXPANDED -> {
                recordOutSubExpanded();
                recordOutSubAbsorbed(update.getAbsorbedCount());
                recordOutSubMerged(update.getMergedCount());
            }
            case ADDED -> recordOutSubAdded();
        }
    }

    private void logToCsv(SimulationSubscription s, String logDetail, StoreOpResult result) {
        if (s.getMetrics() != null) {
            CsvMetricWriter.getInstance().logSubscription(
                s.getMetrics().getTraceId(), 
                getName(), 
                s.getSource().getName(), 
                s.getMetrics().getHops(), 
                logDetail, 
                result.name() 
            );
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p instanceof PublicationWithLocation pub) {
            this.totalMatchingComputations += inputStore.size();
            List<TreeNode> matches = inputStore.findMatches(pub.getLocation());
            int usefulForwards = 0;
            for (TreeNode target : matches) {
                if (target == p.getSource()) continue;
                forwardPublicationToNode(p, target);
                usefulForwards++;
            }
            if (usefulForwards == 0) {
                this.totalFalsePositiveEvents++;
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