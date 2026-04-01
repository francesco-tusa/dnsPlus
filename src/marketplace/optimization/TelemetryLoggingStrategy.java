package marketplace.optimization;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import marketplace.events.ServiceRequest;
import marketplace.events.ServiceOffer;
import marketplace.common.MetricHyperCube;
import utils.CustomLogger;

public class TelemetryLoggingStrategy implements ServiceSelectionStrategy {

    private static final Logger logger = CustomLogger.getLogger(TelemetryLoggingStrategy.class.getName());
    private static final List<TelemetryLoggingStrategy> activeLoggers = new ArrayList<>();
    
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static String dumpDirectory = "telemetry_dump";

    private final ServiceSelectionStrategy baseStrategy;
    private final TreeNode hostBroker;
    private BufferedWriter writer;
    private boolean isInitialized = false;

    private final Set<Long> processedRequestIds = new HashSet<>();

    public static void setDumpDirectory(String path) {
        dumpDirectory = path;
    }

    public TelemetryLoggingStrategy(ServiceSelectionStrategy baseStrategy, TreeNode hostBroker) {
        this.baseStrategy = baseStrategy;
        this.hostBroker = hostBroker;
    }

    private void lazyInitialize() {
        if (isInitialized) return;
        
        try {
            File dir = new File(dumpDirectory);
            if (!dir.exists()) dir.mkdirs();
            
            String geoName = hostBroker.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = dumpDirectory + File.separator + geoName + "_dataset.jsonl";
            
            this.writer = new BufferedWriter(new FileWriter(fileName, false));
            
            synchronized(activeLoggers) {
                activeLoggers.add(this);
            }
            
            isInitialized = true;
        } catch (IOException e) {
            logger.severe("Failed to initialize telemetry logger for broker: " + hostBroker.getName());
            e.printStackTrace();
        }
    }

    @Override
    public SelectionResult selectBestProvider(ServiceRequest req, Map<TreeNode, List<SimulationSubscription>> candidates) {
        SelectionResult result = baseStrategy.selectBestProvider(req, candidates);
        
        processedRequestIds.add(req.getOriginalRequestId());

        lazyInitialize();
        logRawTelemetry(req, candidates, result);
        
        return result;
    }

    private void logRawTelemetry(ServiceRequest req, Map<TreeNode, List<SimulationSubscription>> candidates, SelectionResult result) {
        try {
            // Build a dynamic, un-padded representation of the routing event
            Map<String, Object> eventLog = new HashMap<>();

            // 1. Identifiers
            Map<String, Object> ids = new HashMap<>();
            ids.put("requestId", req.getId());
            ids.put("originalRequestId", req.getOriginalRequestId());
            eventLog.put("trajectory", ids);

            // 2. Client Context
            Map<String, Object> clientState = new HashMap<>();
            clientState.put("x", req.getLocation().getX());
            clientState.put("y", req.getLocation().getY());
            clientState.put("weights", req.getWeights());
            clientState.put("constraints", req.getQoSConstraintsLocation().getMetricValues());
            clientState.put("minimizeFlags", req.getMinimizeFlags()); // Kept as pure booleans
            eventLog.put("client", clientState);

            // 3. Candidates (Variable Length)
            List<Map<String, Object>> candidateList = new ArrayList<>();
            int chosenActionIndex = -1;
            int currentIndex = 0;

            for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : candidates.entrySet()) {
                TreeNode candidateNode = entry.getKey();
                if (candidateNode == req.getSource()) continue;

                if (result.bestNode() != null && candidateNode.equals(result.bestNode())) {
                    chosenActionIndex = currentIndex;
                }

                MetricHyperCube mhc = extractMetricHyperCube(entry.getValue().get(0));
                if (mhc != null) {
                    Map<String, Object> candMap = new HashMap<>();
                    candMap.put("nodeName", candidateNode.getName()); // Traceability
                    candMap.put("centroidX", mhc.getDensityCentroid().getX());
                    candMap.put("centroidY", mhc.getDensityCentroid().getY());
                    candMap.put("yields", mhc.getQosCenterOfMass());
                    candMap.put("minVals", mhc.getMinValues());
                    candMap.put("maxVals", mhc.getMaxValues());
                    
                    candidateList.add(candMap);
                }
                currentIndex++;
            }
            eventLog.put("candidates", candidateList);

            // 4. Oracle Outcome
            Map<String, Object> outcome = new HashMap<>();
            outcome.put("heuristicActionIndex", chosenActionIndex);
            outcome.put("heuristicScore", result.bestScore());
            outcome.put("heuristicDistance", result.distance());
            eventLog.put("oracle", outcome);

            // Serialize to a single JSON line and flush
            String jsonLine = jsonMapper.writeValueAsString(eventLog);
            writer.write(jsonLine + "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MetricHyperCube extractMetricHyperCube(SimulationSubscription sub) {
        if (sub instanceof ServiceOffer offer && offer.getRegion() instanceof MetricHyperCube mhc) {
            return mhc;
        } else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube mhc) {
            return mhc;
        }
        return null;
    }

    public static void flushAndCloseAll() {
        logger.info("Flushing " + activeLoggers.size() + " raw JSONL telemetry silos to disk...");
        for (TelemetryLoggingStrategy strat : activeLoggers) {
            try {
                if (strat.writer != null) {
                    strat.writer.flush();
                    strat.writer.close();
                }
            } catch (IOException e) {
                logger.severe("Failed to close logger for: " + strat.hostBroker.getName());
            }
        }
        activeLoggers.clear();
    }

    public static void flushLocalizedOutcomes(Map<Long, Map<String, Object>> globalOutcomes) {
        for (TelemetryLoggingStrategy strat : activeLoggers) {
            if (strat.processedRequestIds.isEmpty()) continue;

            String geoName = strat.hostBroker.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = dumpDirectory + File.separator + geoName + "_outcomes.jsonl";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
                // ONLY write the outcomes for requests this specific broker touched
                for (Long reqId : strat.processedRequestIds) {
                    Map<String, Object> outcome = globalOutcomes.get(reqId);
                    if (outcome != null) {
                        String jsonLine = jsonMapper.writeValueAsString(outcome);
                        writer.write(jsonLine + "\n");
                    }
                }
            } catch (IOException e) {
                logger.severe("Failed to write localized outcomes for: " + strat.hostBroker.getName());
            }
        }
    }
}