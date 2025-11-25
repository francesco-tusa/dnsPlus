package simulator.config;

import java.util.Properties;

public class PathsConfig {
    // Directories
    public final String resourcesDir;
    public final String outputDir;

    // Input Files (Resources)
    public final String allCountriesFile;
    public final String countryInfoFile;
    public final String admin1CodesFile;
    public final String admin2CodesFile;
    public final String internetPenetrationFile;
    
    // Output Files (Topologies)
    public final String fullTopologyPolitical;
    public final String fullTopologyRTree;
    public final String subsetTopology;
    
    // Configuration Flag
    private final boolean useFullTopology;
    public final boolean enableVerboseLogs;

    public PathsConfig(Properties props) {
        // Base Directories and verbosity
        this.resourcesDir = props.getProperty("paths.resourcesDir", "resources/world/");
        this.outputDir = props.getProperty("paths.outputDir", "output/");
        this.enableVerboseLogs = Boolean.parseBoolean(props.getProperty("paths.enableVerboseLogs", "false"));
        
        // Input Files (Constructed from resourcesDir)
        this.allCountriesFile = resourcesDir + "allCountries.txt";
        this.countryInfoFile = resourcesDir + "countryInfo.txt";
        this.admin1CodesFile = resourcesDir + "admin1CodesASCII.txt";
        this.admin2CodesFile = resourcesDir + "admin2Codes.txt";
        this.internetPenetrationFile = resourcesDir + "internet_penetration_iso2.csv";

        // Output Files (Constructed from outputDir)
        this.fullTopologyPolitical = outputDir + "geonames_topology_political.json";
        this.fullTopologyRTree = outputDir + "geonames_topology_rtree.json";
        this.subsetTopology = outputDir + "geonames_subset_political_bangladesh_beijing.json";
        
        this.useFullTopology = Boolean.parseBoolean(props.getProperty("paths.useFullTopology", "true"));
    }
    
    /**
     * Returns the topology file path based on the configuration flag.
     * Useful for generic simulation runners to pick the 'active' topology.
     * * Note: This logic defaults to Political for Full topology. 
     * If you want RTree, you might need to check TopologyConfig.strategy as well,
     * but for simple path resolution, this is the baseline.
     */
    public String getActiveTopologyFile() {
        if (!useFullTopology) {
            return subsetTopology;
        }

        // If Full Topology is requested, we default to Political unless logic is added to check Strategy
        return fullTopologyPolitical; 
    }
}