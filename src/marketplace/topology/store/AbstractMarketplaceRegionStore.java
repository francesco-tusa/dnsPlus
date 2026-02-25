package marketplace.topology.store;

import java.util.*;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.*;
import marketplace.events.ServiceOffer;

public abstract class AbstractMarketplaceRegionStore extends AbstractMultiRegionStore {

    protected final Map<TreeNode, RegionQuadTree> map = new HashMap<>();

    public AbstractMarketplaceRegionStore(double threshold) {
        super(threshold);
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

    protected MergeEvaluation evaluateMerge(Region accumulator, SubscriptionWithRegion existing) {
        boolean result = shouldMerge(accumulator, existing);
        return new MergeEvaluation(result, result ? "Merged" : "Threshold Exceeded", 0.0);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        RegionQuadTree tree = map.computeIfAbsent(source, k -> new RegionQuadTree(-180, -90, 180, 90));

        // 1. Filter Check (If it's identical, it interacted with 'existing')
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
        
        // Track the existing region we are merging with
        SubscriptionWithRegion existingTarget = null; 

        // 2. Absorb/Merge Loop
        do {
            mergedInPass = false;
            Region spatialQuery = new Region(accumulator);
            List<SubscriptionWithRegion> overlaps = tree.findIntersections(spatialQuery);

            for (SubscriptionWithRegion existing : overlaps) {
                if (!areCompatible(sub, existing)) continue;

                MergeEvaluation eval = evaluateMerge(accumulator, existing);
                if (eval.canMerge) {
                    maxExpansionPenalty = Math.max(maxExpansionPenalty, eval.penalty);
                    
                    // Track the largest existing region we successfully merged with
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
            // Update the log string to match the new unified metric
            explanation = String.format("EXPANDED [Max Expansion Penalty: %.5f]", maxExpansionPenalty);
        } else {
            explanation = createCleanExplanation(opResult, mergedCount, absorbedCount, isIdenticalReplacement);
        }
        
        // Pass the tracked existingTarget down the pipeline
        return new StoreUpdate(opResult, resultingEntry, explanation, absorbedCount, mergedCount, existingTarget);
    }

    protected abstract SubscriptionWithRegion wrapAggregatedRegion(SubscriptionWithRegion originalSub, Region accumulator, int mergedCount, int absorbedCount);

    private boolean areCompatible(SubscriptionWithRegion a, SubscriptionWithRegion b) {
        if (a instanceof ServiceOffer oa && b instanceof ServiceOffer ob) {
            return oa.getServiceId() == ob.getServiceId();
        }
        return !(a instanceof ServiceOffer) && !(b instanceof ServiceOffer);
    }

    @Override
    public int findMatches(Location loc, Map<TreeNode, List<SimulationSubscription>> resultsBuffer) {
        int[] opsCounter = new int[1];
        for (Map.Entry<TreeNode, RegionQuadTree> entry : map.entrySet()) {
            RegionQuadTree tree = entry.getValue();
            List<SimulationSubscription> hits = new ArrayList<>();
            
            tree.findMatches(loc, hits, opsCounter);
            
            if (!hits.isEmpty()) {
                resultsBuffer.put(entry.getKey(), hits);
            }
        }
        return opsCounter[0];
    }

    @Override
    public List<SimulationSubscription> getOutputFor(TreeNode target) {
        RegionQuadTree tree = map.get(target);
        return tree != null ? new ArrayList<>(tree.getAll()) : Collections.emptyList();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions() {
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        for (Map.Entry<TreeNode, RegionQuadTree> entry : map.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue().getAll()));
        }
        return result;
    }

    @Override
    public int size() {
        return map.values().stream().mapToInt(RegionQuadTree::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    protected double calculateSpatialFpr(Region accumulator, SubscriptionWithRegion existing) {
        float minLonA = (float) accumulator.getMinLon();
        float maxLonA = (float) accumulator.getMaxLon();
        float minLatA = (float) accumulator.getMinLat();
        float maxLatA = (float) accumulator.getMaxLat();
        
        float minLonB = (float) existing.getRegion().getMinLon();
        float maxLonB = (float) existing.getRegion().getMaxLon();
        float minLatB = (float) existing.getRegion().getMinLat();
        float maxLatB = (float) existing.getRegion().getMaxLat();

        float areaA = Region.fastArea(minLonA, maxLonA, minLatA, maxLatA);
        float areaB = Region.fastArea(minLonB, maxLonB, minLatB, maxLatB);
        float interArea = Region.fastIntersectionArea(minLonA, maxLonA, minLatA, maxLatA, minLonB, maxLonB, minLatB, maxLatB);
        float mergedArea = Region.fastMBRArea(minLonA, maxLonA, minLatA, maxLatA, minLonB, maxLonB, minLatB, maxLatB);

        double spatialFpr = 0.0;
        if (mergedArea > 1e-9) {
            double geometricUnion = areaA + areaB - interArea;
            spatialFpr = (mergedArea - geometricUnion) / mergedArea;
        }
        return spatialFpr;
    }

    // --- Embedded QuadTree ---
    protected static class RegionQuadTree {
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
            // 1. MBR Check Cost (1 Op)
            ops[0]++;
            if (cachedMBR == null || !cachedMBR.contains(loc)) return;

            // 2. Items Check Cost (N Ops)
            for (SubscriptionWithRegion s : items) {
                ops[0]++;
                if (s.getRegion().contains(loc)) {
                    hits.add(s);
                }
            }

            // 3. Child Traversal (Recursion)
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