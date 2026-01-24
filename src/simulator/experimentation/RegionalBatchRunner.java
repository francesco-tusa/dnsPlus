package simulator.experimentation;

public class RegionalBatchRunner extends AbstractBatchOrchestrator {

    public static void main(String[] args) {
        System.out.println("=== Starting Regional (Spatial Match) Batch ===");
        
        new RegionalBatchRunner().runBatch(
            new RegionalScenario(), 
            "results_regional_heavy.csv"
        );
    }
}