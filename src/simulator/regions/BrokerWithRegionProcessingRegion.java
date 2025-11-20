package simulator.regions;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    private static final Logger logger = CustomLogger.getLogger(BrokerWithRegionProcessingRegion.class.getName());

    private final Map<TreeNode, SimulationSubscription> propagatedSubscriptions = new HashMap<>();
    private final RegionSubscriptionManager inputView;
    private final RegionSubscriptionManager outputView;

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
        this.inputView = new RegionSubscriptionManager(getSubscriptionsTable());
        this.outputView = new RegionSubscriptionManager(this.propagatedSubscriptions);
    }

    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
        this.inputView = new RegionSubscriptionManager(getSubscriptionsTable());
        this.outputView = new RegionSubscriptionManager(this.propagatedSubscriptions);
    }

    public Map<TreeNode, SimulationSubscription> getPropagatedSubscriptions() {
        return propagatedSubscriptions;
    }

    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) throw new IllegalArgumentException("Subscription source cannot be null");

        // 1. STREAMING LOG: Log Arrival Immediately (No Timestamp)
        if (s.getMetrics() != null) {
            String regionStr = (s instanceof SubscriptionWithRegion swr) ? swr.getRegion().toLogString() : "N/A";
            CsvMetricWriter.getInstance().logSubscription(
                s.getMetrics().getTraceId(),
                this.getName(),
                s.getSource().getName(),
                s.getMetrics().getHops(),
                regionStr
            );
        }

        if (s instanceof SubscriptionWithRegion sub) {
            boolean changed = inputView.updateOrExpand(s.getSource(), sub);
            if (changed) {
                incrementMainTableExpansions();
            }
        } else {
            getSubscriptionsTable().put(s.getSource(), s);
        }
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        String sourceName = (s.getSource() != null) ? s.getSource().getName() : "NULL_SOURCE";
        String regionStr = (s instanceof SubscriptionWithRegion swr) ? swr.getRegion().toLogString() : "N/A";
        
        logger.fine(String.format("%s (Region: %s): processing a subscription received from %s",
                getName(), regionStr, sourceName));

        if (!(s instanceof SubscriptionWithRegion newSub)) {
            return;
        }

        addSubscription(newSub); 

        if (s.getSource() != getParentBroker()) {
            propagateSubscriptionUpward(newSub);
        }
        propagateSubscriptionDownward(newSub);
    }

    private void propagateSubscriptionUpward(SubscriptionWithRegion newSub) {
        BrokerWithRegion parent = getParentBroker();
        if (parent == null) return;
        
        SubscriptionWithRegion stateUpdate = new SubscriptionWithRegion(new Region(newSub.getRegion()));
        stateUpdate.setSource(this);

        boolean changed = outputView.updateOrExpand(parent, stateUpdate);
        
        if (changed) {
            incrementPropagationFilterExpansions(); 
            logger.fine(String.format("%s: Region expanded. Propagating update to parent %s.", getName(), parent.getName()));
            
            SimulationSubscription currentOutput = propagatedSubscriptions.get(parent);
            SubscriptionWithRegion eventToSend = new SubscriptionWithRegion(new Region(((SubscriptionWithRegion)currentOutput).getRegion()));
            eventToSend.setSource(this);
            
            // Metrics Deep Copy
            if (newSub.getMetrics() != null) {
                EventMetrics copiedMetrics = new EventMetrics(newSub.getMetrics());
                copiedMetrics.incrementHops();
                eventToSend.setMetrics(copiedMetrics);
            }
            
            parent.processSubscription(eventToSend); 
        } else {
             logger.fine(String.format("%s: Region covered. Filtering propagation to parent.", getName()));
        }
    }

    private void propagateSubscriptionDownward(SubscriptionWithRegion newSub) {
        for (TreeNode child : getChildren()) {
            if (child == newSub.getSource() || !(child instanceof BrokerWithRegion childBroker)) {
                continue;
            }

            boolean intersects = false;
            Region childRegion = childBroker.getRegion();
            if (childRegion != null) {
                intersects = childRegion.intersects(newSub.getRegion());
            }

            if (intersects) {
                SubscriptionWithRegion stateUpdate = new SubscriptionWithRegion(new Region(newSub.getRegion()));
                stateUpdate.setSource(this);
                boolean changed = outputView.updateOrExpand(childBroker, stateUpdate);

                if (changed) {
                    incrementPropagationFilterExpansions();
                    logger.fine(String.format("%s: Forwarding subscription to interested child %s.", getName(), childBroker.getName()));
                    
                    SimulationSubscription currentOutput = propagatedSubscriptions.get(childBroker);
                    SubscriptionWithRegion eventToSend = new SubscriptionWithRegion(new Region(((SubscriptionWithRegion)currentOutput).getRegion()));
                    eventToSend.setSource(this);
                    
                    // Metrics Deep Copy
                    if (newSub.getMetrics() != null) {
                        EventMetrics copiedMetrics = new EventMetrics(newSub.getMetrics());
                        copiedMetrics.incrementHops();
                        eventToSend.setMetrics(copiedMetrics);
                    }

                    childBroker.processSubscription(eventToSend);
                }
            }
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        String sourceName = (p.getSource() != null) ? p.getSource().getName() : "NULL_SOURCE";
        logger.fine(getName() + ": processing a publication received from " + sourceName);
        
        // No default upward propagation here (LeafBrokers handle origination)
        
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            if (nextNode == p.getSource()) continue;

            if (entry.getValue() instanceof SubscriptionWithRegion subRegion
                    && p instanceof PublicationWithLocation pubLocation) {
                if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                    logger.fine(getName() + ": forwarding publication to " + nextNode.getName());
                    forwardPublicationToNode(p, nextNode);
                }
            }
        }
        return null;
    }

    protected void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            logger.fine(getName() + ": forwarding publication upward to " + parentBroker.getName());
            
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            
            if (p.getMetrics() != null) {
                EventMetrics copiedMetrics = new EventMetrics(p.getMetrics());
                copiedMetrics.incrementHops();
                forwardedCopy.setMetrics(copiedMetrics);
            }
            
            parentBroker.processPublication(forwardedCopy);
        }
    }

    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof BrokerWithRegion broker) {
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