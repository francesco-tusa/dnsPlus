package marketplace.workload.topics;

public interface FunctionDistributionStrategy {
    long selectClientFunction();
    long selectProviderFunction();
}