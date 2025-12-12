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
    
    // Policy determines if/how we clip the region before sending it downwards
    private final PropagationRegionPolicy downwardPolicy;

    /**
     * Primary Constructor used by the Factory.
     */
    public SpatialMatchBroker(String name, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name);
        // Default to Strict (Algorithm compliant) if null
        this.downwardPolicy = (policy != null) ? policy : new StrictPropagationPolicy();
        
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(threshold);
            this.outputStore = new MultiRegionStore(threshold);
        }
    }

    /**
     * Constructor for explicit bounds (p1, p2) with Policy support.
     * Required by SpatialMatchLeafBroker.
     */
    public SpatialMatchBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name, p1, p2); // Initializes BoundedBroker with explicit region
        this.downwardPolicy = (policy != null) ? policy : new StrictPropagationPolicy();
        
        if (forceSingleRegion) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(threshold);
            this.outputStore = new MultiRegionStore(threshold);
        }
    }
    
    // --- Legacy Constructors (Delegating to Primary) ---

    public SpatialMatchBroker(String name, boolean forceSingleRegion, double threshold) {
        this(name, forceSingleRegion, threshold, new StrictPropagationPolicy());
    }
    
    public SpatialMatchBroker(String name, Location p1, Location p2, boolean force, double thresh) {
        super(name, p1, p2);
        this.downwardPolicy = new StrictPropagationPolicy();
        if (force) {
            this.inputStore = new SimpleRegionStore();
            this.outputStore = new SimpleRegionStore();
        } else {
            this.inputStore = new MultiRegionStore(thresh);
            this.outputStore = new MultiRegionStore(thresh);
        }
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

        // Logic split: We need to perform the store update AND log the result.
        // In propagateSubscription, we do the update earlier to get the aggregate.
        // Here, we do standard processing for non-propagating calls (if any).
        
        StoreOpResult resultForLog = StoreOpResult.NO_CHANGE;
        String logDetail = "";

        if (s instanceof SubscriptionWithRegion sub) {
            StoreUpdate update = inputStore.addOrUpdate(s.getSource(), sub);
            resultForLog = update.getResult();
            
            // Re-use logic for logging details
            logDetail = buildLogDetail(sub, update);
            
            updateCounters(resultForLog);
        }
        
        logToCsv(s, logDetail, resultForLog);
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        if (!(s instanceof SubscriptionWithRegion newSub)) return;

        // 1. Update Input Table & Retrieve the AGGREGATE State
        StoreUpdate inputUpdate = inputStore.addOrUpdate(s.getSource(), newSub);
        SubscriptionWithRegion aggregatedState = inputUpdate.getRegion();

        // We must tell the aggregate who sent the original update 
        // so the downward propagation loop knows who to skip.
        aggregatedState.setSource(s.getSource());

        // 2. Logging & Metrics (Using the helpers present in your class)
        String logDetail = buildLogDetail(newSub, inputUpdate);
        updateCounters(inputUpdate.getResult());
        logToCsv(s, logDetail, inputUpdate.getResult());

        // 3. Propagation Logic
        if (inputUpdate.getResult() != StoreOpResult.NO_CHANGE) {
            
            // Preserve/Update metrics for the propagated event
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

        // Upward propagation always sends the FULL Aggregate state.
        // We clone the Region to ensure the message payload is independent of the Store's internal object.
        Region regionPayload = new Region(aggregatedState.getRegion());
        SubscriptionWithRegion candidate = new SubscriptionWithRegion(regionPayload);
        
        StoreUpdate update = outputStore.addOrUpdate(parent, candidate);

        if (update.isChange()) {
             SubscriptionWithRegion finalReg = update.getRegion();
             SubscriptionWithRegion toSend = (SubscriptionWithRegion) finalReg.getSubscription();
             toSend.setSource(this);
             
             // Copy metrics from the trigger event
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

             // POLYMORPHIC LOGIC: Determine what to send based on the Policy.
             // Arguments: (Child's Region, Parent's Global Aggregate Need)
             // Returns: The SpatialRegion to send (full, clipped, or null)
             SpatialRegion regionToSend = downwardPolicy.determineRegionToSend(childBroker.getRegion(), aggregatedState.getRegion());
             
             if (regionToSend != null) {
                 // Convert the abstract SpatialRegion back to a concrete Region for the message payload
                 Region concretePayload = new Region(regionToSend);
                 SubscriptionWithRegion candidate = new SubscriptionWithRegion(concretePayload);
                 
                 StoreUpdate update = outputStore.addOrUpdate(childBroker, candidate);
                 
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

    // --- Helper Methods for Logging ---

    private String buildLogDetail(SubscriptionWithRegion sub, StoreUpdate update) {
        return String.format("Broker: %s; Incoming: %s; %s", 
                              this.getRegion().toLogString(), 
                              sub.getRegion().toLogString(), 
                              update.getAdditionalInfo());
    }

    private void updateCounters(StoreOpResult result) {
        switch (result) {
            case NO_CHANGE -> recordSubCovered();
            case EXPANDED -> recordSubExpanded();
            case ADDED -> recordSubAdded();
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