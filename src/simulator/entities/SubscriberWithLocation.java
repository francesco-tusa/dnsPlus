package simulator.entities;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.SubscriptionWithRegion;
import simulator.visualisation.TopologyVisualiser;

public class SubscriberWithLocation extends TreeNode {
    private Location location;
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
        System.out.println(getName() + ": received publication " + p);
        nPublications++;
        if (p instanceof PublicationWithLocation) {
            this.lastReceivedPublication = (PublicationWithLocation) p;
        }

        // --- Visualisation Hook for Heat Map ---
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null) {
            // Set the final edge of the publication path to red
            visualizer.setPublicationEdge(p.getSource().getName(), getName());
        }
    }
    
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);

        if (broker != null) {
            String subInfo = "";
            if (s instanceof SubscriptionWithLocation) {
                SubscriptionWithLocation sl = (SubscriptionWithLocation) s;
                subInfo = " for location " + sl.getLocation();
            } else if (s instanceof SubscriptionWithRegion) {
                SubscriptionWithRegion sr = (SubscriptionWithRegion) s;
                subInfo = " for region " + sr.getRegion().toShortString();
            }
            System.out.println("\n" + getName() + ": sending subscription" + subInfo);

            broker.processSubscription(s);
            nSubscriptions++;
        } 
        else {
            System.out.println(getName() + ": topology error, there is no broker to send the subscription to");
        }
    }
    
    // Unchanged getters
    public Location getLocation() { return location; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
    public PublicationWithLocation getLastReceivedPublication() { return lastReceivedPublication; }
}