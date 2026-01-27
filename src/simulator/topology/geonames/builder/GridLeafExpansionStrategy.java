package simulator.topology.geonames.builder;

import java.util.Objects;
import simulator.core.Location;
import simulator.regions.Region;

public class GridLeafExpansionStrategy implements LeafExpansionStrategy {
    private final boolean distributeEqually;
    private final int maxBranching;

    public GridLeafExpansionStrategy(boolean distributeEqually, int maxBranching) {
        this.distributeEqually = distributeEqually;
        this.maxBranching = maxBranching;
    }

    @Override
    public boolean expand(GeoNamesBuilderNode leafNode, long threshold, GeoNamesBuilderNode.NodeType childType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) return false;
        if (leafNode.bounds == null || leafNode.bounds.getBottomLeft() == null) return false;

        // Explicitly mark as non-leaf before adding children
        leafNode.isLeaf = false; 

        long parentPop = leafNode.aggregatedPopulation;
        long parentInternetPop = leafNode.internetPopulation;
        
        // 1. Calculate ideal children count
        int totalChildrenNeeded = distributeEqually 
            ? (int) Math.max(2, Math.ceil((double) parentPop / threshold))
            : (int) Math.max(2, (parentPop + threshold - 1) / threshold);

        // 2. Apply Branching Constraint
        int actualNumChildren = (maxBranching > 0 && totalChildrenNeeded > maxBranching) 
            ? maxBranching 
            : totalChildrenNeeded;

        // 3. Grid Layout
        int gridCols = (int) Math.ceil(Math.sqrt(actualNumChildren));
        int gridRows = (int) Math.ceil((double) actualNumChildren / gridCols);

        double startLat = leafNode.bounds.getBottomLeft().getY();
        double cellHeight = (gridRows > 0) ? leafNode.bounds.getHeight() / gridRows : 0;
        double startLon = leafNode.bounds.getBottomLeft().getX();
        double cellWidth = (gridCols > 0) ? leafNode.bounds.getWidth() / gridCols : 0;

        long remainingPop = parentPop;
        long internetPopSum = 0;

        for (int i = 0; i < actualNumChildren; i++) {
            String childName = leafNode.name + " part " + (i + 1);
            int childId = -(Objects.hash(leafNode.geonameId, childName)); 
            
            // New nodes are leaves by default (handled in Constructor)
            GeoNamesBuilderNode child = new GeoNamesBuilderNode(childId, childName, childType, leafNode.code, "ARTIFICIAL");

            // Population logic
            long childPop;
            if (distributeEqually) {
                childPop = parentPop / actualNumChildren + (i < parentPop % actualNumChildren ? 1 : 0);
            } else {
                // If hierarchical, prefer even split. If flat bin-packing, fill threshold.
                if (maxBranching > 0) {
                     childPop = parentPop / actualNumChildren + (i < parentPop % actualNumChildren ? 1 : 0);
                } else {
                     childPop = (i == actualNumChildren - 1 ? remainingPop : Math.min(remainingPop, threshold));
                }
            }
            
            remainingPop -= childPop;
            child.aggregatedPopulation = childPop;

            long iPop = (parentPop > 0) ? Math.round(((double) childPop / parentPop) * parentInternetPop) : 0;
            if (i == actualNumChildren - 1 && remainingPop <= 0) iPop = parentInternetPop - internetPopSum;
            
            child.internetPopulation = Math.max(0, iPop);
            internetPopSum += child.internetPopulation;

            int r = i / gridCols;
            int c = i % gridCols;
            double minLat = Math.max(leafNode.bounds.getBottomLeft().getY(), startLat + r * cellHeight);
            double maxLat = Math.min(leafNode.bounds.getTopRight().getY(), startLat + (r + 1) * cellHeight);
            double minLon = normalizeLongitude(startLon + c * cellWidth);
            double maxLon = normalizeLongitude(startLon + (c + 1) * cellWidth);
            
            child.bounds = new Region(new Location(minLon, minLat, 0), new Location(maxLon, maxLat, 0));
            leafNode.addChild(child);
            
            // --- RECURSIVE EXPANSION ---
            if (child.aggregatedPopulation > threshold) {
                this.expand(child, threshold, childType);
            }
        }
        return true;
    }

    private double normalizeLongitude(double lon) {
        while (lon <= -180.0) lon += 360.0;
        while (lon > 180.0) lon -= 360.0;
        return lon;
    }
}