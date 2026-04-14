package marketplace.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceRequest;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import utils.SimulationRandom;

/**
 * An Offline-RL Data Generation Strategy.
 * Wraps the WeightedUtilityStrategy with an Epsilon-Greedy exploration mechanic.
 */
public class EpsilonGreedyUtilityStrategy extends WeightedUtilityStrategy {

    @Override
    public SelectionResult selectBestProvider(ServiceRequest req, Map<TreeNode, List<SimulationSubscription>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SelectionResult(null, PENALTY_SCORE, -1.0, "No Candidates");
        }

        double epsilon = MarketplaceConfig.get().mlExplorationRate;

        // --- EXPLORATION (Random Branching) ---
        if (epsilon > 0.0 && SimulationRandom.get().nextDouble() < epsilon) {
            
            // Extract all available top-level branches
            List<TreeNode> availableBranches = new ArrayList<>(candidates.keySet());
            
            // Pick a purely random branch (intentionally ignoring the heuristic)
            TreeNode randomNode = availableBranches.get(SimulationRandom.get().nextInt(availableBranches.size()));
            
            // Create a constrained map containing ONLY the random choice
            Map<TreeNode, List<SimulationSubscription>> constrainedMap = 
                    Collections.singletonMap(randomNode, candidates.get(randomNode));
            
            // Defer to the superclass. 
            // This guarantees inspectLogic() runs on the random choice, correctly calculating 
            // the distance, network latency additions, and SLA rejection reasons needed 
            // to generate the negative reward telemetry for the ML dataset.
            return super.selectBestProvider(req, constrainedMap);
        }

        // --- EXPLOITATION (Expert Heuristic) ---
        // The coin flip landed on exploit; evaluate the entire map normally.
        return super.selectBestProvider(req, candidates);
    }
}