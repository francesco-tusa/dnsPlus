package simulator.experimentation;

public class ProximityBatchRunner extends AbstractBatchOrchestrator {

    public static void main(String[] args) {
        System.out.println("=== Starting Proximity (Closest) Batch ===");
        
        new ProximityBatchRunner().runBatch(
            new ProximityScenario(), 
            "results_closest_proximity.csv"
        );
    }
}