package simulator.config;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class PathsConfig {
    private static final Logger logger = CustomLogger.getLogger(PathsConfig.class.getName());

    // Directories
    public final String resourcesDir;
    public final String outputDir;
    public final String topologiesDir;

    // Input Files
    public final String allCountriesFile;
    public final String countryInfoFile;
    public final String admin1CodesFile;
    public final String admin2CodesFile;
    public final String internetPenetrationFile;
    
    // Flags
    private final boolean useFullTopology;
    public final boolean enableVerboseLogs;

    public PathsConfig(Properties props) {
        this.resourcesDir = props.getProperty("paths.resourcesDir", "resources/world/");
        this.outputDir = props.getProperty("paths.outputDir", "output/");
        // New dedicated directory for topology files
        this.topologiesDir = this.outputDir + "topologies/";

        this.enableVerboseLogs = Boolean.parseBoolean(props.getProperty("paths.enableVerboseLogs", "false"));
        // enableVisualisation removed as per previous refactoring

        this.allCountriesFile = resourcesDir + "allCountries.txt";
        this.countryInfoFile = resourcesDir + "countryInfo.txt";
        this.admin1CodesFile = resourcesDir + "admin1CodesASCII.txt";
        this.admin2CodesFile = resourcesDir + "admin2Codes.txt";
        this.internetPenetrationFile = resourcesDir + "internet_penetration_iso2.csv";

        this.useFullTopology = Boolean.parseBoolean(props.getProperty("paths.useFullTopology", "true"));
    }
    
    /**
     * Returns the absolute path to the latest topology file for the given strategy.
     * Searches in output/topologies/ for files matching the pattern:
     * [subset_]{strategy}_topology_{timestamp}.json
     */
    public String getActiveTopologyFile(TopologyConfig.StrategyType strategy) {
        String filePrefix;

        switch (strategy) {
            case POLITICAL -> filePrefix = "political_topology";
            case RTREE -> filePrefix = "rtree_topology";
            default -> {
                // Fixed, Grid, Random do not use the file loader mechanism
                return null;
            }
        }

        // Adjust prefix if we are running a subset simulation
        if (!useFullTopology) {
            filePrefix = "subset_" + filePrefix;
        }

        String latestFile = findLatestTopologyFile(filePrefix);
        
        if (latestFile == null) {
            String msg = String.format("No topology file found in '%s' starting with '%s'. Please run TopologyFileBuilder first.", topologiesDir, filePrefix);
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        
        logger.info("Resolved latest topology file: " + latestFile);
        return latestFile;
    }

    /**
     * Scans the directory and returns the path of the file with the largest timestamp (latest).
     */
    private String findLatestTopologyFile(String prefix) {
        File dir = new File(topologiesDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        // Filter files that start with the prefix and end with .json
        File[] matchingFiles = dir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".json"));

        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        // Sort by name in descending order. 
        // Since filenames end with a timestamp, the lexicographically largest name is the latest.
        Arrays.sort(matchingFiles, Comparator.comparing(File::getName).reversed());

        return matchingFiles[0].getAbsolutePath();
    }
}