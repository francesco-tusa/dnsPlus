package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.metrics.EventMetrics;
import utils.CustomLogger;
import simulator.visualisation.TopologyVisualiser;

public class PublisherWithLocation extends TreeNode {
    private static final Logger logger = CustomLogger.getLogger(PublisherWithLocation.class.getName());
    
    private static final AtomicInteger PUBLISHER_ID_GENERATOR = new AtomicInteger(1);
    
    private final Location location;
    private final int myPublisherId; 
    private int nPublications;
    private final List<PublicationWithLocation> sentPublications = new ArrayList<>();

    // Cached country name to avoid recalculating on every send
    private String myCountry = null;

    public PublisherWithLocation(String name, Location location) {
        this(PUBLISHER_ID_GENERATOR.getAndIncrement(), name, location);
    }

    public PublisherWithLocation(Location location) {
        this(PUBLISHER_ID_GENERATOR.getAndIncrement(), null, location);
    }

    private PublisherWithLocation(int id, String explicitName, Location location) {
        super(resolveName(id, explicitName));
        this.myPublisherId = id;
        this.location = location;
        this.nPublications = 0;
    }
    
    private static String resolveName(int id, String explicitName) {
        if (explicitName != null) return explicitName;
        if (SimConfiguration.get().paths.enableEventTracing) return "Pub-" + id;
        return null; 
    }
    
    @Override
    public String getName() {
        String storedName = super.getName();
        if (storedName != null) return storedName;
        return "Pub-" + myPublisherId;
    }
    
    public void send(PublicationWithLocation pub) {
        pub.setSource(this);

        long seqId = this.nPublications + 1;
        // TraceID is always required for latency calculations (GroundTruthCalculator)
        long traceId = ((long) this.myPublisherId << 32) | (seqId & 0xFFFFFFFFL);
        
        EventMetrics metrics = new EventMetrics(traceId);
        
        if (SimConfiguration.get().paths.enableEventTracing) {
            if (myCountry == null) {
                myCountry = resolveCountry();
            }
            
            metrics.setOriginalSourceInfo(
                getName(), 
                myCountry, 
                location.getX(), 
                location.getY()
            );
        }
        
        pub.setMetrics(metrics);

        TreeNode parent = getParent();
        
        if (parent instanceof SimulationBroker) {
            SimulationBroker broker = (SimulationBroker) parent;
            
            sentPublications.add(pub); 
            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                logger.fine("\n" + getName() + ": sending publication for location " + pub.getLocation());
            }
            
            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
                visualizer.updatePublisherLabel(this);
            }

            broker.processPublication(pub);
            
            nPublications++;
        } else {
            logger.severe(getName() + ": parent is not a SimulationBroker");
        }
    }

    private String resolveCountry() {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = this;
        while (current != null) { 
            path.add(current); 
            current = current.getParent(); 
        }
        // [Pub, Leaf, Region, Country, Root] -> Country is at index size-3
        if (path.size() >= 3) {
            return path.get(path.size() - 3).getName();
        }
        return "Unknown";
    }
    
    public List<PublicationWithLocation> getSentPublications() { return sentPublications; }
    public Location getLocation() { return location; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
    @Override public String resolveLogLocation(String receiverLocationInfo) { return this.location.toString(); }
    @Override public Location getMetricLocation() { return this.location; }
}