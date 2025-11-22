package simulator.topology.geonames.builder;

import java.util.Objects;
import simulator.core.Location;
import simulator.regions.Region;
import utils.CustomLogger;

public class GridLeafExpansionStrategy implements LeafExpansionStrategy {
    private final boolean distributeEqually;

    public GridLeafExpansionStrategy(boolean distributeEqually) {
        this.distributeEqually = distributeEqually;
    }

    @Override
    public boolean expand(GeoNamesBuilderNode leafNode, long threshold, GeoNamesBuilderNode.NodeType childType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) return false;
        if (leafNode.bounds == null || leafNode.bounds.getBottomLeft() == null) return false;

        long parentPop = leafNode.aggregatedPopulation;
        long parentInternetPop = leafNode.internetPopulation;
        
        // 1. Calculate Grid Size
        int numChildren = distributeEqually 
            ? (int) Math.max(2, Math.ceil((double) parentPop / threshold))
            : (int) Math.max(2, (parentPop + threshold - 1) / threshold);

        int gridCols = (int) Math.ceil(Math.sqrt(numChildren));
        int gridRows = (int) Math.ceil((double) numChildren / gridCols);

        double startLat = leafNode.bounds.getBottomLeft().getY();
        double cellHeight = (gridRows > 0) ? leafNode.bounds.getHeight() / gridRows : 0;
        double startLon = leafNode.bounds.getBottomLeft().getX();
        double cellWidth = (gridCols > 0) ? leafNode.bounds.getWidth() / gridCols : 0;

        long remainingPop = parentPop;
        long internetPopSum = 0;

        for (int i = 0; i < numChildren; i++) {
            String childName = leafNode.name + " part " + (i + 1);
            int childId = -(Objects.hash(leafNode.geonameId, childName)); // Artificial ID
            
            GeoNamesBuilderNode child = new GeoNamesBuilderNode(childId, childName, childType, leafNode.code, "ARTIFICIAL");

            // Population logic
            long childPop = distributeEqually 
                ? parentPop / numChildren + (i < parentPop % numChildren ? 1 : 0)
                : (i == numChildren - 1 ? remainingPop : Math.min(remainingPop, threshold));
            remainingPop -= childPop;
            child.aggregatedPopulation = childPop;

            // Internet Pop logic
            long iPop = (parentPop > 0) ? Math.round(((double) childPop / parentPop) * parentInternetPop) : 0;
            if (i == numChildren - 1) iPop = parentInternetPop - internetPopSum;
            child.internetPopulation = Math.max(0, iPop);
            internetPopSum += child.internetPopulation;

            // Bounds logic
            int r = i / gridCols;
            int c = i % gridCols;
            double minLat = Math.max(leafNode.bounds.getBottomLeft().getY(), startLat + r * cellHeight);
            double maxLat = Math.min(leafNode.bounds.getTopRight().getY(), startLat + (r + 1) * cellHeight);
            double minLon = normalizeLongitude(startLon + c * cellWidth);
            double maxLon = normalizeLongitude(startLon + (c + 1) * cellWidth);
            
            child.bounds = new Region(new Location(minLon, minLat, 0), new Location(maxLon, maxLat, 0));
            leafNode.addChild(child);
        }
        return true;
    }

    private double normalizeLongitude(double lon) {
        while (lon <= -180.0) lon += 360.0;
        while (lon > 180.0) lon -= 360.0;
        return lon;
    }
}