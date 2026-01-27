package simulator.topology.geonames.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import simulator.core.Location;
import simulator.regions.Region;
import simulator.topology.geonames.builder.GeoNamesBuilderNode.NodeType;

public class STRBulkLoader {

    // Safety padding (in degrees) to account for subscribers near the edge of a leaf broker's region.
    // This compensates for the loss of "overlap safety" caused by the Country Explosion optimization.
    // 0.55 degrees roughly matches half the subscription region size (1.0).
    private static final double LEAF_REGION_PADDING = 0.75; 

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

        List<GeoNamesBuilderNode> leafBrokers = packLayer(pointNodes, leafCapacity, true, NodeType.ADM2);

        List<GeoNamesBuilderNode> currentLayer = leafBrokers;
        while (currentLayer.size() > 1) {
            currentLayer = packLayer(currentLayer, branchFactor, false, internalNodeType);
        }

        return currentLayer.isEmpty() ? null : currentLayer.get(0);
    }

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

        inputNodes.sort(Comparator.comparingDouble(n -> n.bounds.getCenter().getX()));

        int sliceCapacity = S * capacity;
        for (int i = 0; i < inputNodes.size(); i += sliceCapacity) {
            int end = Math.min(i + sliceCapacity, inputNodes.size());
            List<GeoNamesBuilderNode> slice = inputNodes.subList(i, end);
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
        parent.type = type;
        
        if (isLeafLayer) {
            parent.name = chunk.get(0).name + "_Cluster";
        } else {
            parent.name = "Spatial_" + type + "_" + Math.abs(chunk.get(0).geonameId);
        }
        
        parent.geonameId = -(chunk.get(0).geonameId); 
        parent.bounds = new Region();
        
        for (GeoNamesBuilderNode child : chunk) {
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

        // Apply padding to Leaf Brokers ONLY.
        // This ensures the broker's MBR covers the "Interest Region" of subscribers at the edge.
        if (isLeafLayer && parent.bounds.getBottomLeft() != null) {
            expandBounds(parent.bounds, LEAF_REGION_PADDING);
        }

        return parent;
    }

    /**
     * Expands the region by a fixed degree amount in all directions.
     */
    private static void expandBounds(Region r, double padding) {
        Location bl = r.getBottomLeft();
        Location tr = r.getTopRight();
        
        Location newBl = new Location(bl.getX() - padding, bl.getY() - padding, 0);
        Location newTr = new Location(tr.getX() + padding, tr.getY() + padding, 0);
        
        r.set(new Region(newBl, newTr));
    }
}