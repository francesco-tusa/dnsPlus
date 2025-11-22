package simulator.topology;

/**
 * Central configuration for Topology JSON file paths and Resource definitions.
 */
public class TopologyPaths {

    // --- Directories ---
    public static final String RESOURCES_DIR = "resources/world/";
    public static final String OUTPUT_DIR = "output/";

    // --- Specific Output Files ---
    // Political Topology
    public static final String FULL_TOPOLOGY_POLITICAL = OUTPUT_DIR + "geonames_topology_political.json";
    public static final String SUBSET_TOPOLOGY_POLITICAL = OUTPUT_DIR + "geonames_subset_political_bangladesh_beijing.json";

    // R-Tree Topology
    public static final String FULL_TOPOLOGY_RTREE = OUTPUT_DIR + "geonames_topology_rtree.json";
    public static final String SUBSET_TOPOLOGY_RTREE = OUTPUT_DIR + "geonames_subset_rtree_bangladesh_beijing.json";

    // --- Default Aliases (Change these to switch experiment modes) ---
    public static final String FULL_TOPOLOGY = FULL_TOPOLOGY_RTREE;
    public static final String SUBSET_TOPOLOGY = SUBSET_TOPOLOGY_POLITICAL;

    // --- Input Resources ---
    public static final String ALL_COUNTRIES_FILE = RESOURCES_DIR + "allCountries.txt";
    public static final String COUNTRY_INFO_FILE = RESOURCES_DIR + "countryInfo.txt";
    public static final String ADMIN1_CODES_FILE = RESOURCES_DIR + "admin1CodesASCII.txt";
    public static final String ADMIN2_CODES_FILE = RESOURCES_DIR + "admin2Codes.txt";
    public static final String INTERNET_PENETRATION_FILE = RESOURCES_DIR + "internet_penetration_iso2.csv";

    // Prevent instantiation
    private TopologyPaths() {}
}