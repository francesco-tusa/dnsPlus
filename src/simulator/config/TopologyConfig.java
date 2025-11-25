package simulator.config;

import java.util.Properties;

public class TopologyConfig {
    public enum StrategyType { POLITICAL, RTREE, GRID, FIXED }

    // GeoNames Topology Settings
    public final StrategyType strategy;
    public final int rTreeBranchingFactor;

    // Grid Topology Settings
    public final int gridDimension;
    public final double overlapFactor;
    
    // Random Topology Settings
    
    public final int randomMaxBranching;
    public final int randomNumRegions;
    public final double randomWorldWidth;
    public final double randomWorldHeight;

    // Random or Grid Topology Settings
    public final int treeDepth;

    public TopologyConfig(Properties props) {
        String stratStr = props.getProperty("topology.strategy", "POLITICAL").toUpperCase();
        try {
            this.strategy = StrategyType.valueOf(stratStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Config Error: Unknown topology.strategy '" + stratStr + "'. Valid: POLITICAL, RTREE, GRID, FIXED");
        }

        this.rTreeBranchingFactor = parseInt(props, "topology.rtreeBranchingFactor", "20");

        // Grid Topology params
        this.gridDimension = parseInt(props, "topology.grid.dimension", "10");
        this.overlapFactor = parseDouble(props, "topology.grid.overlapFactor", "0.0");
        
        // Random Topology params
        this.randomMaxBranching = parseInt(props, "topology.random.branching", "4");
        this.randomNumRegions = parseInt(props, "topology.random.regions", "100");
        this.randomWorldWidth = parseDouble(props, "topology.random.width", "100.0");
        this.randomWorldHeight = parseDouble(props, "topology.random.height", "100.0");

        // Random or Grid Topology params
        this.treeDepth = parseInt(props, "topology.treeDepth", "4");
    }
    
    private int parseInt(Properties props, String key, String defaultVal) {
        try { return Integer.parseInt(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { return Integer.parseInt(defaultVal); }
    }
    
    private double parseDouble(Properties props, String key, String defaultVal) {
        try { return Double.parseDouble(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { return Double.parseDouble(defaultVal); }
    }
}