package simulator.config;

import java.util.Properties;

public class PathsConfig {
    // Directories
    public final String resourcesDir;
    public final String outputDir;

    // Input Files
    public final String allCountriesFile;
    public final String countryInfoFile;
    public final String admin1CodesFile;
    public final String admin2CodesFile;
    public final String internetPenetrationFile;
    
    // Output Files
    public final String fullTopologyPolitical;
    public final String fullTopologyRTree;
    public final String subsetTopology;
    
    // Flags
    private final boolean useFullTopology;
    public final boolean enableVerboseLogs;
    public final boolean enableVisualisation;

    public PathsConfig(Properties props) {
        this.resourcesDir = props.getProperty("paths.resourcesDir", "resources/world/");
        this.outputDir = props.getProperty("paths.outputDir", "output/");

        this.enableVerboseLogs = Boolean.parseBoolean(props.getProperty("paths.enableVerboseLogs", "false"));
        this.enableVisualisation = Boolean.parseBoolean(props.getProperty("paths.enableVisualisation", "true"));

        this.allCountriesFile = resourcesDir + "allCountries.txt";
        this.countryInfoFile = resourcesDir + "countryInfo.txt";
        this.admin1CodesFile = resourcesDir + "admin1CodesASCII.txt";
        this.admin2CodesFile = resourcesDir + "admin2Codes.txt";
        this.internetPenetrationFile = resourcesDir + "internet_penetration_iso2.csv";

        this.fullTopologyPolitical = outputDir + "geonames_topology_political.json";
        this.fullTopologyRTree = outputDir + "geonames_topology_rtree.json";
        this.subsetTopology = outputDir + "geonames_subset_political_bangladesh_beijing.json";
        
        this.useFullTopology = Boolean.parseBoolean(props.getProperty("paths.useFullTopology", "true"));
    }
    
    /**
     * Returns the topology file path based on the simulation strategy.
     */
    public String getActiveTopologyFile(TopologyConfig.StrategyType strategy) {
        if (!useFullTopology) {
            return subsetTopology;
        }

        return switch (strategy) {
            case RTREE -> fullTopologyRTree;
            case POLITICAL -> fullTopologyPolitical;
            default -> fullTopologyPolitical; // Fallback for GRID/FIXED/RANDOM if they used files
        };
    }
}