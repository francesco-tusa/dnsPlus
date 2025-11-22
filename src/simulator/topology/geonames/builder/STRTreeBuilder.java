package simulator.topology.geonames.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import simulator.core.Location;
import simulator.regions.Region;

/**
 * Implements the Sort-Tile-Recursive (STR) bulk loading algorithm 
 * to build efficient R-Trees from static data.
 */
public class STRTreeBuilder {

    /**
     * Builds a tree level from raw Data Points (Leaves are generic PPL nodes).
     */
    public static GeoNamesBuilderNode build(List<GeoNamesEntry> points, int maxChildren) {
        List<GeoNamesBuilderNode> nodes = new ArrayList<>(points.size());
        for (GeoNamesEntry p : points) {
            GeoNamesBuilderNode leaf = new GeoNamesBuilderNode();
            leaf.geonameId = p.geonameId;
            leaf.name = p.name;
            leaf.type = GeoNamesBuilderNode.NodeType.PPL;
            leaf.aggregatedPopulation = p.population;
            // Note: internetPopulation calculated later via Country stats
            
            leaf.bounds = new Region(new Location(p.longitude, p.latitude, 0), new Location(p.longitude, p.latitude, 0));
            nodes.add(leaf);
        }
        return buildFromNodes(nodes, maxChildren);
    }

    public static GeoNamesBuilderNode buildFromNodes(List<GeoNamesBuilderNode> nodes, int maxChildren) {
        if (nodes.size() <= maxChildren) {
            return createParent(nodes);
        }

        int P = (int) Math.ceil((double) nodes.size() / maxChildren);
        int S = (int) Math.ceil(Math.sqrt(P));

        nodes.sort(Comparator.comparingDouble(n -> n.bounds.getCenter().getX()));

        List<GeoNamesBuilderNode> nextLevelNodes = new ArrayList<>();
        int sliceCapacity = S * maxChildren;
        
        for (int i = 0; i < nodes.size(); i += sliceCapacity) {
            int end = Math.min(i + sliceCapacity, nodes.size());
            List<GeoNamesBuilderNode> slice = nodes.subList(i, end);
            
            slice.sort(Comparator.comparingDouble(n -> n.bounds.getCenter().getY()));

            for (int j = 0; j < slice.size(); j += maxChildren) {
                int chunkEnd = Math.min(j + maxChildren, slice.size());
                List<GeoNamesBuilderNode> children = slice.subList(j, chunkEnd);
                nextLevelNodes.add(createParent(children));
            }
        }
        return buildFromNodes(nextLevelNodes, maxChildren);
    }

    private static GeoNamesBuilderNode createParent(List<GeoNamesBuilderNode> children) {
        GeoNamesBuilderNode parent = new GeoNamesBuilderNode();
        parent.type = GeoNamesBuilderNode.NodeType.ADM1; 
        parent.geonameId = -(children.get(0).geonameId); 
        parent.name = "Cluster_" + Math.abs(parent.geonameId);
        parent.bounds = new Region();
        
        for (GeoNamesBuilderNode child : children) {
            parent.addChild(child);
            parent.aggregatedPopulation += child.aggregatedPopulation;
            
            // --- Sum Internet Population ---
            parent.internetPopulation += child.internetPopulation;
            
            if (child.bounds != null) {
                if (parent.bounds.getBottomLeft() == null) {
                    parent.bounds = new Region(child.bounds);
                } else {
                    parent.bounds.expand(child.bounds);
                }
            }
        }
        return parent;
    }
}