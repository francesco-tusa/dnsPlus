package marketplace.topology.store;

import java.util.Collection;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

/**
 * A generic interface representing the first-tier routing namespace.
 * Implementations dictate HOW function identifiers are resolved (e.g., Plaintext HashMap, 
 * Paillier Binary Tree, TFHE Linear Array) before returning the spatial QoS capabilities.
 */
public interface FunctionDirectory {
    
    /**
     * Resolves a client's execution request to the specific spatial tree containing valid offers.
     */
    MarketplaceRegionStore.RegionQuadTree getOfferIndex(ServiceRequest request);
    
    /**
     * Resolves an incoming provider offer to the correct spatial tree, creating it if it doesn't exist.
     */
    MarketplaceRegionStore.RegionQuadTree getOrCreateOfferIndex(ServiceOffer offer);
    
    /**
     * Retrieves all active spatial trees for maintenance, pruning, and state aggregation.
     */
    Collection<MarketplaceRegionStore.RegionQuadTree> getAllOfferIndexes();
}