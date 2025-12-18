package simulator.config;

import java.util.Properties;

public class TopologyConfig {
    public enum StrategyType { POLITICAL, RTREE, GRID, FIXED }

    // --- Generation Parameters ---
    public final StrategyType generationStrategy;
    public final int branchingFactor;

    // --- Political Topology Specifics ---
    public final boolean enablePoliticalExpansion; // Master switch for Grid Expansion
    public final long politicalCoarseThreshold;    // Pass 8 Threshold
    public final long politicalLeafCapacity;       // Pass 9 Threshold
    
    // Analysis Flags
    public final boolean enablePoliticalAnalysis;
    public final boolean enableTopologyAnalysis;

    // --- R-tree Specifics ---
    public final int rTreeLeafCapacity;
    public final double rTreeMaxCountryWidth;

    // --- Simulation Parameters ---
    public final StrategyType simulationStrategy;

    // --- Synthetic Topology ---
    public final int gridDimension;
    public final double overlapFactor;
    
    public final int randomMaxBranching;
    public final int randomNumRegions;
    public final double randomWorldWidth;
    public final double randomWorldHeight;

    public final int treeDepth;

    public TopologyConfig(Properties props) {
        // Strategies
        this.generationStrategy = StrategyType.valueOf(props.getProperty("topology.generation.strategy", "POLITICAL").toUpperCase());
        this.simulationStrategy = StrategyType.valueOf(props.getProperty("topology.simulation.strategy", "POLITICAL").toUpperCase());

        // Unified Branching
        this.branchingFactor = parseInt(props, "topology.branchingFactor", "20");
             
        // Political Params
        // Toggle for Grid Expansion (Pass 8 & 9)
        this.enablePoliticalExpansion = Boolean.parseBoolean(props.getProperty("topology.political.enableExpansion", "true"));
        
        this.politicalCoarseThreshold = parseLong(props, "topology.political.threshold.adm3", "1000000");
        
        String legacyVal = props.getProperty("topology.political.threshold.adm4", "10000");
        this.politicalLeafCapacity = parseLong(props, "topology.political.leafCapacity", legacyVal);

        // Analysis Flags
        this.enablePoliticalAnalysis = Boolean.parseBoolean(props.getProperty("topology.political.enableAnalysis", "false"));
        this.enableTopologyAnalysis = Boolean.parseBoolean(props.getProperty("topology.analysis.enabled", "true"));

        // R-Tree Params
        this.rTreeLeafCapacity = parseInt(props, "topology.rtree.leafCapacity", "50");
        this.rTreeMaxCountryWidth = parseDouble(props, "topology.rtree.maxCountryWidth", "20.0");

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

    private long parseLong(Properties props, String key, String defaultVal) {
        try { return Long.parseLong(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { return Long.parseLong(defaultVal); }
    }
    
    private double parseDouble(Properties props, String key, String defaultVal) {
        try { return Double.parseDouble(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { return Double.parseDouble(defaultVal); }
    }
}