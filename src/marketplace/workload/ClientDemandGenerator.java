package marketplace.workload;

public interface ClientDemandGenerator {
    ClientDemandProfile generateDemand(int clientIndex, long functionId);
}