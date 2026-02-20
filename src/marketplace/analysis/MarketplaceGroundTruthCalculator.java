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
    // Ensure we use the same strategy logic as the Broker
    private final WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs) {
        long totalOptimalMatches = 0;
        CsvMetricWriter writer = CsvMetricWriter.getInstance();

        logger.info(">>> STARTING GROUND TRUTH CALCULATION (Unified Strategy Mode & Spatial Bounds) <<<");

        // Inside the calculate() method:
        for (PublicationWithLocation pub : pubs) {
            if (!(pub instanceof ServiceRequest))
                continue;
            ServiceRequest request = (ServiceRequest) pub;

            double bestScore = Double.MAX_VALUE;
            ServiceOffer winner = null;
            double exactDist = -1.0;

            // Add tracking counters
            int serviceRejects = 0;
            int spatialRejects = 0;
            int qosRejects = 0;

            for (SimulationSubscription sub : subs) {
                if (!(sub instanceof ServiceOffer))
                    continue;
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
                    qosRejects++; // Failed Multi-Objective Strategy constraint
                }
            }

            if (winner != null) {
                totalOptimalMatches++;
                writer.logGroundTruth(request.getId(), request.getServiceId(), winner.getProviderName(), bestScore,
                        exactDist);
            } else {
                // Log the aggregated rejection reasons
                String rejectionReason = String.format("NO_MATCH (Svc:%d Spat:%d QoS:%d)", serviceRejects,
                        spatialRejects, qosRejects);
                writer.logGroundTruth(request.getId(), request.getServiceId(), rejectionReason, -1.0, -1.0);
            }
        }

        logger.info(">>> Ground Truth Calculation Complete. Records written to ground_truth.csv");
        return totalOptimalMatches;
    }
}