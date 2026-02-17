package marketplace.topology.store;

import java.util.*;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.*;
import marketplace.events.ServiceOffer;
import marketplace.common.MetricHyperCube;

public class MarketplaceRegionStore extends AbstractMultiRegionStore {

    private final Map<TreeNode, RegionQuadTree> map = new HashMap<>();

    public MarketplaceRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        RegionQuadTree tree = map.computeIfAbsent(source,
                k -> new RegionQuadTree(-180, -90, 180, 90));

        // 1. Filter Check
        List<SubscriptionWithRegion> candidates = tree.findCandidatesContaining(sub.getRegion());
        for (SubscriptionWithRegion existing : candidates) {
            if (areCompatible(sub, existing) && existing.contains(sub)) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Filtered", 0, 0);
            }
        }

        Region accumulator = sub.getRegion().copy();

        boolean mergedInPass;
        int absorbedCount = 0;
        int mergedCount = 0;
        double absorbedArea = 0.0;

        // 2. Absorb/Merge Loop
        do {
            mergedInPass = false;

            // Search spatial QuadTree using only the physical 2D bounds.
            // This prevents disjoint FaaS points in QoS space from being pre-filtered
            // before the N-Dimensional FPR math (shouldMerge) can evaluate them.
            Region spatialQuery = new Region(accumulator);
            List<SubscriptionWithRegion> overlaps = tree.findIntersections(spatialQuery);

            for (SubscriptionWithRegion existing : overlaps) {
                // Ensure we only merge offers for the same service type
                if (!areCompatible(sub, existing))
                    continue;

                if (accumulator.contains(existing.getRegion())) {
                    absorbedArea += existing.getArea();
                    tree.remove(existing);
                    absorbedCount++;
                } else if (shouldMerge(accumulator, existing)) {
                    accumulator.expand(existing.getRegion()); // Merges HyperCube dimensions
                    tree.remove(existing);
                    mergedInPass = true;
                    mergedCount++;
                    break;
                }
            }
        } while (mergedInPass);

        // 3. Create Result (Robust Type Preservation)
        SubscriptionWithRegion resultingEntry;

        // Ensure we don't lose ServiceOffer type if we have the metadata
        if (sub instanceof ServiceOffer offer && accumulator instanceof MetricHyperCube mhc) {

            // If no cross-offer merging occurred, preserve the exact location
            if (mergedCount == 0 && absorbedCount == 0) {
                resultingEntry = ServiceOffer.createWithUpdatedRegion(offer, mhc);
            } else {
                resultingEntry = ServiceOffer.createAggregated(offer.getServiceId(), mhc);
            }

        } else {
            // [DEBUG] Warning if we degrade a ServiceOffer to generic
            if (sub instanceof ServiceOffer) {
                System.err.println("[MarketplaceRegionStore] WARNING: ServiceOffer degraded to generic Subscription! "
                        + "Accumulator type: " + accumulator.getClass().getSimpleName());
            }
            resultingEntry = new SubscriptionWithRegion(accumulator);
        }

        tree.insert(resultingEntry);

        double accumArea = accumulator.getArea();
        boolean isIdenticalReplacement = (mergedCount == 0
                && Math.abs(accumArea - (sub.getArea() + absorbedArea)) < 1e-9);

        StoreOpResult opResult = determineResult(absorbedCount, mergedCount, isIdenticalReplacement);
        String explanation = createCleanExplanation(opResult, mergedCount, absorbedCount, isIdenticalReplacement);
        return new StoreUpdate(opResult, resultingEntry, explanation, absorbedCount, mergedCount);
    }

    private boolean areCompatible(SubscriptionWithRegion a, SubscriptionWithRegion b) {
        if (a instanceof ServiceOffer oa && b instanceof ServiceOffer ob) {
            return oa.getServiceId() == ob.getServiceId();
        }
        return !(a instanceof ServiceOffer) && !(b instanceof ServiceOffer);
    }

    @Override
    public int findMatches(Location loc, List<TreeNode> resultsBuffer) {
        int[] opsCounter = new int[1];
        for (Map.Entry<TreeNode, RegionQuadTree> entry : map.entrySet()) {
            if (entry.getValue().containsPoint(loc, opsCounter)) {
                resultsBuffer.add(entry.getKey());
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
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        if (this.mergeThreshold <= 0.0) return false;

        // Fallback to standard 2D volume math if not dealing with FaaS QoS metrics
        if (!(accumulator instanceof MetricHyperCube) || !(existing.getRegion() instanceof MetricHyperCube)) {
            return super.shouldMerge(accumulator, existing);
        }

        MetricHyperCube cubeA = (MetricHyperCube) accumulator;
        MetricHyperCube cubeB = (MetricHyperCube) existing.getRegion();

        // 1. Calculate PURE 2D Spatial FPR (Zero-Allocation, Antimeridian-Aware)
        // Extract bounds and cast to float to match the fast static methods in Region.java
        float minLonA = (float) cubeA.getMinLon();
        float maxLonA = (float) cubeA.getMaxLon();
        float minLatA = (float) cubeA.getMinLat();
        float maxLatA = (float) cubeA.getMaxLat();
        
        float minLonB = (float) cubeB.getMinLon();
        float maxLonB = (float) cubeB.getMaxLon();
        float minLatB = (float) cubeB.getMinLat();
        float maxLatB = (float) cubeB.getMaxLat();

        // Use the existing highly-optimized spherical math
        float areaA = Region.fastArea(minLonA, maxLonA, minLatA, maxLatA);
        float areaB = Region.fastArea(minLonB, maxLonB, minLatB, maxLatB);
        float interArea = Region.fastIntersectionArea(minLonA, maxLonA, minLatA, maxLatA, minLonB, maxLonB, minLatB, maxLatB);
        float mergedArea = Region.fastMBRArea(minLonA, maxLonA, minLatA, maxLatA, minLonB, maxLonB, minLatB, maxLatB);

        double spatialFpr = 0.0;
        if (mergedArea > 1e-9) {
            double geometricUnion = areaA + areaB - interArea;
            spatialFpr = (mergedArea - geometricUnion) / mergedArea;
        }

        // 2. Calculate QoS Dimension FPR (Metric Spread)
        double[] minA = cubeA.getMinValues();
        double[] maxA = cubeA.getMaxValues();
        double[] minB = cubeB.getMinValues();
        double[] maxB = cubeB.getMaxValues();

        double totalRelativeExpansion = 0.0;
        int dimensions = minA.length;

        for (int i = 0; i < dimensions; i++) {
            double currentRange = maxA[i] - minA[i];
            double newMin = Math.min(minA[i], minB[i]);
            double newMax = Math.max(maxA[i], maxB[i]);
            double mergedRange = newMax - newMin;
            
            double expansion = mergedRange - currentRange;
            
            if (expansion > 0) {
                double scale = Math.max(Math.abs(newMax), 1e-6); 
                totalRelativeExpansion += (expansion / scale);
            }
        }

        double qosFpr = totalRelativeExpansion / dimensions;

        // 3. Continuum Aggregation Logic: 
        // We take the MAX of physical and logical dilution. BOTH must satisfy the threshold.
        double combinedFpr = Math.max(spatialFpr, qosFpr);
        
        return combinedFpr <= this.mergeThreshold;
    }

    @Override
    public int size() {
        return map.values().stream().mapToInt(RegionQuadTree::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    // --- Embedded QuadTree (Standard implementation) ---
    private static class RegionQuadTree {
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
            if (cachedMBR == null)
                cachedMBR = new Region(sub.getRegion());
            else
                cachedMBR.expand(sub.getRegion());
            if (children != null) {
                int index = getIndex(sub.getRegion());
                if (index != -1) {
                    children[index].insert(sub);
                    return;
                }
            }
            items.add(sub);
            if (items.size() > MAX_ITEMS && depth < MAX_DEPTH) {
                if (children == null)
                    split();
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
                if (index != -1)
                    removed = children[index].remove(sub);
                else
                    for (RegionQuadTree child : children)
                        if (child.remove(sub)) {
                            removed = true;
                            break;
                        }
            }
            if (removed)
                recalculateMBR();
            return removed;
        }

        private void recalculateMBR() {
            cachedMBR = null;
            for (SubscriptionWithRegion s : items) {
                if (cachedMBR == null)
                    cachedMBR = new Region(s.getRegion());
                else
                    cachedMBR.expand(s.getRegion());
            }
            if (children != null)
                for (RegionQuadTree c : children)
                    if (c.getMBR() != null) {
                        if (cachedMBR == null)
                            cachedMBR = new Region(c.getMBR());
                        else
                            cachedMBR.expand(c.getMBR());
                    }
        }

        public Region getMBR() {
            return cachedMBR;
        }

        public List<SubscriptionWithRegion> getAll() {
            List<SubscriptionWithRegion> all = new ArrayList<>(items);
            if (children != null)
                for (RegionQuadTree c : children)
                    all.addAll(c.getAll());
            return all;
        }

        public int size() {
            int c = items.size();
            if (children != null)
                for (RegionQuadTree child : children)
                    c += child.size();
            return c;
        }

        public boolean containsPoint(Location loc, int[] ops) {
            ops[0]++;
            if (cachedMBR == null || !cachedMBR.contains(loc))
                return false;
            for (SubscriptionWithRegion s : items) {
                ops[0]++;
                if (s.getRegion().contains(loc))
                    return true;
            }
            if (children != null) {
                int idx = getPointIndex(loc.getX(), loc.getY());
                if (idx != -1)
                    return children[idx].containsPoint(loc, ops);
            }
            return false;
        }

        public List<SubscriptionWithRegion> findCandidatesContaining(Region query) {
            if (cachedMBR == null || !cachedMBR.contains(query))
                return Collections.emptyList();
            List<SubscriptionWithRegion> c = new ArrayList<>();
            for (SubscriptionWithRegion s : items)
                if (s.getRegion().contains(query))
                    c.add(s);
            if (children != null) {
                int idx = getIndex(query);
                if (idx != -1)
                    c.addAll(children[idx].findCandidatesContaining(query));
            }
            return c;
        }

        public List<SubscriptionWithRegion> findIntersections(Region query) {
            if (cachedMBR == null || !cachedMBR.intersects(query))
                return Collections.emptyList();
            List<SubscriptionWithRegion> h = new ArrayList<>();
            for (SubscriptionWithRegion s : items)
                if (s.getRegion().intersects(query))
                    h.add(s);
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
            if (top) {
                if (left)
                    return 0;
                if (right)
                    return 1;
            } else if (bottom) {
                if (left)
                    return 2;
                if (right)
                    return 3;
            }
            return -1;
        }

        private int getPointIndex(double x, double y) {
            double mx = (minLon + maxLon) / 2, my = (minLat + maxLat) / 2;
            boolean top = y >= my, left = x <= mx;
            return top ? (left ? 0 : 1) : (left ? 2 : 3);
        }
    }
}