package simulator.regions;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.store.ListMultiRegionStore;
import simulator.regions.store.RegionSubscriptionStore;
import simulator.regions.store.SimpleRegionStore;
import simulator.regions.store.StoreOpResult;
import simulator.regions.store.StoreUpdate;
import simulator.regions.store.TreeMultiRegionStore;
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
        // Centralized Store Selection
        this.inputStore = createStore(forceSingleRegion, threshold);
        this.outputStore = createStore(forceSingleRegion, threshold);
    }

    public SpatialMatchBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name, p1, p2); 
        this.downwardPolicy = (policy != null) ? policy : new StrictPropagationPolicy();
        // Centralized Store Selection
        this.inputStore = createStore(forceSingleRegion, threshold);
        this.outputStore = createStore(forceSingleRegion, threshold);
    }

    private RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        if (forceSingleRegion) {
            return new SimpleRegionStore();
        }

        String impl = SimConfiguration.get().broker.storeImplementation;
        
        if ("LIST".equalsIgnoreCase(impl)) {
            return new ListMultiRegionStore(threshold);
        } else {
            // Default to TREE for "TREE" or any unknown value
            return new TreeMultiRegionStore(threshold);
        }
    }
    
    @Override public int getInputSubscriptionCount() { return inputStore.size(); }
    @Override public int getOutputSubscriptionCount() { return outputStore.size(); }
    @Override public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() { return inputStore.getAllSubscriptions(); }
    @Override public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() { return outputStore.getAllSubscriptions(); }

    @Override
    protected void handleSubscriptionProcessing(SimulationSubscription s) {
        if (!(s instanceof SubscriptionWithRegion newSub)) return;

        StoreUpdate inputUpdate = inputStore.addOrUpdate(s.getSource(), newSub);
        updateInputCounters(inputUpdate);

        if (SimConfiguration.get().paths.enableEventTracing) {
            logSubscriptionTrace(s, inputUpdate, newSub);
        }

        if (inputUpdate.getResult() != StoreOpResult.NO_CHANGE) {
            SubscriptionWithRegion aggregatedState = inputUpdate.getRegion();
            if (aggregatedState != null) {
                aggregatedState.setSource(s.getSource());
                aggregatedState.copyStateFrom(newSub);
                aggregatedState.incrementHops();
                
                if (s.getSource() != getParentBroker()) {
                    propagateSubscriptionUpward(aggregatedState);
                }
                propagateSubscriptionDownward(aggregatedState);
            }
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
             if (finalReg != null) {
                 SubscriptionWithRegion toSend = (SubscriptionWithRegion) finalReg.getSubscription();
                 toSend.setSource(this);
                 toSend.copyStateFrom(aggregatedState); 
                 parent.processSubscription(toSend);
             }
        }
    }

    private void propagateSubscriptionDownward(SubscriptionWithRegion aggregatedState) {
        SpatialRegion incomingRegion = aggregatedState.getRegion();
        for (TreeNode child : getChildren()) {
             if (child == aggregatedState.getSource()) continue;
             if (!(child instanceof BoundedBroker childBroker)) continue;

             SpatialRegion regionToSend = downwardPolicy.determineRegionToSend(childBroker, incomingRegion);
             
             if (regionToSend != null) {
                 Region concretePayload = new Region(regionToSend);
                 SubscriptionWithRegion candidate = new SubscriptionWithRegion(concretePayload);
                 
                 StoreUpdate update = outputStore.addOrUpdate(childBroker, candidate);
                 updateOutputCounters(update);
                 
                 if (update.isChange()) {
                     SubscriptionWithRegion finalReg = update.getRegion();
                     if (finalReg != null) {
                         SubscriptionWithRegion toSend = (SubscriptionWithRegion) finalReg.getSubscription();
                         toSend.setSource(this);
                         toSend.copyStateFrom(aggregatedState);
                         childBroker.processSubscription(toSend);
                     }
                 }
             }
        }
    }

    private void updateInputCounters(StoreUpdate update) {
        switch (update.getResult()) {
            case NO_CHANGE -> recordSubCovered();
            case EXPANDED -> { 
                recordSubExpanded(); 
                
                // Logic: 0 or 1 removal means the topology didn't "collapse".
                int removed = update.getAbsorbedCount() + update.getMergedCount();
                
                if (removed <= 1) {
                    recordSubSimpleExpanded(); 
                } else {
                    recordSubComplexExpanded(); // Record the EVENT
                    recordSubAbsorbed(update.getAbsorbedCount()); // Record the VICTIMS
                    recordSubMerged(update.getMergedCount()); 
                }
            }
            case ADDED -> recordSubAdded();
        }
    }

    private void updateOutputCounters(StoreUpdate update) {
        switch (update.getResult()) {
            case NO_CHANGE -> recordOutSubCovered();
            case EXPANDED -> { 
                recordOutSubExpanded(); 
                
                int removed = update.getAbsorbedCount() + update.getMergedCount();
                
                if (removed <= 1) {
                    recordOutSubSimpleExpanded(); 
                } else {
                    recordOutSubComplexExpanded(); // Record the EVENT
                    recordOutSubAbsorbed(update.getAbsorbedCount()); // Record the VICTIMS
                    recordOutSubMerged(update.getMergedCount());
                }
            }
            case ADDED -> recordOutSubAdded();
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (p instanceof PublicationWithLocation pub) {
            this.totalMatchingComputations += inputStore.size();
            List<TreeNode> matches = inputStore.findMatches(pub.getLocation());
            
            boolean forwardedToAny = false;

            for (TreeNode target : matches) {
                if (target == p.getSource()) continue;
                forwardPublicationToNode(p, target);
                forwardedToAny = true;
            }

            if (!forwardedToAny) this.totalFalsePositiveEvents++;

            if (SimConfiguration.get().paths.enableEventTracing) {
                logPublicationTrace(p, forwardedToAny);
            }
        }
        return null;
    }

    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        SimulationPublication forwardedCopy = p.getPublication();
        forwardedCopy.setSource(this);
        forwardedCopy.copyStateFrom(p);
        if (next instanceof BoundedBroker) forwardedCopy.incrementHops();
        if (next instanceof BoundedBroker broker) broker.processPublication(forwardedCopy);
        else if (next instanceof SubscriberWithLocation subscriber) subscriber.receive(forwardedCopy);
    }

    private void logSubscriptionTrace(SimulationSubscription s, StoreUpdate inputUpdate, SubscriptionWithRegion newSub) {
        CsvMetricWriter.getInstance().logSubscription(
            s, getName(), inputUpdate.getResult().name(), newSub.getRegion(), this.getRegion(), inputUpdate.getAdditionalInfo() 
        );
    }

    private void logPublicationTrace(SimulationPublication p, boolean forwardedToAny) {
        String status = forwardedToAny ? "Received" : "Received (Dead End)";
        CsvMetricWriter.getInstance().logPublication(p, getName(), status, this.getRegion());
    }
}