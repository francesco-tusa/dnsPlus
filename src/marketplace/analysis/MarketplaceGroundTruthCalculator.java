package marketplace.analysis;

import java.util.List;
import java.util.logging.Logger;

import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.optimization.WeightedUtilityStrategy;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import utils.CustomLogger;
import utils.CsvMetricWriter;

public class MarketplaceGroundTruthCalculator implements GroundTruthCalculator {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceGroundTruthCalculator.class.getName());
    private final WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();

    // --- NEW MARKETPLACE METRICS STATE ---
    private double cumulativeUtilityScore = 0.0;
    private long totalQosRejects = 0;
    private long totalSpatialRejects = 0;
    private long actualOptimalMatches = 0;

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs) {
        actualOptimalMatches = 0;
        cumulativeUtilityScore = 0.0;
        totalQosRejects = 0;
        totalSpatialRejects = 0;
        
        CsvMetricWriter writer = CsvMetricWriter.getInstance();
        logger.info(">>> STARTING GROUND TRUTH CALCULATION (Unified Strategy Mode & Spatial Bounds) <<<");

        for (PublicationWithLocation pub : pubs) {
            if (!(pub instanceof ServiceRequest)) continue;
            ServiceRequest request = (ServiceRequest) pub;

            double bestScore = Double.MAX_VALUE;
            ServiceOffer winner = null;
            double exactDist = -1.0;

            int serviceRejects = 0;
            int spatialRejects = 0;
            int qosRejects = 0;

            for (SimulationSubscription sub : subs) {
                if (!(sub instanceof ServiceOffer)) continue;
                ServiceOffer offer = (ServiceOffer) sub;

                if (offer.getServiceId() != request.getServiceId()) {
                    serviceRejects++;
                    continue;
                }

                double currentDist = -1.0;
                if (offer.getLocation() != null && request.getLocation() != null) {
                    currentDist = Math.sqrt(offer.getLocation().distanceSquared(request.getLocation()));
                }

                if (request.getLocation() != null && offer.getRegion() != null) {
                    if (!offer.getRegion().contains(request.getLocation())) {
                        spatialRejects++;
                        continue;
                    }
                } else if (currentDist > offer.getCoverageRadius()) {
                    spatialRejects++;
                    continue;
                }

                var result = strategy.inspect(offer, request);
                if (result.isFeasible()) {
                    if (result.score() < bestScore) {
                        bestScore = result.score();
                        winner = offer;
                        exactDist = currentDist;
                    }
                } else {
                    qosRejects++; 
                }
            }

            if (winner != null) {
                actualOptimalMatches++;
                cumulativeUtilityScore += bestScore; // Accumulate utility for the batch average
                writer.logGroundTruth(request.getId(), request.getServiceId(), winner.getProviderName(), bestScore, exactDist);
            } else {
                totalSpatialRejects += spatialRejects;
                totalQosRejects += qosRejects;
                String rejectionReason = String.format("NO_MATCH (Svc:%d Spat:%d QoS:%d)", serviceRejects, spatialRejects, qosRejects);
                writer.logGroundTruth(request.getId(), request.getServiceId(), rejectionReason, -1.0, -1.0);
            }
        }

        logger.info(">>> Ground Truth Calculation Complete.");
        return actualOptimalMatches;
    }

    // --- GETTERS FOR CSV EXPORT ---
    public double getAverageUtilityScore() {
        return actualOptimalMatches == 0 ? 0.0 : cumulativeUtilityScore / actualOptimalMatches;
    }
    public long getTotalQosRejects() { return totalQosRejects; }
    public long getTotalSpatialRejects() { return totalSpatialRejects; }
}