package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.SubscriptionWithRegion;
import simulator.visualisation.TopologyVisualiser;
import utils.CustomLogger;

public class SubscriberWithLocation extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SubscriberWithLocation.class.getName());

    private final Location location;
    private int nSubscriptions;
    private int nPublications;
    private PublicationWithLocation lastReceivedPublication;
    
    // Stores the hop count of each publication received
    private final List<Integer> receivedPublicationHops = new ArrayList<>();

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
        receivedPublicationHops.add(p.getHops()); // Store the hop count
        
        if (p instanceof PublicationWithLocation) {
            this.lastReceivedPublication = (PublicationWithLocation) p;
        }

        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            // We now call updatePublicationEdge and specify the direction as 'false' (downward).
            boolean isUpward = false; // Delivery to a subscriber is always a downward event.
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), isUpward);
        }
    }
    
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);

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
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
    public PublicationWithLocation getLastReceivedPublication() { return lastReceivedPublication; }
    
    /**
     * Gets the list of hop counts for all publications delivered to this subscriber.
     * @return A list of hop count integers.
     */
    public List<Integer> getReceivedPublicationHops() {
        return receivedPublicationHops;
    }
}
