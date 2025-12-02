package simulator.config;

import java.util.Properties;

import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Default;

public class TopologyConfig {
    public enum StrategyType { POLITICAL, RTREE, GRID, FIXED }

    // --- Generation Parameters (Used by Builder) ---
    public final StrategyType generationStrategy;

    public final int rTreeBranchingFactor;
    public final int rTreeLeafCapacity;
    public final double rTreeMaxCountryWidth;

    // --- Simulation Parameters (Used by Runner) ---
    public final StrategyType simulationStrategy;

    // --- Synthetic Topology Parameters ---
    public final int gridDimension;
    public final double overlapFactor;
    
    public final int randomMaxBranching;
    public final int randomNumRegions;
    public final double randomWorldWidth;
    public final double randomWorldHeight;

    public final int treeDepth;

    public TopologyConfig(Properties props) {
        // 1. Load Generation Strategy
        String genStratStr = props.getProperty("topology.generation.strategy", "POLITICAL").toUpperCase();
        try {
            this.generationStrategy = StrategyType.valueOf(genStratStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Config Error: Unknown topology.generation.strategy '" + genStratStr + "'");
        }

        // 2. Load Simulation Strategy
        String simStratStr = props.getProperty("topology.simulation.strategy", "POLITICAL").toUpperCase();
        try {
            this.simulationStrategy = StrategyType.valueOf(simStratStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Config Error: Unknown topology.simulation.strategy '" + simStratStr + "'");
        }

        // R-Tree Params
        this.rTreeBranchingFactor = parseInt(props, "topology.rtree.branchingFactor", "20");
        this.rTreeLeafCapacity = parseInt(props, "topology.rtree.leafCapacity", "50");
        this.rTreeMaxCountryWidth = parseDouble(props, "topology.rtree.maxCountryWidth", "20.0"); // Default to 20.0 degrees (approx width of Poland/Germany).

        // Grid Params
        this.gridDimension = parseInt(props, "topology.grid.dimension", "10");
        this.overlapFactor = parseDouble(props, "topology.grid.overlapFactor", "0.0");
        
        // Random Params
        this.randomMaxBranching = parseInt(props, "topology.random.branching", "4");
        this.randomNumRegions = parseInt(props, "topology.random.regions", "100");
        this.randomWorldWidth = parseDouble(props, "topology.random.width", "100.0");
        this.randomWorldHeight = parseDouble(props, "topology.random.height", "100.0");

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