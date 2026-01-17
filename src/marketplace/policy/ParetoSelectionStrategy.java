package marketplace.policy;

import marketplace.logic.ServiceEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParetoSelectionStrategy implements ServiceSelectionStrategy {

    @Override
    public String selectBestProvider(List<ServiceEntry> candidates, Map<String, Object> requirements) {
        // 1. FILTER: Eliminate candidates that violate hard constraints
        List<ServiceEntry> validCandidates = filterByConstraints(candidates, requirements);

        if (validCandidates.isEmpty()) return null;

        // 2. PARETO FRONT: Find the set of non-dominated candidates
        List<ServiceEntry> paretoFront = findParetoFront(validCandidates);
        
        System.out.println("   [Pareto] Identified " + paretoFront.size() + " optimal candidates on the frontier.");

        // 3. SELECTION: Pick the best from the Front based on a "Utility Function"
        // (Here we use a weighted score, but you could use Euclidean distance to origin)
        return selectFromFront(paretoFront);
    }

    private List<ServiceEntry> findParetoFront(List<ServiceEntry> candidates) {
        List<ServiceEntry> front = new ArrayList<>();

        for (ServiceEntry candidate : candidates) {
            boolean isDominated = false;
            for (ServiceEntry other : candidates) {
                if (candidate == other) continue;
                if (dominates(other, candidate)) {
                    isDominated = true;
                    break;
                }
            }
            if (!isDominated) {
                front.add(candidate);
            }
        }
        return front;
    }

    /**
     * Returns true if provider 'A' dominates provider 'B'.
     * Logic: A is better/equal in ALL metrics, and strictly better in at least ONE.
     */
    private boolean dominates(ServiceEntry a, ServiceEntry b) {
        Map<String, Object> capsA = a.getCapabilities();
        Map<String, Object> capsB = b.getCapabilities();

        // Dimensions to optimize (Lower is better for Cost & Latency)
        double costA = asDouble(capsA.get("cost"));
        double costB = asDouble(capsB.get("cost"));
        
        double latA = asDouble(capsA.get("latency")); // e.g., ms
        double latB = asDouble(capsB.get("latency"));

        boolean betterInOne = false;

        // Check Cost (Minimization)
        if (costA > costB) return false; // A is worse
        if (costA < costB) betterInOne = true;

        // Check Latency (Minimization)
        if (latA > latB) return false; // A is worse
        if (latA < latB) betterInOne = true;

        // Check Reliability (Maximization - Higher is better)
        double relA = asDouble(capsA.get("reliability")); // e.g., 0.99
        double relB = asDouble(capsB.get("reliability"));
        if (relA < relB) return false; // A is worse
        if (relA > relB) betterInOne = true;

        return betterInOne;
    }

    private String selectFromFront(List<ServiceEntry> front) {
        // Simple Utility: Equal weights for Cost and Latency (normalized)
        // In a real system, you'd normalize these values or take user preferences.
        ServiceEntry best = front.get(0);
        double bestScore = Double.MAX_VALUE;

        for (ServiceEntry entry : front) {
            // Simple Weighted Sum (Cost + Latency)
            // Assumes broadly similar scales or just illustrative logic
            double score = asDouble(entry.getCapabilities().get("cost")) + 
                           asDouble(entry.getCapabilities().get("latency"));
            
            if (score < bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        return best.getProviderId();
    }

    private List<ServiceEntry> filterByConstraints(List<ServiceEntry> candidates, Map<String, Object> reqs) {
        List<ServiceEntry> valid = new ArrayList<>();
        for (ServiceEntry c : candidates) {
            if (satisfies(c, reqs)) valid.add(c);
        }
        return valid;
    }

    private boolean satisfies(ServiceEntry entry, Map<String, Object> reqs) {
        // Reuse constraint logic (max_cost, min_ram) from previous steps
        Map<String, Object> caps = entry.getCapabilities();
        if (reqs.containsKey("max_cost") && asDouble(caps.get("cost")) > asDouble(reqs.get("max_cost"))) return false;
        if (reqs.containsKey("min_ram") && asDouble(caps.get("ram")) < asDouble(reqs.get("min_ram"))) return false;
        return true;
    }

    private double asDouble(Object val) {
        if (val == null) return 0.0; // Default or handle appropriately
        return ((Number) val).doubleValue();
    }
}