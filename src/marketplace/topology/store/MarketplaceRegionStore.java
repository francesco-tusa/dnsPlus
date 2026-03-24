package marketplace.topology.store;

import java.util.*;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.*;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.common.aggregation.AggregationStrategy;
import marketplace.config.MarketplaceConfig;

/**
 * Unified Region Store for the Marketplace.
 * Exclusively handles MetricHyperCubes, evaluating both Spatial and QoS Multi-Objective FPR.
 */
public class MarketplaceRegionStore extends AbstractMultiRegionStore {

    // TreeNode (Interface) -> FunctionDirectory (Topic Resolver) -> RegionQuadTree (Capabilities)
    protected final Map<TreeNode, FunctionDirectory> routingTable = new HashMap<>();

    public MarketplaceRegionStore(double threshold) {
        super(threshold);
    }

    /**
     * Polymorphic Factory Method. 
     * Override this in cryptographic subclasses (e.g., PaillierRegionStore) 
     * to inject a different directory implementation.
     */
    protected FunctionDirectory createFunctionDirectory() {
        return new PlaintextFunctionDirectory();
    }

    protected static class MergeEvaluation {
        public final boolean canMerge;
        public final String reason;
        public final double penalty;

        public MergeEvaluation(boolean canMerge, String reason, double penalty) {
            this.canMerge = canMerge;
            this.reason = reason;
            this.penalty = penalty;
        }
    }

    // =========================================================================
    // THE UNIFIED EVALUATION PIPELINE
    // =========================================================================

    protected MergeEvaluation evaluateMerge(Region accumulator, SubscriptionWithRegion existing) {
        if (!(accumulator instanceof MetricHyperCube cubeA) || !(existing.getRegion() instanceof MetricHyperCube cubeB)) {
            boolean result = super.shouldMerge(accumulator, existing);
            return new MergeEvaluation(result, result ? "Merged (Spatial)" : "Threshold Exceeded", 0.0); 
        }

        double maxFpr = calculateMaxFprPenalty(cubeA, cubeB);
        
        if (maxFpr <= this.mergeThreshold) {
            return new MergeEvaluation(true, "Merged", maxFpr);
        } else {
            return new MergeEvaluation(false, String.format("Max FPR %.2f > %.2f", maxFpr, this.mergeThreshold), maxFpr);
        }
    }

    private double calculateMaxFprPenalty(MetricHyperCube cubeA, MetricHyperCube cubeB) {
        // 1. Calculate Spatial FPR
        double maxFpr = calculateSpatialFpr(cubeA, cubeB);

        // 2. Calculate QoS Dimensional FPR
        double[] lowA = cubeA.getCapabilityMinValues();
        double[] highA = cubeA.getCapabilityMaxValues();
        double[] meanA = cubeA.getQosCenterOfMass();
        double weightA = cubeA.getProviderWeight();

        double[] lowB = cubeB.getCapabilityMinValues();
        double[] highB = cubeB.getCapabilityMaxValues();
        double[] meanB = cubeB.getQosCenterOfMass();
        double weightB = cubeB.getProviderWeight();

        double epsilon = 1e-9;
        AggregationStrategy strategy = MarketplaceConfig.get().activeAggregationStrategy;

        for (int i = 0; i < lowA.length; i++) {
            double spanA = highA[i] - lowA[i];
            double spanB = highB[i] - lowB[i];
            double minLow = Math.min(lowA[i], lowB[i]);
            double maxHigh = Math.max(highA[i], highB[i]);
            double spanAgg = maxHigh - minLow;

            // Gate 1: 1D Gap Penalty
            double gap = Math.max(0.0, Math.max(lowA[i], lowB[i]) - Math.min(highA[i], highB[i]));
            double gapFpr = gap / Math.max(spanAgg, epsilon);
            maxFpr = Math.max(maxFpr, gapFpr);

            // Gate 2: Mean Similarity Penalty
            double meanAgg = strategy.calculateAggregatedValue(meanA[i], (int)weightA, meanB[i], (int)weightB);
            double shiftA = Math.abs(meanAgg - meanA[i]);
            double shiftB = Math.abs(meanAgg - meanB[i]);
            double aggregatedShift = strategy.calculateAggregatedValue(shiftA, (int)weightA, shiftB, (int)weightB);
            
            double aggregatedDenominator = strategy.calculateAggregatedValue(spanA, (int)weightA, spanB, (int)weightB);
            if (aggregatedDenominator < epsilon) {
                aggregatedDenominator = spanAgg;
            }

            double meanShiftFpr = aggregatedShift / Math.max(aggregatedDenominator, epsilon);
            maxFpr = Math.max(maxFpr, meanShiftFpr);
        }
        return maxFpr;
    }

    private double calculateSpatialFpr(Region cubeA, Region cubeB) {
        double mergedArea = cubeA.getMergedArea(cubeB);
        if (mergedArea <= 1e-9) return 0.0;
        double unionArea = cubeA.getArea() + cubeB.getArea() - cubeA.getIntersectionArea(cubeB);
        double spatialDeadSpace = Math.max(0.0, mergedArea - unionArea);
        return spatialDeadSpace / mergedArea;
    }

    // =========================================================================
    // STORE LOGIC
    // =========================================================================

    protected SubscriptionWithRegion wrapAggregatedRegion(SubscriptionWithRegion sub, Region accumulator, int mergedCount, int absorbedCount) {
        if (sub instanceof ServiceOffer offer && accumulator instanceof MetricHyperCube mhc) {
            if (mergedCount == 0 && absorbedCount == 0) {
                return ServiceOffer.createWithUpdatedRegion(offer, mhc);
            } else {
                return ServiceOffer.createAggregated(offer.getOracleServiceId(), offer.getIdentifier(), mhc);
            }
        } else {
            return new SubscriptionWithRegion(accumulator);
        }
    }

    private boolean areCompatible(SubscriptionWithRegion a, SubscriptionWithRegion b) {
        if (a instanceof ServiceOffer oa && b instanceof ServiceOffer ob) {
            return oa.getOracleServiceId() == ob.getOracleServiceId();
        }
        return !(a instanceof ServiceOffer) && !(b instanceof ServiceOffer);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        if (!(sub instanceof ServiceOffer offer)) {
            return new StoreUpdate(StoreOpResult.NO_CHANGE, sub, "Invalid Type", 0, 0, null);
        }

        // 1. Get the interface/neighbor's directory
        FunctionDirectory directory = routingTable.computeIfAbsent(source, k -> createFunctionDirectory());

        // 2. Delegate identifier resolution to the generic directory
        RegionQuadTree tree = directory.getOrCreateOfferIndex(offer);
        
        List<SubscriptionWithRegion> candidates = tree.findCandidatesContaining(sub.getRegion());
        for (SubscriptionWithRegion existing : candidates) {
            if (areCompatible(sub, existing) && existing.contains(sub)) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Filtered", 0, 0, existing);
            }
        }

        Region accumulator = sub.getRegion().copy();
        boolean mergedInPass;
        int absorbedCount = 0;
        int mergedCount = 0;
        double absorbedArea = 0.0;
        
        String rejectionReason = "No Overlaps";
        double maxExpansionPenalty = 0.0;
        SubscriptionWithRegion existingTarget = null; 

        do {
            mergedInPass = false;
            Region spatialQuery = new Region(accumulator);
            List<SubscriptionWithRegion> overlaps = tree.findIntersections(spatialQuery);

            for (SubscriptionWithRegion existing : overlaps) {
                if (!areCompatible(sub, existing)) continue;

                MergeEvaluation eval = evaluateMerge(accumulator, existing);
                if (eval.canMerge) {
                    maxExpansionPenalty = Math.max(maxExpansionPenalty, eval.penalty);
                    
                    if (existingTarget == null) existingTarget = existing;
                    else if (existing.getArea() > existingTarget.getArea()) existingTarget = existing;
                    
                    if (accumulator.contains(existing.getRegion())) {
                        absorbedArea += existing.getArea();
                        tree.remove(existing);
                        absorbedCount++;
                        mergedInPass = true; 
                        break;
                    } else {
                        accumulator.expand(existing.getRegion());
                        tree.remove(existing);
                        mergedInPass = true;
                        mergedCount++;
                        break;
                    }
                } else {
                    rejectionReason = eval.reason;
                }
            }
        } while (mergedInPass);

        SubscriptionWithRegion resultingEntry = wrapAggregatedRegion(sub, accumulator, mergedCount, absorbedCount);
        tree.insert(resultingEntry);

        double accumArea = accumulator.getArea();
        boolean isIdenticalReplacement = (mergedCount == 0 && Math.abs(accumArea - (sub.getArea() + absorbedArea)) < 1e-9);

        StoreOpResult opResult = determineResult(absorbedCount, mergedCount, isIdenticalReplacement);
        
        String explanation;
        if (opResult == StoreOpResult.ADDED) {
            explanation = "ADDED [" + rejectionReason + "]";
        } else if (opResult == StoreOpResult.EXPANDED) {
            explanation = String.format("EXPANDED [Max Expansion Penalty: %.5f]", maxExpansionPenalty);
        } else {
            explanation = createCleanExplanation(opResult, mergedCount, absorbedCount, isIdenticalReplacement);
        }
        
        return new StoreUpdate(opResult, resultingEntry, explanation, absorbedCount, mergedCount, existingTarget);
    }

    /**
     * Dual-Key Resolution. Matches strictly against the required Service ID Topic
     * before evaluating the Spatial QoS criteria.
     */
    public int findMatchesForRequest(ServiceRequest req, Map<TreeNode, List<SimulationSubscription>> resultsBuffer) {
        int[] opsCounter = new int[1];

        for (Map.Entry<TreeNode, FunctionDirectory> entry : routingTable.entrySet()) {
            TreeNode interfaceNode = entry.getKey();
            FunctionDirectory directory = entry.getValue();

            // 1. Topic Match (Delegated to the polymorphic directory implementation)
            RegionQuadTree tree = directory.getOfferIndex(req);

            if (tree != null) {
                List<SimulationSubscription> hits = new ArrayList<>();
                // 2. Spatial Match (Search only the specific function's tree)
                tree.findMatches(req.getLocation(), hits, opsCounter);
                
                if (!hits.isEmpty()) {
                    resultsBuffer.put(interfaceNode, hits);
                }
            }
        }
        return opsCounter[0];
    }

    /**
     * Determines if the specific Function Topic is currently tracked by this broker,
     * regardless of whether the spatial/QoS constraints match.
     */
    public boolean hasFunctionTopic(ServiceRequest req) {
        for (FunctionDirectory directory : routingTable.values()) {
            if (directory.getOfferIndex(req) != null) {
                return true; // The topic exists on at least one branch
            }
        }
        return false; // Complete Topic Miss
    }

    /**
     * Legacy Fallback: Iterates across ALL function trees for a pure Spatial Match.
     */
    @Override
    public int findMatches(Location loc, Map<TreeNode, List<SimulationSubscription>> resultsBuffer) {
        int[] opsCounter = new int[1];
        for (Map.Entry<TreeNode, FunctionDirectory> entry : routingTable.entrySet()) {
            TreeNode interfaceNode = entry.getKey();
            List<SimulationSubscription> hits = new ArrayList<>();
            
            for (RegionQuadTree tree : entry.getValue().getAllOfferIndexes()) {
                tree.findMatches(loc, hits, opsCounter);
            }
            
            if (!hits.isEmpty()) {
                resultsBuffer.put(interfaceNode, hits);
            }
        }
        return opsCounter[0];
    }

    @Override
    public List<SimulationSubscription> getOutputFor(TreeNode target) {
        FunctionDirectory dir = routingTable.get(target);
        if (dir == null) return Collections.emptyList();
        
        List<SimulationSubscription> all = new ArrayList<>();
        for (RegionQuadTree tree : dir.getAllOfferIndexes()) {
            all.addAll(tree.getAll());
        }
        return all;
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions() {
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        for (Map.Entry<TreeNode, FunctionDirectory> entry : routingTable.entrySet()) {
            List<SimulationSubscription> all = new ArrayList<>();
            for (RegionQuadTree tree : entry.getValue().getAllOfferIndexes()) {
                all.addAll(tree.getAll());
            }
            if (!all.isEmpty()) {
                result.put(entry.getKey(), all);
            }
        }
        return result;
    }

    /**
     * Calculates the size of the first-tier Function Directory.
     * This represents the total number of unique functions this broker is routing,
     * which translates directly to the number of HE match operations required.
     */
    public int getFunctionDirectorySize() {
        int topicCount = 0;
        for (FunctionDirectory dir : routingTable.values()) {
            topicCount += dir.getAllOfferIndexes().size();
        }
        return topicCount;
    }

    @Override
    public int size() {
        return routingTable.values().stream()
                .flatMap(dir -> dir.getAllOfferIndexes().stream())
                .mapToInt(RegionQuadTree::size)
                .sum();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    // =========================================================================
    // RegionQuadTree 
    // =========================================================================
    public static class RegionQuadTree {
        private static final int MAX_ITEMS = 16;
        private static final int MAX_DEPTH = 10;
        private final double minLon, minLat, maxLon, maxLat;
        private final List<SubscriptionWithRegion> items;
        private RegionQuadTree[] children;
        private final int depth;
        private Region cachedMBR = null;

        public RegionQuadTree(double minLon, double minLat, double maxLon, double maxLat) {
            this(minLon, minLat, maxLon, maxLat, 0);
        }

        private RegionQuadTree(double minLon, double minLat, double maxLon, double maxLat, int depth) {
            this.minLon = minLon;
            this.minLat = minLat;
            this.maxLon = maxLon;
            this.maxLat = maxLat;
            this.items = new ArrayList<>();
            this.depth = depth;
        }

        public void insert(SubscriptionWithRegion sub) {
            if (cachedMBR == null) cachedMBR = new Region(sub.getRegion());
            else cachedMBR.expand(sub.getRegion());

            if (children != null) {
                int index = getIndex(sub.getRegion());
                if (index != -1) {
                    children[index].insert(sub);
                    return;
                }
            }
            items.add(sub);
            if (items.size() > MAX_ITEMS && depth < MAX_DEPTH) {
                if (children == null) split();
                Iterator<SubscriptionWithRegion> it = items.iterator();
                while (it.hasNext()) {
                    SubscriptionWithRegion s = it.next();
                    int index = getIndex(s.getRegion());
                    if (index != -1) {
                        children[index].insert(s);
                        it.remove();
                    }
                }
            }
        }

        public boolean remove(SubscriptionWithRegion sub) {
            boolean removed = items.remove(sub);
            if (!removed && children != null) {
                int index = getIndex(sub.getRegion());
                if (index != -1) removed = children[index].remove(sub);
                else for (RegionQuadTree child : children)
                    if (child.remove(sub)) { removed = true; break; }
            }
            if (removed) recalculateMBR();
            return removed;
        }

        private void recalculateMBR() {
            cachedMBR = null;
            for (SubscriptionWithRegion s : items) {
                if (cachedMBR == null) cachedMBR = new Region(s.getRegion());
                else cachedMBR.expand(s.getRegion());
            }
            if (children != null)
                for (RegionQuadTree c : children)
                    if (c.getMBR() != null) {
                        if (cachedMBR == null) cachedMBR = new Region(c.getMBR());
                        else cachedMBR.expand(c.getMBR());
                    }
        }

        public Region getMBR() { return cachedMBR; }
        
        public List<SubscriptionWithRegion> getAll() {
            List<SubscriptionWithRegion> all = new ArrayList<>(items);
            if (children != null) for (RegionQuadTree c : children) all.addAll(c.getAll());
            return all;
        }
        
        public int size() {
            int c = items.size();
            if (children != null) for (RegionQuadTree child : children) c += child.size();
            return c;
        }

        public void findMatches(Location loc, List<SimulationSubscription> hits, int[] ops) {
            ops[0]++;
            if (cachedMBR == null || !cachedMBR.contains(loc)) return;
            for (SubscriptionWithRegion s : items) {
                ops[0]++;
                if (s.getRegion().contains(loc)) {
                    hits.add(s);
                }
            }
            if (children != null) {
                int idx = getPointIndex(loc.getX(), loc.getY());
                if (idx != -1) {
                    children[idx].findMatches(loc, hits, ops);
                }
            }
        }

        public List<SubscriptionWithRegion> findCandidatesContaining(Region query) {
            if (cachedMBR == null || !cachedMBR.contains(query)) return Collections.emptyList();
            List<SubscriptionWithRegion> c = new ArrayList<>();
            for (SubscriptionWithRegion s : items)
                if (s.getRegion().contains(query)) c.add(s);
            if (children != null) {
                int idx = getIndex(query);
                if (idx != -1) c.addAll(children[idx].findCandidatesContaining(query));
            }
            return c;
        }

        public List<SubscriptionWithRegion> findIntersections(Region query) {
            if (cachedMBR == null || !cachedMBR.intersects(query)) return Collections.emptyList();
            List<SubscriptionWithRegion> h = new ArrayList<>();
            for (SubscriptionWithRegion s : items)
                if (s.getRegion().intersects(query)) h.add(s);
            if (children != null)
                for (RegionQuadTree child : children) {
                    if (child.getMBR() != null && child.getMBR().intersects(query))
                        h.addAll(child.findIntersections(query));
                }
            return h;
        }

        private void split() {
            double mx = (minLon + maxLon) / 2, my = (minLat + maxLat) / 2;
            children = new RegionQuadTree[4];
            children[0] = new RegionQuadTree(minLon, my, mx, maxLat, depth + 1);
            children[1] = new RegionQuadTree(mx, my, maxLon, maxLat, depth + 1);
            children[2] = new RegionQuadTree(minLon, minLat, mx, my, depth + 1);
            children[3] = new RegionQuadTree(mx, minLat, maxLon, my, depth + 1);
        }

        private int getIndex(Region r) {
            double mx = (minLon + maxLon) / 2, my = (minLat + maxLat) / 2;
            boolean top = r.getMinLat() >= my, bottom = r.getMaxLat() <= my;
            boolean left = r.getMaxLon() <= mx, right = r.getMinLon() >= mx;
            if (top) { if (left) return 0; if (right) return 1; } 
            else if (bottom) { if (left) return 2; if (right) return 3; }
            return -1;
        }

        private int getPointIndex(double x, double y) {
            double mx = (minLon + maxLon) / 2, my = (minLat + maxLat) / 2;
            boolean top = y >= my, left = x <= mx;
            return top ? (left ? 0 : 1) : (left ? 2 : 3);
        }
    }
}