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
    
    // Controls BOTH Subscription and Publication tracing
    // Is not final because RegressionSuiteRunner needs to set it to false
    public boolean enableEventTracing;

    public PathsConfig(Properties props) {
        this.resourcesDir = props.getProperty("paths.resourcesDir", "resources/world/");
        this.outputDir = props.getProperty("paths.outputDir", "output/");
        this.topologiesDir = this.outputDir + "topologies/";

        this.enableVerboseLogs = Boolean.parseBoolean(props.getProperty("paths.enableVerboseLogs", "false"));
        
        // Renamed property key to reflect broader usage
        this.enableEventTracing = Boolean.parseBoolean(props.getProperty("paths.enableEventTracing", "false"));

        this.allCountriesFile = resourcesDir + "allCountries.txt";
        this.countryInfoFile = resourcesDir + "countryInfo.txt";
        this.admin1CodesFile = resourcesDir + "admin1CodesASCII.txt";
        this.admin2CodesFile = resourcesDir + "admin2Codes.txt";
        this.internetPenetrationFile = resourcesDir + "internet_penetration_iso2.csv";

        this.useFullTopology = Boolean.parseBoolean(props.getProperty("paths.useFullTopology", "true"));
    }
    
    public String getActiveTopologyFile(TopologyConfig.StrategyType strategy) {
        String filePrefix;

        switch (strategy) {
            case POLITICAL -> filePrefix = "political_topology";
            case RTREE -> filePrefix = "rtree_topology";
            default -> {
                return null;
            }
        }

        if (!useFullTopology) {
            filePrefix = "subset_" + filePrefix;
        }

        String latestFile = findLatestTopologyFile(filePrefix);
        
        if (latestFile == null) {
            String msg = String.format("No topology file found in '%s' (or subdirs) starting with '%s'. Please run TopologyFileBuilder.", topologiesDir, filePrefix);
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        
        logger.info("Resolved latest topology file: " + latestFile);
        return latestFile;
    }

    private String findLatestTopologyFile(String prefix) {
        File baseDir = new File(topologiesDir);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return null;
        }

        File[] content = baseDir.listFiles();
        if (content == null || content.length == 0) return null;

        Arrays.sort(content, Comparator.comparing(File::getName).reversed());

        for (File fileOrDir : content) {
            if (fileOrDir.isDirectory()) {
                File[] matchingFiles = fileOrDir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".json"));
                if (matchingFiles != null && matchingFiles.length > 0) {
                    return matchingFiles[0].getAbsolutePath();
                }
            } 
            else if (fileOrDir.isFile()) {
                if (fileOrDir.getName().startsWith(prefix) && fileOrDir.getName().endsWith(".json")) {
                    return fileOrDir.getAbsolutePath();
                }
            }
        }

        return null;
    }
}