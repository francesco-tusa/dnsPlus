package simulator.topology.geonames.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import simulator.core.Location;
import simulator.regions.Region;
import simulator.topology.geonames.builder.GeoNamesBuilderNode.NodeType;

/**
 * Helper class that implements the Sort-Tile-Recursive (STR) algorithm.
 * Now supports flexible node typing to create Hybrid Political/Spatial trees.
 */
public class STRBulkLoader {

    /**
     * Builds a tree from raw points (Populated Places).
     * Used for the "Below Country" phase.
     * * @param points Raw data points.
     * @param branchFactor Max children per node.
     * @param leafCapacity PPLs per Leaf Broker.
     * @param internalNodeType The type to assign to internal nodes (e.g., ADM1).
     * @return The root node of this subtree.
     */
    public static GeoNamesBuilderNode build(List<GeoNamesEntry> points, int branchFactor, int leafCapacity, NodeType internalNodeType) {
        List<GeoNamesBuilderNode> pointNodes = new ArrayList<>(points.size());
        for (GeoNamesEntry p : points) {
            GeoNamesBuilderNode n = new GeoNamesBuilderNode();
            n.geonameId = p.geonameId;
            n.name = p.name;
            n.type = NodeType.PPL;
            n.aggregatedPopulation = p.population;
            n.bounds = new Region(new Location(p.longitude, p.latitude, 0), new Location(p.longitude, p.latitude, 0));
            pointNodes.add(n);
        }

        // Pass 1: Pack Points into Leaf Clusters (Leaf Brokers)
        // These are always typed as ADM2 (or similar) to represent the bottom-most routing node.
        List<GeoNamesBuilderNode> leafBrokers = packLayer(pointNodes, leafCapacity, true, NodeType.ADM2);

        // Pass 2: Build the upper hierarchy using Branch Factor
        List<GeoNamesBuilderNode> currentLayer = leafBrokers;
        while (currentLayer.size() > 1) {
            currentLayer = packLayer(currentLayer, branchFactor, false, internalNodeType);
        }

        return currentLayer.isEmpty() ? null : currentLayer.get(0);
    }

    /**
     * Builds a tree from existing Nodes (e.g., Countries).
     * Used for the "Above Country" phase.
     * * @param nodes The existing nodes to cluster (e.g., List<CountryNode>).
     * @param maxChildren Max children per container.
     * @param containerType The type to assign to the new containers (e.g., CONTINENT).
     * @return The root node (World).
     */
    public static GeoNamesBuilderNode buildFromNodes(List<GeoNamesBuilderNode> nodes, int maxChildren, NodeType containerType) {
        List<GeoNamesBuilderNode> currentLayer = nodes;
        while (currentLayer.size() > 1) {
            currentLayer = packLayer(currentLayer, maxChildren, false, containerType);
        }
        return currentLayer.isEmpty() ? null : currentLayer.get(0);
    }

    private static List<GeoNamesBuilderNode> packLayer(List<GeoNamesBuilderNode> inputNodes, int capacity, boolean isLeafLayer, NodeType parentType) {
        List<GeoNamesBuilderNode> nextLayer = new ArrayList<>();
        if (inputNodes.isEmpty()) return nextLayer;

        int P = (int) Math.ceil((double) inputNodes.size() / capacity);
        int S = (int) Math.ceil(Math.sqrt(P));

        // Sort by X (Longitude)
        inputNodes.sort(Comparator.comparingDouble(n -> n.bounds.getCenter().getX()));

        int sliceCapacity = S * capacity;
        for (int i = 0; i < inputNodes.size(); i += sliceCapacity) {
            int end = Math.min(i + sliceCapacity, inputNodes.size());
            List<GeoNamesBuilderNode> slice = inputNodes.subList(i, end);
            
            // Sort by Y (Latitude)
            slice.sort(Comparator.comparingDouble(n -> n.bounds.getCenter().getY()));

            for (int j = 0; j < slice.size(); j += capacity) {
                int chunkEnd = Math.min(j + capacity, slice.size());
                List<GeoNamesBuilderNode> chunk = slice.subList(j, chunkEnd);
                nextLayer.add(createParent(chunk, isLeafLayer, parentType));
            }
        }
        return nextLayer;
    }

    private static GeoNamesBuilderNode createParent(List<GeoNamesBuilderNode> chunk, boolean isLeafLayer, NodeType type) {
        GeoNamesBuilderNode parent = new GeoNamesBuilderNode();
        parent.type = type; // Use the requested type (ADM1, CONTINENT, etc.)
        
        if (isLeafLayer) {
            // This is a Leaf Broker containing points
            parent.name = chunk.get(0).name + "_Cluster";
        } else {
            // This is a routing container (Spatial Continent or State)
            parent.name = "Spatial_" + type + "_" + Math.abs(chunk.get(0).geonameId);
        }
        
        // Artificial negative ID to avoid collisions
        parent.geonameId = -(chunk.get(0).geonameId); 
        parent.bounds = new Region();
        
        for (GeoNamesBuilderNode child : chunk) {
            // If it's a Leaf Layer of points, we DO NOT add children (Broker logic).
            // If it's an upper layer (clustering Brokers or Countries), we DO add children.
            if (!isLeafLayer) {
                parent.addChild(child);
            }
            
            parent.aggregatedPopulation += child.aggregatedPopulation;
            parent.internetPopulation += child.internetPopulation;
            
            if (child.bounds != null) {
                if (parent.bounds.getBottomLeft() == null) parent.bounds = new Region(child.bounds);
                else parent.bounds.expand(child.bounds);
            }
        }
        return parent;
    }
}