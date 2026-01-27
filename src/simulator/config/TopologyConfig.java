package simulator.config;

import java.util.Properties;

public class TopologyConfig {
    public enum StrategyType { POLITICAL, RTREE, GRID, FIXED }

    // --- Generation Parameters ---
    public final StrategyType generationStrategy;
    public final int branchingFactor;

    // --- Political Topology Specifics ---
    public final boolean enablePoliticalExpansion; 
    public final long politicalCoarseThreshold;    
    public final long politicalLeafCapacity;       
    
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
        this.generationStrategy = ConfigParser.parseEnum(props, "topology.generation.strategy", StrategyType.class, StrategyType.POLITICAL);
        this.simulationStrategy = ConfigParser.parseEnum(props, "topology.simulation.strategy", StrategyType.class, StrategyType.POLITICAL);

        // Unified Branching
        this.branchingFactor = ConfigParser.parseInt(props, "topology.branchingFactor", 20);
             
        // Political Params
        this.enablePoliticalExpansion = ConfigParser.parseBoolean(props, "topology.political.enableExpansion", true);
        this.politicalCoarseThreshold = ConfigParser.parseLong(props, "topology.political.threshold.adm3", 1000000L);
        
        // Handle legacy fallback logic implicitly or explicitly. 
        // Here we default to 10000 if not found, mirroring the original logic but cleaner.
        this.politicalLeafCapacity = ConfigParser.parseLong(props, "topology.political.leafCapacity", 10000L);

        // Analysis Flags
        this.enablePoliticalAnalysis = ConfigParser.parseBoolean(props, "topology.political.enableAnalysis", false);
        this.enableTopologyAnalysis = ConfigParser.parseBoolean(props, "topology.analysis.enabled", true);

        // R-Tree Params
        this.rTreeLeafCapacity = ConfigParser.parseInt(props, "topology.rtree.leafCapacity", 50);
        this.rTreeMaxCountryWidth = ConfigParser.parseDouble(props, "topology.rtree.maxCountryWidth", 20.0);

        // Grid Params
        this.gridDimension = ConfigParser.parseInt(props, "topology.grid.dimension", 10);
        this.overlapFactor = ConfigParser.parseDouble(props, "topology.grid.overlapFactor", 0.0);
        
        // Random Params
        this.randomMaxBranching = ConfigParser.parseInt(props, "topology.random.branching", 4);
        this.randomNumRegions = ConfigParser.parseInt(props, "topology.random.regions", 100);
        this.randomWorldWidth = ConfigParser.parseDouble(props, "topology.random.width", 100.0);
        this.randomWorldHeight = ConfigParser.parseDouble(props, "topology.random.height", 100.0);

        this.treeDepth = ConfigParser.parseInt(props, "topology.treeDepth", 4);
    }
}