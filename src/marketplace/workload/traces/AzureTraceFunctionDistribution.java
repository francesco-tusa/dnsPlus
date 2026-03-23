package marketplace.workload.traces;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import marketplace.workload.topics.FunctionDistributionStrategy;
import utils.SimulationRandom;

public class AzureTraceFunctionDistribution implements FunctionDistributionStrategy {
    
    private final Random random;
    private final AzureTraceRepository repository;
    private final List<Long> uniformProviderKeys;

    public AzureTraceFunctionDistribution() {
        this.random = SimulationRandom.get();
        this.repository = AzureTraceRepository.getInstance();
        
        // Cache the keyset as an array list strictly for O(1) uniform provider sampling
        this.uniformProviderKeys = new ArrayList<>(repository.getAllRecords().keySet());
        
        if (uniformProviderKeys.isEmpty()) {
            throw new IllegalStateException("AzureTraceRepository is empty! Did you call loadTraces() before starting the simulation?");
        }
    }

    @Override
    public long selectClientFunction() {
        // 1. Clients request functions based on the highly skewed empirical probability distribution
        // The repository handles the scaling against the floating-point total probability weight
        return repository.selectFunctionRouletteWheel(random.nextDouble());
    }

    @Override
    public long selectProviderFunction() {
        // 2. Providers uniformly offer functions to ensure coverage across the 1000 capabilities
        return uniformProviderKeys.get(random.nextInt(uniformProviderKeys.size()));
    }
}