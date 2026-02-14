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

        logger.info(">>> STARTING GROUND TRUTH CALCULATION (Unified Strategy Mode) <<<");

        for (PublicationWithLocation pub : pubs) {
            if (!(pub instanceof ServiceRequest)) {
                continue;
            }
            ServiceRequest request = (ServiceRequest) pub;

            double bestScore = Double.MAX_VALUE;
            ServiceOffer winner = null;
            double exactDist = -1.0; 

            for (SimulationSubscription sub : subs) {
                if (!(sub instanceof ServiceOffer)) {
                    continue;
                }
                ServiceOffer offer = (ServiceOffer) sub;

                if (offer.getServiceId() != request.getServiceId()) {
                    continue;
                }

                // Use the exact same logic as the Broker to determine feasibility
                var result = strategy.inspect(offer, request);

                if (result.isFeasible()) {
                    if (result.score() < bestScore) {
                        bestScore = result.score();
                        winner = offer;
                    }
                }
            }

            if (winner != null) {
                totalOptimalMatches++;
                
                // [FIXED] Calculate exact Euclidean distance using distanceSquared + sqrt
                if (winner.getLocation() != null && request.getLocation() != null) {
                    exactDist = Math.sqrt(winner.getLocation().distanceSquared(request.getLocation()));
                }
                
                writer.logGroundTruth(
                    request.getId(), 
                    request.getServiceId(), 
                    winner.getProviderName(), 
                    bestScore, 
                    exactDist
                );
            } else {
                writer.logGroundTruth(
                    request.getId(), 
                    request.getServiceId(), 
                    "NO_MATCH", 
                    -1.0, 
                    -1.0
                );
            }
        }
        
        logger.info(">>> Ground Truth Calculation Complete. Records written to ground_truth.csv");
        return totalOptimalMatches;
    }
}