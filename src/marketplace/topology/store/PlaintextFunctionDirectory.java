package marketplace.topology.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.common.identifiers.PlaintextIdentifier;

public class PlaintextFunctionDirectory implements FunctionDirectory {
    
    // The plaintext generic container: maps the exact Long ID to the QoS QuadTree
    private final Map<Long, MarketplaceRegionStore.RegionQuadTree> indexMap = new HashMap<>();

    @Override
    public MarketplaceRegionStore.RegionQuadTree getOfferIndex(ServiceRequest request) {
        // 1. Unpack the opaque routing identifier, strictly ignoring the oracle ID
        if (request.getIdentifier() instanceof PlaintextIdentifier plainId) {
            return indexMap.get(plainId.getId());
        }
        throw new IllegalArgumentException("PlaintextFunctionDirectory requires a PlaintextIdentifier payload");
    }

    @Override
    public MarketplaceRegionStore.RegionQuadTree getOrCreateOfferIndex(ServiceOffer offer) {
        if (offer.getIdentifier() instanceof PlaintextIdentifier plainId) {
            return indexMap.computeIfAbsent(
                plainId.getId(),
                k -> new MarketplaceRegionStore.RegionQuadTree(-180, -90, 180, 90)
            );
        }
        throw new IllegalArgumentException("PlaintextFunctionDirectory requires a PlaintextIdentifier payload");
    }

    @Override
    public Collection<MarketplaceRegionStore.RegionQuadTree> getAllOfferIndexes() {
        return indexMap.values();
    }
}