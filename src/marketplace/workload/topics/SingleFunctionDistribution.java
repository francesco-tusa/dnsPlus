package marketplace.workload.topics;

/**
 * Baseline strategy: Hardcodes the entire simulation to use a single function ID.
 */
public class SingleFunctionDistribution implements FunctionDistributionStrategy {
    private static final long HARDCODED_ID = 1L;

    @Override
    public long selectClientFunction() { return HARDCODED_ID; }

    @Override
    public long selectProviderFunction() { return HARDCODED_ID; }
}