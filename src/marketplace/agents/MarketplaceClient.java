package marketplace.agents;

import simulator.entities.PublisherWithLocation;
import simulator.core.Location;
import java.util.Map;

import marketplace.common.identifiers.RoutingIdentifierFactory;
import marketplace.common.identifiers.ServiceIdentifier;
import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceRequest;

public class MarketplaceClient extends PublisherWithLocation {
    private final RoutingIdentifierFactory cryptographyFactory;

    public MarketplaceClient(String name, Location location) {
        super(name, location);
        this.cryptographyFactory = MarketplaceConfig.get().routingCryptography;
    }

    public MarketplaceClient(Location location) {
        super(location); // Defers to the AtomicInteger in PublisherWithLocation
        this.cryptographyFactory = MarketplaceConfig.get().routingCryptography;
    }

    /**
     * Factory method for the simulation runner. Creates a cryptographically 
     * wrapped request without immediately injecting it into the network.
     */
    public ServiceRequest createServiceRequest(long oracleServiceId, Map<String, Double> constraints, Map<String, Double> weights) {
        
        ServiceIdentifier routingId = this.cryptographyFactory.createIdentifier(oracleServiceId);

        // Create the request using the Dual-Track constructor
        ServiceRequest req = new ServiceRequest(oracleServiceId, routingId, constraints, weights, this.getLocation());
        req.setSource(this);
        
        return req;
    }

    /**
     * Immediate Execution: Request a service and inject it straight into the broker network.
     */
    public void requestService(long oracleServiceId, Map<String, Double> constraints, Map<String, Double> weights) {
        ServiceRequest req = this.createServiceRequest(oracleServiceId, constraints, weights);
        super.send(req);
    }

    public void requestService(long oracleServiceId, Map<String, Double> constraints) {
        this.requestService(oracleServiceId, constraints, null);
    }
}