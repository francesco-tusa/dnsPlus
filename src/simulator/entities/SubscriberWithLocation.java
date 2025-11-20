package simulator.entities;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.events.metrics.EventMetrics;
import simulator.regions.SubscriptionWithRegion;
import simulator.visualisation.TopologyVisualiser;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public class SubscriberWithLocation extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SubscriberWithLocation.class.getName());

    private final Location location;
    private int nSubscriptions;
    private int nPublications;
    private PublicationWithLocation lastReceivedPublication;

    public SubscriberWithLocation(String name, Location location) {
        super(name);
        this.location = location;
        this.nSubscriptions = 0;
        this.nPublications = 0;
        this.lastReceivedPublication = null;
    }

    public void receive(SimulationPublication p) {
        logger.fine(getName() + ": received publication " + p);
        nPublications++;
        
        // --- METRIC COLLECTION (Streaming) ---
        if (p.getMetrics() != null && p instanceof PublicationWithLocation pub) {
            // Log SUCCESSFUL Delivery (No Timestamp)
            CsvMetricWriter.getInstance().logPublicationDelivery(
                p.getMetrics().getTraceId(),
                this.getName(),
                p.getMetrics().getHops(),
                pub.getLocation().getX(),
                pub.getLocation().getY()
            );
        }
        
        if (p instanceof PublicationWithLocation) {
            this.lastReceivedPublication = (PublicationWithLocation) p;
        }

        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            boolean isUpward = false; 
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), isUpward);
        }
    }
    
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);
        
        String traceId = this.getName();
        s.setMetrics(new EventMetrics(traceId));

        if (broker != null) {
            String subInfo = "";
            if (s instanceof SubscriptionWithLocation sl) {
                subInfo = " for location " + sl.getLocation();
            } else if (s instanceof SubscriptionWithRegion sr) {
                subInfo = " for region " + sr.getRegion().toShortString();
            }
            logger.fine("\n" + getName() + ": sending subscription" + subInfo);

            broker.processSubscription(s);
            nSubscriptions++;

            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
                visualizer.updateSubscriberLabel(this, s);
            }

        } else {
            logger.severe(getName() + ": topology error, there is no broker to send the subscription to");
        }
    }
    
    public Location getLocation() { return location; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getnPublications() { return nPublications; }
    
    public SimulationBroker getBroker() { 
        if (getParent() instanceof SimulationBroker) {
            return (SimulationBroker) getParent();
        }
        return null;
    }
    
    public PublicationWithLocation getLastReceivedPublication() { return lastReceivedPublication; }
}