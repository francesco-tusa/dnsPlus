package marketplace.topology.store;

import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.TreeMultiRegionStore;

public abstract class AbstractMarketplaceRegionStore extends TreeMultiRegionStore {

    protected final double threshold;

    public AbstractMarketplaceRegionStore(double threshold) {
        super(threshold); 
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    /**
     * Shared utility method to calculate the purely spatial False Positive Rate 
     * between two regions, correctly handling Earth's curvature (anti-meridian).
     */
    protected double calculateSpatialFpr(simulator.regions.Region accumulator, SubscriptionWithRegion existing) {
        double currentArea = accumulator.getArea();
        double newArea = existing.getRegion().getArea();

        float r1MinLon = (float) accumulator.getBottomLeft().getX();
        float r1MaxLon = (float) accumulator.getTopRight().getX();
        float r1MinLat = (float) accumulator.getBottomLeft().getY();
        float r1MaxLat = (float) accumulator.getTopRight().getY();

        float r2MinLon = (float) existing.getRegion().getBottomLeft().getX();
        float r2MaxLon = (float) existing.getRegion().getTopRight().getX();
        float r2MinLat = (float) existing.getRegion().getBottomLeft().getY();
        float r2MaxLat = (float) existing.getRegion().getTopRight().getY();

        double mergedArea = simulator.regions.Region.fastMBRArea(
            r1MinLon, r1MaxLon, r1MinLat, r1MaxLat,
            r2MinLon, r2MaxLon, r2MinLat, r2MaxLat
        );

        double spatialFpr = 1.0;
        if (mergedArea > 0) {
            spatialFpr = 1.0 - ((currentArea + newArea) / mergedArea);
        }

        return spatialFpr;
    }
}