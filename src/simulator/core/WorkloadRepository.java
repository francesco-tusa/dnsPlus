package simulator.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import simulator.events.SimulationSubscription;
import utils.SimulationRandom;

/**
 * Centralized High-Performance Repository for Simulation Workloads.
 * * <p>Functions as the Single Source of Truth for generated subscriptions, eliminating
 * the need to pass massive Lists between Orchestrators, Simulations, and GroundTruth calculators.
 * Implements "Zero-Copy" retrieval to prevent Heap exhaustion (GC Thrashing) during large-scale runs.</p>
 */
public class WorkloadRepository {

    private static WorkloadRepository instance;

    // The single hard reference to all simulation events.
    private ArrayList<SimulationSubscription> subscriptionRegistry;

    private WorkloadRepository() {
        // Default small size, expected to be resized via prepare()
        this.subscriptionRegistry = new ArrayList<>(100);
    }

    public static synchronized WorkloadRepository getInstance() {
        if (instance == null) {
            instance = new WorkloadRepository();
        }
        return instance;
    }

    /**
     * Resets the repository and pre-allocates memory for the upcoming workload.
     * Call this before generating subscriptions to avoid incremental array resizing.
     * * @param estimatedCapacity The expected number of subscriptions (Subscribers * Mean).
     */
    public synchronized void prepare(int estimatedCapacity) {
        // Clear old references to allow GC
        if (this.subscriptionRegistry != null) {
            this.subscriptionRegistry.clear();
        }
        // Allocate exact size (or slightly buffered) to prevent array copying
        this.subscriptionRegistry = new ArrayList<>(estimatedCapacity);
    }
    
    /**
     * Clears the repository to free memory.
     */
    public static synchronized void reset() {
        if (instance != null && instance.subscriptionRegistry != null) {
            instance.subscriptionRegistry.clear();
            instance.subscriptionRegistry.trimToSize();
        }
    }

    public void add(SimulationSubscription sub) {
        subscriptionRegistry.add(sub);
    }

    public void shuffle() {
        Collections.shuffle(subscriptionRegistry, SimulationRandom.get());
    }

    /**
     * Returns a zero-copy view of the subscriptions cast to the specific type required 
     * by the Ground Truth Calculator (e.g., SubscriptionWithRegion or SubscriptionWithLocation).
     * * <p>WARNING: The caller must ensure the repository actually contains the expected type.
     * In a controlled simulation run, this is guaranteed by the Orchestrator configuration.</p>
     * * @param <T> The target type (e.g., SubscriptionWithRegion)
     * @return The raw list cast to the target generic type.
     */
    @SuppressWarnings("unchecked")
    public <T extends SimulationSubscription> List<T> getSubscriptions() {
        // Efficiently return the raw list cast to the expected generic type.
        // This avoids creating a new ArrayList<T>(registry) which would duplicate (potentially millions of) pointers.
        return (List<T>) (List<?>) subscriptionRegistry;
    }

    public int size() {
        return subscriptionRegistry.size();
    }
}