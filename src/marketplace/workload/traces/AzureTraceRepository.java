package marketplace.workload.traces;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import marketplace.config.MarketplaceConfig;

public class AzureTraceRepository {
    private static final AzureTraceRepository INSTANCE = new AzureTraceRepository();
    
    private final Map<Long, AzureTraceRecord> traceMap = new HashMap<>(1024);
    private final NavigableMap<Double, Long> rouletteWheel = new TreeMap<>();
    private double totalProbabilityWeight = 0.0;

    private AzureTraceRepository() {}

    public static AzureTraceRepository getInstance() {
        return INSTANCE;
    }

    public void loadTraces(String csvFilePath) {
        // Clear previous state in case this is called during a batch reset
        traceMap.clear();
        rouletteWheel.clear();
        
        // Fetch the dynamic trace limit for the current simulation run
        int traceLimit = MarketplaceConfig.get().azureTraceLimit;
        int loadedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip header
            long cumulativeInvocations = 0;
            
            // Strictly enforce the trace limit bounds
            while ((line = br.readLine()) != null && loadedCount < traceLimit) {
                String[] values = line.split(",");
                long id = Long.parseLong(values[0]);
                long count = Long.parseLong(values[1]);
                double duration = Double.parseDouble(values[2]);
                double memory = Double.parseDouble(values[3]);

                AzureTraceRecord record = new AzureTraceRecord(id, count, duration, memory);
                traceMap.put(id, record);
                cumulativeInvocations += count;
                loadedCount++;
            }

            // Build Roulette Wheel
            double currentProb = 0.0;
            for (AzureTraceRecord record : traceMap.values()) {
                double probability = (double) record.invocationCount() / cumulativeInvocations;
                currentProb += probability;
                rouletteWheel.put(currentProb, record.functionId());
            }
            this.totalProbabilityWeight = currentProb; // Should be ~1.0
        } catch (Exception e) {
            throw new RuntimeException("Trace initialization failed. Expected File: " + csvFilePath, e);
        }
    }

    public AzureTraceRecord getRecord(long functionId) {
        return traceMap.get(functionId);
    }

    public long selectFunctionRouletteWheel(double unscaledRandomValue) {
        double scaledRandom = unscaledRandomValue * this.totalProbabilityWeight;
        Map.Entry<Double, Long> entry = rouletteWheel.ceilingEntry(scaledRandom);
        return (entry != null) ? entry.getValue() : rouletteWheel.lastEntry().getValue();
    }
    
    public Map<Long, AzureTraceRecord> getAllRecords() {
        return traceMap;
    }
}