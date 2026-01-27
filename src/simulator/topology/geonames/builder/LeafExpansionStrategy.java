package simulator.topology.geonames.builder;

/**
 * Strategy for splitting dense leaf nodes (e.g., Grid, QuadTree).
 */
public interface LeafExpansionStrategy {
    boolean expand(GeoNamesBuilderNode leafNode, long threshold, GeoNamesBuilderNode.NodeType childType);
}