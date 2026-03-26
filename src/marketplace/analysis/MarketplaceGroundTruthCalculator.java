package marketplace.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private static final long MIN_WORKLOAD_THRESHOLD = 1_000_000;
    private static final double Massive_SLA_PENALTY = 99999.0;
    
    private final WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();

    // --- CROSS-BATCH GLOBAL STATE & ORACLE CACHE ---
    private final Set<Long> resolvedRequests = new HashSet<>();
    private final Map<Long, Double> globalBestScores = new HashMap<>();
    private final Map<Long, ServiceOffer> globalBestOffers = new HashMap<>();
    private final Map<Long, Double> globalBestDistances = new HashMap<>();
    
    private long totalQosRejects = 0;
    private long totalSpatialRejects = 0;
    private boolean isCsvFlushed = false;

    private record RequestMatchResult(
        long reqId, double bestScore, ServiceOffer winner, 
        double exactDist, int spatialRejects, int qosRejects
    ) {}

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs) {
        if (subs.isEmpty() || pubs.isEmpty()) return 0;
        logger.info(">>> STARTING GROUND TRUTH CALCULATION BATCH (Parallel & Indexed) <<<");

        // --- Global Topic Pre-Indexing ---
        Map<Long, List<ServiceOffer>> offersByTopic = new HashMap<>();
        for (SimulationSubscription sub : subs) {
            if (sub instanceof ServiceOffer offer) {
                offersByTopic.computeIfAbsent(offer.getOracleServiceId(), k -> new ArrayList<>()).add(offer);
            }
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        long workload = (long) pubs.size() * subs.size();
        boolean tooFewPubsForParallel = pubs.size() < numThreads && pubs.size() < 50;

        long newMatchesInThisBatch = 0;

        if (workload < MIN_WORKLOAD_THRESHOLD || numThreads <= 1 || tooFewPubsForParallel) {
            newMatchesInThisBatch = calculateSequential(offersByTopic, pubs);
        } else {
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            try {
                newMatchesInThisBatch = executePublisherParallel(executor, offersByTopic, pubs, numThreads);
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Parallel marketplace ground truth calculation failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }
        }

        logger.info(">>> Batch Ground Truth Calculation Complete. New Unique Matches: " + newMatchesInThisBatch);
        return newMatchesInThisBatch;
    }

    private long executePublisherParallel(ExecutorService executor, Map<Long, List<ServiceOffer>> offersByTopic, List<PublicationWithLocation> pubs, int numThreads) throws InterruptedException, ExecutionException {
        List<Callable<List<RequestMatchResult>>> tasks = new ArrayList<>();
        int batchSize = (int) Math.ceil((double) pubs.size() / numThreads);
        Map<Long, Double> currentBestScoresSnapshot = new HashMap<>(globalBestScores);

        for (int i = 0; i < pubs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, pubs.size());
            List<PublicationWithLocation> pubBatch = pubs.subList(i, end);
            
            tasks.add(() -> {
                List<RequestMatchResult> batchResults = new ArrayList<>();
                for (PublicationWithLocation pub : pubBatch) {
                    if (!(pub instanceof ServiceRequest request)) continue;
                    double initialBestScore = currentBestScoresSnapshot.getOrDefault(request.getId(), Double.MAX_VALUE);
                    batchResults.add(evaluateSingleRequest(request, offersByTopic, initialBestScore));
                }
                return batchResults;
            });
        }

        long newMatchesInThisBatch = 0;
        for (Future<List<RequestMatchResult>> future : executor.invokeAll(tasks)) {
            for (RequestMatchResult res : future.get()) {
                totalSpatialRejects += res.spatialRejects();
                totalQosRejects += res.qosRejects();
                
                if (res.winner() != null) {
                    globalBestScores.put(res.reqId(), res.bestScore());
                    globalBestOffers.put(res.reqId(), res.winner());
                    globalBestDistances.put(res.reqId(), res.exactDist());
                    
                    if (!resolvedRequests.contains(res.reqId())) {
                        resolvedRequests.add(res.reqId());
                        newMatchesInThisBatch++;
                    }
                }
            }
        }
        return newMatchesInThisBatch;
    }

    private long calculateSequential(Map<Long, List<ServiceOffer>> offersByTopic, List<PublicationWithLocation> pubs) {
        long newMatchesInThisBatch = 0;
        for (PublicationWithLocation pub : pubs) {
            if (!(pub instanceof ServiceRequest request)) continue;
            double initialBestScore = globalBestScores.getOrDefault(request.getId(), Double.MAX_VALUE);
            RequestMatchResult res = evaluateSingleRequest(request, offersByTopic, initialBestScore);
            
            totalSpatialRejects += res.spatialRejects();
            totalQosRejects += res.qosRejects();
            
            if (res.winner() != null) {
                globalBestScores.put(res.reqId(), res.bestScore());
                globalBestOffers.put(res.reqId(), res.winner());
                globalBestDistances.put(res.reqId(), res.exactDist());
                if (!resolvedRequests.contains(res.reqId())) {
                    resolvedRequests.add(res.reqId());
                    newMatchesInThisBatch++;
                }
            }
        }
        return newMatchesInThisBatch;
    }

    private RequestMatchResult evaluateSingleRequest(ServiceRequest request, Map<Long, List<ServiceOffer>> offersByTopic, double initialBestScore) {
        double bestScore = initialBestScore;
        ServiceOffer winner = null;
        double exactDist = -1.0;
        int spatialRejects = 0;
        int qosRejects = 0;

        // Only evaluate providers actually hosting this specific function
        List<ServiceOffer> relevantOffers = offersByTopic.getOrDefault(request.getOracleServiceId(), Collections.emptyList());

        for (ServiceOffer offer : relevantOffers) {
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
        return new RequestMatchResult(request.getId(), bestScore, winner, exactDist, spatialRejects, qosRejects);
    }

    // --- EXPOSED STATE FOR METRICS COLLECTOR ---
    public Map<Long, Double> getOracleOptimalScores() {
        flushCsvOnce(); 
        return globalBestScores;
    }

    public double getAverageUtilityScore() {
        if (globalBestScores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double score : globalBestScores.values()) sum += score;
        return sum / globalBestScores.size();
    }
    
    public long getTotalQosRejects() { return totalQosRejects; }
    public long getTotalSpatialRejects() { return totalSpatialRejects; }

    private synchronized void flushCsvOnce() {
        if (isCsvFlushed) return;
        isCsvFlushed = true;
        CsvMetricWriter writer = CsvMetricWriter.getInstance();
        for (Map.Entry<Long, ServiceOffer> entry : globalBestOffers.entrySet()) {
            long reqId = entry.getKey();
            ServiceOffer offer = entry.getValue();

            double rawScore = globalBestScores.get(reqId);
            double score = (rawScore == Double.MAX_VALUE) ? Massive_SLA_PENALTY : rawScore;

            double dist = globalBestDistances.get(reqId);
            writer.logGroundTruth(reqId, offer.getOracleServiceId(), offer.getProviderName(), score, dist);
        }
        logger.info(String.format("Flushed Ground Truth CSV. Total QoS Rejects: %d", totalQosRejects));
    }  
}