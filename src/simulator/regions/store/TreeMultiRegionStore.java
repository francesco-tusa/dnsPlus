package simulator.regions.store;

import java.util.*;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class TreeMultiRegionStore extends AbstractMultiRegionStore {

    private final Map<TreeNode, RegionQuadTree> map = new HashMap<>();

    public TreeMultiRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        RegionQuadTree tree = map.computeIfAbsent(source,
                k -> new RegionQuadTree(-180, -90, 180, 90));

        // 1. Filter Check (O(log N))
        List<SubscriptionWithRegion> candidates = tree.findCandidatesContaining(sub.getRegion());
        for (SubscriptionWithRegion existing : candidates) {
            if (existing.contains(sub)) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Filtered", 0, 0);
            }
        }

        Region accumulator = sub.getRegion().copy();
        boolean mergedInPass;
        int absorbedCount = 0;
        int mergedCount = 0;
        double absorbedArea = 0.0;

        // 2. Absorb/Merge Loop (O(log N))
        do {
            mergedInPass = false;
            List<SubscriptionWithRegion> overlaps = tree.findIntersections(accumulator);

            for (SubscriptionWithRegion existing : overlaps) {
                if (accumulator.contains(existing.getRegion())) {
                    absorbedArea += existing.getArea();
                    tree.remove(existing);
                    absorbedCount++;
                } else if (shouldMerge(accumulator, existing)) {
                    accumulator.expand(existing.getRegion());
                    tree.remove(existing);
                    mergedInPass = true;
                    mergedCount++;
                    break;
                }
            }
        } while (mergedInPass);

        SubscriptionWithRegion resultingEntry = new SubscriptionWithRegion(accumulator);
        tree.insert(resultingEntry);

        double accumArea = accumulator.getArea();
        boolean isIdenticalReplacement = (mergedCount == 0
                && Math.abs(accumArea - (sub.getArea() + absorbedArea)) < 1e-9);

        StoreOpResult opResult = determineResult(absorbedCount, mergedCount, isIdenticalReplacement);
        String explanation = createCleanExplanation(opResult, mergedCount, absorbedCount, isIdenticalReplacement);
        return new StoreUpdate(opResult, resultingEntry, explanation, absorbedCount, mergedCount);
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

    // =========================================================================
    // RegionQuadTree (Double Precision)
    // =========================================================================
    private static class RegionQuadTree {
        private static final int MAX_ITEMS = 16;
        private static final int MAX_DEPTH = 10;

        private final double minLon, minLat, maxLon, maxLat;
        private final List<SubscriptionWithRegion> items;
        private RegionQuadTree[] children;
        private final int depth;

        // Track MBR at the node level
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
            // 1. Efficiently update MBR
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
            boolean removed = false;
            if (items.remove(sub)) {
                removed = true;
            } else if (children != null) {
                int index = getIndex(sub.getRegion());
                if (index != -1) {
                    removed = children[index].remove(sub);
                } else {
                    for (RegionQuadTree child : children) {
                        if (child.remove(sub)) {
                            removed = true;
                            break;
                        }
                    }
                }
            }
            if (removed) {
                recalculateMBR();
            }
            return removed;
        }

        private void recalculateMBR() {
            this.cachedMBR = null;
            for (SubscriptionWithRegion s : items) {
                if (cachedMBR == null)
                    cachedMBR = new Region(s.getRegion());
                else
                    cachedMBR.expand(s.getRegion());
            }
            if (children != null) {
                for (RegionQuadTree child : children) {
                    Region childMBR = child.getMBR();
                    if (childMBR != null) {
                        if (cachedMBR == null)
                            cachedMBR = new Region(childMBR);
                        else
                            cachedMBR.expand(childMBR);
                    }
                }
            }
        }

        public Region getMBR() {
            return cachedMBR;
        }

        public List<SubscriptionWithRegion> getAll() {
            List<SubscriptionWithRegion> all = new ArrayList<>(items);
            if (children != null) {
                for (RegionQuadTree child : children)
                    all.addAll(child.getAll());
            }
            return all;
        }

        public int size() {
            int count = items.size();
            if (children != null) {
                for (RegionQuadTree child : children)
                    count += child.size();
            }
            return count;
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
                int index = getPointIndex(loc.getX(), loc.getY());
                if (index != -1) {
                    children[index].findMatches(loc, hits, ops);
                }
            }
        }

        public List<SubscriptionWithRegion> findCandidatesContaining(Region query) {
            // Strict check: If the MBR of this node doesn't contain the query,
            // no single item inside can possibly contain it.
            if (cachedMBR == null || !cachedMBR.contains(query)) {
                return Collections.emptyList();
            }

            List<SubscriptionWithRegion> candidates = new ArrayList<>();
            for (SubscriptionWithRegion s : items) {
                if (s.getRegion().contains(query))
                    candidates.add(s);
            }

            if (children != null) {
                int index = getIndex(query);
                if (index != -1) {
                    // Only a child that fully encloses the query region can contain a
                    // subscription that fully encloses the query region.
                    candidates.addAll(children[index].findCandidatesContaining(query));
                }
            }
            return candidates;
        }

        public List<SubscriptionWithRegion> findIntersections(Region query) {
            if (cachedMBR == null || !cachedMBR.intersects(query))
                return Collections.emptyList();

            List<SubscriptionWithRegion> hits = new ArrayList<>();
            for (SubscriptionWithRegion s : items) {
                if (s.getRegion().intersects(query))
                    hits.add(s);
            }

            if (children != null) {
                for (RegionQuadTree child : children) {
                    // CHANGED: Use child.getMBR() safely
                    Region childMBR = child.getMBR();
                    if (childMBR != null && childMBR.intersects(query)) {
                        hits.addAll(child.findIntersections(query));
                    }
                }
            }
            return hits;
        }

        private void split() {
            double midLon = (minLon + maxLon) / 2;
            double midLat = (minLat + maxLat) / 2;
            children = new RegionQuadTree[4];
            children[0] = new RegionQuadTree(minLon, midLat, midLon, maxLat, depth + 1); // NW
            children[1] = new RegionQuadTree(midLon, midLat, maxLon, maxLat, depth + 1); // NE
            children[2] = new RegionQuadTree(minLon, minLat, midLon, midLat, depth + 1); // SW
            children[3] = new RegionQuadTree(midLon, minLat, maxLon, midLat, depth + 1); // SE
        }

        private int getIndex(Region r) {
            double midLon = (minLon + maxLon) / 2;
            double midLat = (minLat + maxLat) / 2;
            boolean top = r.getMinLat() >= midLat; // Use >= to match point logic
            boolean bottom = r.getMaxLat() <= midLat;
            boolean left = r.getMaxLon() <= midLon;
            boolean right = r.getMinLon() >= midLon;

            if (top) {
                if (left)
                    return 0; // NW
                if (right)
                    return 1; // NE
            } else if (bottom) {
                if (left)
                    return 2; // SW
                if (right)
                    return 3; // SE
            }
            return -1;
        }

        private int getPointIndex(double x, double y) {
            double midLon = (minLon + maxLon) / 2;
            double midLat = (minLat + maxLat) / 2;
            boolean top = y >= midLat;
            boolean left = x <= midLon;
            if (top)
                return left ? 0 : 1;
            else
                return left ? 2 : 3;
        }
    }
}