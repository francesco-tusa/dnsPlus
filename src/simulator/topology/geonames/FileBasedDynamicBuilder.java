package simulator.topology.geonames;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Helper class to store and update bounding box coordinates.
 */
class BoundingBox {
    // Initialize with invalid values to ensure first point sets the bounds
    double minLat = 91.0;
    double maxLat = -91.0;
    double minLon = 181.0;
    double maxLon = -181.0;

    // Extends this bounding box to include the coordinates of another point
    void extend(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon))
            return; // Ignore invalid coords
        if (lat < minLat)
            minLat = lat;
        if (lat > maxLat)
            maxLat = lat;
        if (lon < minLon)
            minLon = lon;
        if (lon > maxLon)
            maxLon = lon;
    }

    // Extends this bounding box to include another bounding box
    void extend(BoundingBox other) {
        if (other == null || !other.isValid())
            return; // Ignore invalid boxes
        if (other.minLat < minLat)
            minLat = other.minLat;
        if (other.maxLat > maxLat)
            maxLat = other.maxLat;
        if (other.minLon < minLon)
            minLon = other.minLon;
        if (other.maxLon < maxLon)
            maxLon = other.maxLon;
    }

    // Checks if the bounding box has been initialized with valid coordinates
    boolean isValid() {
        return minLat <= 90.0 && maxLat >= -90.0 && minLon <= 180.0 && maxLon >= -180.0 && minLat <= maxLat
                && minLon <= maxLon;
    }

    @Override
    public String toString() {
        if (!isValid()) {
            return "Invalid BBox";
        }
        return String.format("[(%.4f, %.4f) - (%.4f, %.4f)]", minLat, minLon, maxLat, maxLon);
    }
}

/**
 * Represents an entry parsed from a GeoNames data file (like allCountries.txt).
 * Includes robust parsing for required fields including lat/lon.
 */
class GeonameEntry {
    int geonameId;
    String name;
    String featureClass;
    String featureCode;
    String countryCode; // ISO 3166-1 alpha-2 code
    String admin1Code;
    String admin2Code;
    long population;
    int elevation;
    double latitude = Double.NaN;
    double longitude = Double.NaN;

    public GeonameEntry(String[] parts) {
        try {
            final int ID_IDX = 0;
            final int NAME_IDX = 1;
            final int LAT_IDX = 4;
            final int LON_IDX = 5;
            final int FEAT_CLASS_IDX = 6;
            final int FEAT_CODE_IDX = 7;
            final int COUNTRY_CODE_IDX = 8; // This is the ISO2 code
            final int ADMIN1_CODE_IDX = 10;
            final int ADMIN2_CODE_IDX = 11;
            final int POPULATION_IDX = 14;
            final int ELEVATION_IDX = 15;
            final int MIN_EXPECTED_PARTS = 19;
            if (parts.length < MIN_EXPECTED_PARTS) {
                this.geonameId = -1;
                return;
            }
            this.geonameId = Integer.parseInt(parts[ID_IDX].trim());
            this.name = parts[NAME_IDX].trim();
            this.featureClass = parts[FEAT_CLASS_IDX].trim();
            this.featureCode = parts[FEAT_CODE_IDX].trim();
            this.countryCode = parts[COUNTRY_CODE_IDX].trim(); // Store ISO2 code
            this.admin1Code = parts[ADMIN1_CODE_IDX].trim();
            this.admin2Code = parts[ADMIN2_CODE_IDX].trim();
            try {
                this.latitude = Double.parseDouble(parts[LAT_IDX].trim());
            } catch (NumberFormatException | NullPointerException e) { /* Keep NaN */ }
            try {
                this.longitude = Double.parseDouble(parts[LON_IDX].trim());
            } catch (NumberFormatException | NullPointerException e) { /* Keep NaN */ }
            String populationStr = parts[POPULATION_IDX].trim();
            if (populationStr.isEmpty()) {
                this.population = 0;
            } else {
                try {
                    this.population = Long.parseLong(populationStr);
                } catch (NumberFormatException nfe) {
                    this.population = 0; /* Log */
                }
            }
            String elevationStr = parts[ELEVATION_IDX].trim();
            if (elevationStr.isEmpty()) {
                this.elevation = 0;
            } else {
                try {
                    this.elevation = Integer.parseInt(elevationStr);
                } catch (NumberFormatException nfe) {
                    this.elevation = 0;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Critical parse error: " + String.join("|", parts) + " - " + e.getMessage());
            this.geonameId = -1;
        }
    }
}

/**
 * Represents a node in the hierarchical tree structure.
 * Added fields for official population, internet penetration, and internet population.
 */
class TreeNode {
    int geonameId; // 0 for World/Continent, negative for artificial
    String name;
    String featureCode; // Original feature code (PCLI, ADM1, CONT, ARTIFICIAL)
    String code; // admin code, country code, continent code, or inherited code
    NodeType type;
    long aggregatedPopulation; // Population aggregated/scaled from children or PPLs
    long internetPopulation; // Estimated internet users based on scaled pop and rate
    long officialPopulation; // Official population from PCLI entry (only for COUNTRY nodes)
    double internetPenetrationRate; // Country-level penetration rate (0.0 to 1.0)
    BoundingBox bounds;
    List<TreeNode> children = new ArrayList<>();

    enum NodeType {
        WORLD, CONTINENT, COUNTRY, ADM1, ADM2, S_ADM3, S_ADM4, PPL // PPL is used internally but not added to tree
    }

    // Main constructor for ALL node types
    TreeNode(int geonameId, String name, NodeType type, String code, String featureCode) {
        this.geonameId = geonameId;
        this.name = name;
        this.type = type;
        this.code = (code != null ? code : "");
        this.featureCode = (featureCode != null ? featureCode : "");
        this.aggregatedPopulation = 0; // Initialize pop to 0
        this.internetPopulation = 0;   // Initialize internet pop to 0
        this.officialPopulation = 0;   // Initialize official pop to 0
        this.internetPenetrationRate = 0.0; // Initialize rate to 0.0
        this.bounds = null;
    }

    // Constructor for Continent/World specifically (calls main constructor)
    TreeNode(String name, NodeType type, String code) {
        this(0, name, type, code, (type == NodeType.CONTINENT ? "CONT" : "WORLD"));
    }

    void addChild(TreeNode child) {
        if (child != null)
            children.add(child);
    }

    @Override
    public String toString() {
        String boundsStr = (bounds != null && bounds.isValid()) ? ", bounds=" + bounds : "";
        String officialPopStr = (type == NodeType.COUNTRY && officialPopulation > 0) ? ", officialPop=" + officialPopulation : "";
        String rateStr = (type == NodeType.COUNTRY && internetPenetrationRate > 0) ? String.format(", rate=%.3f", internetPenetrationRate) : "";
        return name + " (" + type + (code != null && !code.isEmpty() ? ", code=" + code : "") + ", id=" + geonameId
                + ", pop=" + aggregatedPopulation
                + ", internetPop=" + internetPopulation // Added internet pop
                + officialPopStr + rateStr // Added official pop and rate for countries
                + boundsStr
                + (children.isEmpty() ? "" : ", children=" + children.size()) + ")";
    }

    // Modified printTree to write to PrintWriter
    public void printTree(PrintWriter writer, String indent) {
        writer.println(indent + this); // Use writer
        children.sort(Comparator.comparing(a -> a.name));
        for (TreeNode child : children) {
            child.printTree(writer, indent + "  "); // Pass writer down recursively
        }
    }
}

/**
 * Lightweight holder for relevant PCLI/ADM1/ADM2 info collected in Pass 1.
 */
class RelevantAdminInfo {
    int geonameId;
    String name;
    TreeNode.NodeType type;
    String code; // admin code (ADM1/ADM2) or country ISO2 code (PCLI)
    String featureCode;
    String countryCode; // ISO2 code (always present)
    String admin1Code; // Specific ADM1 code part
    long officialPopulation; // Store official population for PCLI

    RelevantAdminInfo(GeonameEntry entry, TreeNode.NodeType type) {
        this.geonameId = entry.geonameId;
        this.name = entry.name;
        this.type = type;
        this.featureCode = entry.featureCode;
        this.countryCode = entry.countryCode; // ISO2 code from entry
        this.admin1Code = entry.admin1Code;
        this.officialPopulation = 0; // Default

        switch (type) {
            case COUNTRY:
                this.code = entry.countryCode; // For COUNTRY type, 'code' is the ISO2 code
                this.officialPopulation = entry.population; // Store official pop for PCLI
                break;
            case ADM1:
                this.code = entry.admin1Code; // For ADM1 type, 'code' is the ADM1 part
                break;
            case ADM2:
                this.code = entry.admin2Code; // For ADM2 type, 'code' is the ADM2 part
                break;
            default:
                this.code = "";
        }
    }
}

/**
 * Main class to build dynamically deep hierarchy from files.
 */
public class FileBasedDynamicBuilder {

    // --- Maps loaded from smaller files ---
    Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    Map<String, String> countryToContinentMap = new HashMap<>();
    Map<String, Integer> countryCodeToIdMap = new HashMap<>(); // Maps ISO2 code to Geoname ID
    Map<String, Double> countryIsoToPenetrationMap = new HashMap<>(); // Maps ISO2 code to penetration rate
    Map<Integer, Long> countryIdToOfficialPopulationMap = new HashMap<>(); // Maps Country Geoname ID to official PCLI population

    // --- Data collected from Pass 1 ---
    Map<Integer, RelevantAdminInfo> relevantAdminMap = new HashMap<>();
    Map<Integer, Long> adm1PopulationMap = new HashMap<>(); // Aggregated from PPLs
    Map<Integer, BoundingBox> adm1BoundsMap = new HashMap<>();
    Map<Integer, Long> adm2PopulationMap = new HashMap<>(); // Aggregated from PPLs
    Map<Integer, BoundingBox> adm2BoundsMap = new HashMap<>();

    // --- Final Tree structure ---
    Map<Integer, TreeNode> nodeMap = new HashMap<>(); // Maps Geoname ID to TreeNode

    // --- Constants ---
    final long POPULATION_THRESHOLD_1M = 1_000_000;
    final long POPULATION_THRESHOLD_10K = 10_000;

    // --- Loading methods ---
    void loadAdminCodes(String filePath, Map<String, Integer> map, String adminLevelName) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t");
                // Use parts.length >= 4 for admin codes as they have geonameid at index 3
                if (parts.length >= 4) {
                    String key = parts[0].trim(); // Key is like US.CA or US.CA.037
                    try {
                        int geonameId = Integer.parseInt(parts[3].trim());
                        map.put(key, geonameId);
                        count++;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.err.println(
                                "Skipping invalid " + adminLevelName + " line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + adminLevelName + " codes file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded " + count + " " + adminLevelName + " code mappings from " + filePath);
    }

    void loadCountryInfo(String filePath) {
        final int ISO_CODE_IDX = 0; // ISO 3166-1 alpha-2
        final int CONTINENT_CODE_IDX = 8;
        final int GEONAMEID_IDX = 16; // Index for country geonameid
        final int MIN_COUNTRY_PARTS = 17; // Need at least 17 fields for geonameid

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1); // Split and keep trailing empty strings
                if (parts.length >= MIN_COUNTRY_PARTS) {
                    String isoCode = parts[ISO_CODE_IDX].trim();
                    String continentCode = parts[CONTINENT_CODE_IDX].trim();
                    String geonameIdStr = parts[GEONAMEID_IDX].trim();

                    if (!isoCode.isEmpty() && !continentCode.isEmpty()) {
                        countryToContinentMap.put(isoCode, continentCode);
                    }
                    // Also map ISO code to Geoname ID if available
                    if (!isoCode.isEmpty() && !geonameIdStr.isEmpty()) {
                        try {
                            int geonameId = Integer.parseInt(geonameIdStr);
                            countryCodeToIdMap.put(isoCode, geonameId);
                        } catch (NumberFormatException e) {
                            System.err.println("Skipping country line due to invalid Geoname ID: " + line);
                        }
                    }
                } else {
                     System.err.println("Skipping short country info line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading country info file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded " + countryToContinentMap.size() + " country->continent mappings from " + filePath);
        System.out.println("Loaded " + countryCodeToIdMap.size() + " country ISO2->GeonameID mappings from " + filePath);
    }

    /**
     * Loads internet penetration data from the CSV file.
     * Finds the latest year with valid data for each country.
     * Stores the rate as a decimal (e.g., 85.5% -> 0.855).
     *
     * @param filePath Path to the internet penetration CSV file.
     */
     void loadInternetPenetration(String filePath) {
        System.out.println("Loading Internet Penetration data from " + filePath + "...");
        countryIsoToPenetrationMap.clear();
        int validRatesLoaded = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Read header
            if (line == null) {
                System.err.println("Error: Internet penetration file is empty.");
                return;
            }

            // Process header to find year columns and their indices
            String[] headers = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Handle quoted commas
            List<Integer> yearIndices = new ArrayList<>();
            List<Integer> years = new ArrayList<>();
            int isoCodeIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].replace("\"", "").trim(); // Clean header
                if ("ISO2 Code".equalsIgnoreCase(header)) {
                    isoCodeIndex = i;
                } else if (header.matches("\\d{4} \\[YR\\d{4}\\]")) { // Match format like "2023 [YR2023]"
                    try {
                        int year = Integer.parseInt(header.substring(0, 4));
                        yearIndices.add(i);
                        years.add(year);
                    } catch (NumberFormatException e) {
                        // Ignore columns that don't match the year format
                    }
                }
            }

            if (isoCodeIndex == -1) {
                System.err.println("Error: Could not find 'ISO2 Code' column in penetration file.");
                return;
            }
            if (yearIndices.isEmpty()) {
                System.err.println("Error: Could not find any valid year columns (e.g., '2023 [YR2023]') in penetration file.");
                return;
            }

            // Process data rows
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length > isoCodeIndex) {
                    String isoCode = parts[isoCodeIndex].replace("\"", "").trim();
                    if (isoCode.isEmpty()) continue; // Skip rows with no ISO code

                    double latestRate = -1.0;
                    int latestYear = -1;

                    // Iterate through year columns from latest to earliest
                    for (int i = yearIndices.size() - 1; i >= 0; i--) {
                        int colIndex = yearIndices.get(i);
                        int currentYear = years.get(i);

                        if (parts.length > colIndex) {
                            String rateStr = parts[colIndex].replace("\"", "").trim();
                            if (!rateStr.isEmpty() && !rateStr.equals("..")) {
                                try {
                                    double rate = Double.parseDouble(rateStr);
                                    // Assuming rate is a percentage, convert to decimal
                                    latestRate = rate / 100.0;
                                    latestYear = currentYear;
                                    break; // Found the latest valid rate for this country
                                } catch (NumberFormatException e) {
                                    // Ignore invalid number formats like ".."
                                }
                            }
                        }
                    }

                    if (latestRate >= 0.0) { // Check if a valid rate was found
                        countryIsoToPenetrationMap.put(isoCode, latestRate);
                        validRatesLoaded++;
                         // System.out.println("  Loaded rate for " + isoCode + ": " + latestRate + " (from year " + latestYear + ")");
                    } else {
                         // System.out.println("  No valid rate found for " + isoCode);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading internet penetration file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded latest internet penetration rates for " + validRatesLoaded + " countries.");
    }


     // --- Pass 1: Process allCountries.txt ---
     // Modified to store official population for PCLI entries
     void processAllCountriesPass1(String filePath) {
        System.out.println("Starting Pass 1: Identifying features, aggregating PPL population & bounds, storing PCLI pop...");
        int lineCount = 0, pplCount = 0, pcliCount = 0, adm1Count = 0, adm2Count = 0;
        long totalPplPop = 0;
        long skippedPplPop = 0;
        int skippedPplCount = 0;
        countryIdToOfficialPopulationMap.clear(); // Clear map before populating

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = line.split("\t", -1);
                final int MIN_EXPECTED_PARTS = 19;
                if (parts.length < MIN_EXPECTED_PARTS) { continue; }

                GeonameEntry entry = new GeonameEntry(parts);
                if (entry.geonameId == -1 || entry.countryCode == null || entry.countryCode.isEmpty()) {
                     continue;
                }

                TreeNode.NodeType type = determineNodeType(entry.featureClass, entry.featureCode);
                if (type == null) continue;

                // --- Store info for Admin/Country types ---
                if (type == TreeNode.NodeType.COUNTRY) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info);
                    // Use countryCodeToIdMap loaded from countryInfo.txt for consistency if available
                    // but also store the official population found here keyed by the PCLI geonameId
                    if (entry.population > 0) {
                        countryIdToOfficialPopulationMap.put(entry.geonameId, entry.population);
                    }
                    // We still need countryCodeToIdMap for linking ADM1s later if PCLI entry is missing in allCountries
                    // This map is primarily built from countryInfo.txt now.
                    pcliCount++;
                } else if (type == TreeNode.NodeType.ADM1) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info); adm1Count++;
                } else if (type == TreeNode.NodeType.ADM2) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info); adm2Count++;

                // --- Process PPL for Aggregation ---
                } else if (type == TreeNode.NodeType.PPL) {
                    Integer parentAdm1Id = null;
                    Integer parentAdm2Id = null;
                    String adm1Key = null;

                    // Try to find ADM1 parent ID
                    if (entry.admin1Code != null && !entry.admin1Code.isEmpty()) {
                        adm1Key = entry.countryCode + "." + entry.admin1Code;
                        parentAdm1Id = admin1CodeToIdMap.get(adm1Key);
                    }

                    // Try to find ADM2 parent ID
                    if (parentAdm1Id != null && entry.admin2Code != null && !entry.admin2Code.isEmpty()) {
                        String adm2Key = entry.countryCode + "." + entry.admin1Code + "." + entry.admin2Code;
                        parentAdm2Id = admin2CodeToIdMap.get(adm2Key);
                    }

                    // Aggregate Population if possible
                    if (entry.population > 0) {
                        boolean popAdded = false;
                        if (parentAdm1Id != null) {
                            adm1PopulationMap.put(parentAdm1Id, adm1PopulationMap.getOrDefault(parentAdm1Id, 0L) + entry.population);
                            popAdded = true;
                        }
                        if (parentAdm2Id != null) {
                            adm2PopulationMap.put(parentAdm2Id, adm2PopulationMap.getOrDefault(parentAdm2Id, 0L) + entry.population);
                        }

                        if (popAdded) {
                            pplCount++;
                            totalPplPop += entry.population;
                        } else {
                            skippedPplCount++;
                            skippedPplPop += entry.population;
                            // Optional: Log skipped PPLs for debugging
                            // System.err.println("[Population Skipped] ID: " + entry.geonameId + ...);
                        }
                    }

                    // Aggregate Bounds
                    if (!Double.isNaN(entry.latitude) && !Double.isNaN(entry.longitude)) {
                        if (parentAdm1Id != null) { BoundingBox bbox1 = adm1BoundsMap.computeIfAbsent(parentAdm1Id, k -> new BoundingBox()); bbox1.extend(entry.latitude, entry.longitude); }
                        if (parentAdm2Id != null) { BoundingBox bbox2 = adm2BoundsMap.computeIfAbsent(parentAdm2Id, k -> new BoundingBox()); bbox2.extend(entry.latitude, entry.longitude); }
                    }
                } // End PPL block

                if (lineCount % 1000000 == 0) {
                    System.out.println("  Processed " + lineCount + " lines...");
                }
            } // End while loop
        } catch (IOException e) {
            System.err.println("Error reading geonames file in Pass 1: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Pass 1 Complete. Found PCLI: " + pcliCount + ", ADM1: " + adm1Count + ", ADM2: " + adm2Count);
        System.out.println("Aggregated population from " + pplCount + " PPLs: " + totalPplPop + " into "
                + adm1PopulationMap.size() + " ADM1 & " + adm2PopulationMap.size() + " ADM2 regions.");
        System.out.println("Skipped " + skippedPplCount + " PPLs with total population " + skippedPplPop + " due to missing/unmatched ADM1 link.");
        System.out.println("Stored official population for " + countryIdToOfficialPopulationMap.size() + " PCLI entries.");
    }

    private TreeNode.NodeType determineNodeType(String fcl, String fcode) {
        if ("PCLI".equals(fcode))
            return TreeNode.NodeType.COUNTRY;
        if ("ADM1".equals(fcode))
            return TreeNode.NodeType.ADM1;
        if ("ADM2".equals(fcode))
            return TreeNode.NodeType.ADM2;
        // Treat PPLA/PPLC etc. also as PPL for aggregation purposes
        if ("P".equals(fcl) && fcode != null && (fcode.startsWith("PPL") || fcode.equals("PPLA") || fcode.equals("PPLC")))
            return TreeNode.NodeType.PPL;
        return null;
    }

    // --- Pass 2: Build Initial Hierarchy (World -> Continent -> Country -> ADM1) ---
    // Modified to set official population and penetration rate on country nodes
    TreeNode buildInitialAdm1Hierarchy() {
        System.out.println("Starting Pass 2: Building initial hierarchy (World -> Continent -> Country -> ADM1)...");
        TreeNode worldRoot = new TreeNode("World", TreeNode.NodeType.WORLD, "WORLD");
        Map<String, TreeNode> continentNodes = new HashMap<>();
        nodeMap.clear();
        nodeMap.put(0, worldRoot); // Add world root to the map

        // Create nodes for Countries and ADM1s first
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            if (info.type == TreeNode.NodeType.COUNTRY || info.type == TreeNode.NodeType.ADM1) {
                TreeNode node = new TreeNode(info.geonameId, info.name, info.type, info.code, info.featureCode);

                // If it's a country node, set official population and penetration rate
                if (node.type == TreeNode.NodeType.COUNTRY) {
                    // Get official population stored from Pass 1 (keyed by PCLI geonameid)
                    node.officialPopulation = countryIdToOfficialPopulationMap.getOrDefault(node.geonameId, 0L);
                    // Get penetration rate (keyed by ISO2 code, which is node.code for countries)
                    node.internetPenetrationRate = countryIsoToPenetrationMap.getOrDefault(node.code, 0.0);
                    if (node.officialPopulation == 0) {
                         System.err.println("Warning: Country " + node.name + " (ID: " + node.geonameId + ", Code: " + node.code + ") has 0 official population from PCLI entry.");
                    }
                     if (node.internetPenetrationRate == 0) {
                         System.err.println("Warning: Country " + node.name + " (Code: " + node.code + ") has 0.0 internet penetration rate.");
                     }
                }
                nodeMap.put(info.geonameId, node);
            }
        }
        System.out.println("  Created " + (nodeMap.size() -1) + " initial TreeNodes (PCLI + ADM1)."); // -1 for world

        // Link nodes together
        int countryLinks = 0, adm1Links = 0, failedLinks = 0;
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            TreeNode childNode = nodeMap.get(info.geonameId);
            if (childNode == null) continue; // Skip if node wasn't created (e.g., not PCLI or ADM1)

            TreeNode parentNode = null;
            if (childNode.type == TreeNode.NodeType.ADM1) {
                // Find parent Country using the country's ISO code from the ADM1's info
                Integer parentCountryId = countryCodeToIdMap.get(info.countryCode); // Use ISO2 code from ADM1 info
                if (parentCountryId != null) {
                    parentNode = nodeMap.get(parentCountryId);
                }
                if (parentNode != null && parentNode.type == TreeNode.NodeType.COUNTRY) {
                    parentNode.addChild(childNode);
                    adm1Links++;
                } else {
                    failedLinks++;
                    System.err.println("Failed to link ADM1: " + childNode.name + " (ID: " + childNode.geonameId + ") - Parent country (ISO: " + info.countryCode + ", ID: " + parentCountryId + ") not found or not a COUNTRY node.");
                }
            } else if (childNode.type == TreeNode.NodeType.COUNTRY) {
                // Find parent Continent using the country's ISO code (childNode.code)
                String continentCode = countryToContinentMap.get(childNode.code);
                if (continentCode != null) {
                    parentNode = continentNodes.computeIfAbsent(continentCode, k -> {
                        // Simplified continent name lookup
                        String continentName = switch (k) {
                            case "EU" -> "Europe"; case "AS" -> "Asia"; case "AF" -> "Africa";
                            case "NA" -> "North America"; case "SA" -> "South America";
                            case "OC" -> "Oceania"; case "AN" -> "Antarctica";
                            default -> k; // Use code if name unknown
                        };
                        TreeNode newNode = new TreeNode(continentName, TreeNode.NodeType.CONTINENT, k);
                        worldRoot.addChild(newNode);
                        // Add continent node to the main nodeMap as well? Maybe not needed if only linking countries.
                        return newNode;
                    });
                } else {
                    // If continent unknown, link directly to World
                    parentNode = worldRoot;
                    System.err.println("Warning: Continent not found for country: " + childNode.name + " (Code: " + childNode.code + "). Linking to World.");
                }
                parentNode.addChild(childNode);
                countryLinks++;
            }
        }
        System.out.println("Initial hierarchy linking complete. Country links: " + countryLinks + ", ADM1 links: "
                + adm1Links + ", Failed links: " + failedLinks);
        return worldRoot;
    }

    // --- Pass 3: Assign initial populations/bounds to ADM1 and find nodes > 1M ---
    // Assigns population aggregated from PPLs in Pass 1
    BoundingBox assignInitialPopBoundsAndIdentifyExpansions(TreeNode node, List<TreeNode> nodesToExpand) {
        BoundingBox nodeBounds = new BoundingBox();
        long currentAggregatedPop = 0; // Use a local var for summing children

        if (node.type == TreeNode.NodeType.ADM1) {
            // Assign population directly aggregated from PPLs for this ADM1
            node.aggregatedPopulation = adm1PopulationMap.getOrDefault(node.geonameId, 0L);
            node.bounds = adm1BoundsMap.get(node.geonameId);
            // Check threshold AFTER assigning initial aggregated pop
            if (node.aggregatedPopulation > POPULATION_THRESHOLD_1M) {
                nodesToExpand.add(node);
            }
             // Return the bounds found for this ADM1
            return node.bounds != null && node.bounds.isValid() ? node.bounds : new BoundingBox(); // Return empty if invalid
        }

        // For higher levels (Country, Continent, World), aggregate from children
        if (node.children != null && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                // Recursively call to process children first
                BoundingBox childBounds = assignInitialPopBoundsAndIdentifyExpansions(child, nodesToExpand);
                // Sum up the population returned by the children (which is the initial PPL aggregated pop for ADM1s)
                currentAggregatedPop += child.aggregatedPopulation;
                nodeBounds.extend(childBounds); // Aggregate bounds
            }
        }

        // Assign the summed population to the current node (Country, Continent, World)
        // This is the initial sum BEFORE scaling
        node.aggregatedPopulation = currentAggregatedPop;
        node.bounds = nodeBounds; // Assign aggregated bounds
        return nodeBounds;
    }


    // --- Pass 4: Add ADM2 Layer for specific ADM1 nodes ---
    // Assigns population aggregated from PPLs in Pass 1 to ADM2 nodes
    void addAdm2Layer(List<TreeNode> nodesToExpand) {
        System.out.println("Starting Pass 4: Adding ADM2 layer for " + nodesToExpand.size() + " ADM1 nodes...");
        int adm2Added = 0;
        if (relevantAdminMap == null || nodeMap == null || adm2PopulationMap == null || adm2BoundsMap == null) {
            System.err.println("Error: Maps not initialized for ADM2 layer addition.");
            return;
        }

        for (TreeNode adm1Node : nodesToExpand) {
            RelevantAdminInfo adm1NodeInfo = relevantAdminMap.get(adm1Node.geonameId);
            if (adm1NodeInfo == null || adm1NodeInfo.countryCode == null) {
                System.err.println(
                        "Warning: Could not find info for ADM1 node: " + adm1Node.name + " (ID: " + adm1Node.geonameId + "). Skipping ADM2 expansion.");
                continue;
            }
            String parentCountryCode = adm1NodeInfo.countryCode; // ISO2 code
            String parentAdm1Code = adm1Node.code; // ADM1 specific code part

            for (RelevantAdminInfo adm2Info : relevantAdminMap.values()) {
                // Match ADM2 entries based on country ISO code and ADM1 code part
                if (adm2Info.type == TreeNode.NodeType.ADM2 && Objects.equals(adm2Info.countryCode, parentCountryCode)
                        && Objects.equals(adm2Info.admin1Code, parentAdm1Code)) {

                    // Check if this ADM2 node already exists (shouldn't, normally)
                    if (!nodeMap.containsKey(adm2Info.geonameId)) {
                        TreeNode adm2Node = new TreeNode(adm2Info.geonameId, adm2Info.name, adm2Info.type,
                                adm2Info.code, adm2Info.featureCode);
                        // Assign population directly aggregated from PPLs for this ADM2
                        adm2Node.aggregatedPopulation = adm2PopulationMap.getOrDefault(adm2Info.geonameId, 0L);
                        adm2Node.bounds = adm2BoundsMap.get(adm2Info.geonameId);

                        nodeMap.put(adm2Info.geonameId, adm2Node); // Add to global map
                        adm1Node.addChild(adm2Node); // Add as child to ADM1
                        adm2Added++;
                    } else {
                         System.err.println("Warning: ADM2 Node already exists in map: " + adm2Info.name + " (ID: " + adm2Info.geonameId + ")");
                    }
                }
            }
        }
        System.out.println("Pass 4 Complete. Added " + adm2Added + " ADM2 nodes.");
    }


    // --- Pass 5: Distribute Remaining ADM1 Population ONLY to Zero-Pop ADM2 Children ---
    // Uses the initial PPL-aggregated populations
    void distributeAdm1Population(List<TreeNode> expandedAdm1Nodes) {
        System.out.println("Starting Pass 5: Distributing remaining initial ADM1 population to zero-pop ADM2 children...");
        int adm2PopDistributed = 0;
        long totalPopDistributed = 0;

        for (TreeNode adm1Node : expandedAdm1Nodes) {
            // Use the initial aggregated population of the ADM1 node BEFORE any scaling
            long totalAdm1PopFromPPLs = adm1PopulationMap.getOrDefault(adm1Node.geonameId, 0L); // Get original sum from PPLs for this ADM1

            if (totalAdm1PopFromPPLs <= 0) {
                continue; // No population to distribute
            }

            List<TreeNode> zeroPopAdm2Children = new ArrayList<>();
            long sumCurrentAdm2Pop = 0; // Sum of populations already assigned to ADM2 children in Pass 4

            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    // Use the population assigned in Pass 4 (from adm2PopulationMap)
                    if (child.aggregatedPopulation > 0) {
                        sumCurrentAdm2Pop += child.aggregatedPopulation;
                    } else {
                        zeroPopAdm2Children.add(child);
                    }
                }
            }

            int numZeroPopAdm2 = zeroPopAdm2Children.size();
            if (numZeroPopAdm2 > 0) {
                // Calculate population not accounted for by ADM2 children that *did* get population from PPLs
                long remainingAdm1Pop = totalAdm1PopFromPPLs - sumCurrentAdm2Pop;

                if (remainingAdm1Pop < 0) {
                    // This might happen if ADM2 pops sum to more than ADM1 pop due to data inconsistencies
                    System.err.println("Warning: ADM2 populations sum (" + sumCurrentAdm2Pop +
                                       ") exceeds ADM1 PPL-aggregated population (" + totalAdm1PopFromPPLs +
                                       ") for " + adm1Node.name + " (ID: " + adm1Node.geonameId + "). Cannot distribute negative remainder.");
                    remainingAdm1Pop = 0; // Prevent distribution
                }

                if (remainingAdm1Pop > 0) {
                    long share = remainingAdm1Pop / numZeroPopAdm2;
                    long remainder = remainingAdm1Pop % numZeroPopAdm2;

                    for (int i = 0; i < numZeroPopAdm2; i++) {
                        TreeNode adm2Child = zeroPopAdm2Children.get(i);
                        long assignedPop = share + (i < remainder ? 1 : 0);
                        // Assign the distributed population to the ADM2 node
                        adm2Child.aggregatedPopulation = assignedPop;
                        adm2PopDistributed++;
                        totalPopDistributed += assignedPop;
                    }
                }
            }
        }
        System.out.println("Pass 5 complete. Assigned estimated population (" + totalPopDistributed + ") to " + adm2PopDistributed
                + " previously zero-pop ADM2 nodes.");
    }


    // --- Pass 6: Estimate Missing ADM2 Bounding Boxes ---
    void estimateMissingAdm2Bounds(List<TreeNode> expandedAdm1Nodes) {
        System.out.println("Starting Pass 6: Estimating missing bounds for ADM2 children...");
        int boundsEstimated = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            // Ensure parent ADM1 has valid bounds to base estimation on
            if (adm1Node.bounds == null || !adm1Node.bounds.isValid()) {
                 System.err.println("Warning: Cannot estimate ADM2 bounds for children of " + adm1Node.name + " (ID: " + adm1Node.geonameId + ") because parent bounds are invalid.");
                continue;
            }

            List<TreeNode> adm2ChildrenMissingBounds = new ArrayList<>();
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    if (child.bounds == null || !child.bounds.isValid()) {
                        adm2ChildrenMissingBounds.add(child);
                    }
                }
            }

            int totalAdm2ToEstimate = adm2ChildrenMissingBounds.size();
            if (totalAdm2ToEstimate == 0) {
                continue; // All children have bounds
            }

            // Simple grid estimation based on the number of children needing bounds
            int gridCols = (int) Math.ceil(Math.sqrt(totalAdm2ToEstimate));
            int gridRows = (int) Math.ceil((double) totalAdm2ToEstimate / gridCols);

            double totalLatSpan = adm1Node.bounds.maxLat - adm1Node.bounds.minLat;
            double totalLonSpan = adm1Node.bounds.maxLon - adm1Node.bounds.minLon;
            double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
            double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;

            // Sort children needing bounds for consistent assignment (optional)
            adm2ChildrenMissingBounds.sort(Comparator.comparing(a -> a.name));

            boolean estimatedAny = false;
            for (int i = 0; i < totalAdm2ToEstimate; i++) {
                TreeNode adm2ChildNode = adm2ChildrenMissingBounds.get(i);

                if (!estimatedAny) {
                     System.out.println("  Estimating bounds for ADM2 children under " + adm1Node.name);
                    estimatedAny = true;
                }

                int r = i / gridCols;
                int c = i % gridCols;

                BoundingBox estimatedBounds = new BoundingBox();
                // Calculate cell bounds only if spans are meaningful
                if (cellWidth > 0 || cellHeight > 0) {
                    estimatedBounds.minLat = adm1Node.bounds.minLat + r * cellHeight;
                    estimatedBounds.minLon = adm1Node.bounds.minLon + c * cellWidth;
                    estimatedBounds.maxLat = adm1Node.bounds.minLat + (r + 1) * cellHeight;
                    estimatedBounds.maxLon = adm1Node.bounds.minLon + (c + 1) * cellWidth;

                    // Clamp estimated bounds to parent bounds
                    estimatedBounds.minLat = Math.max(estimatedBounds.minLat, adm1Node.bounds.minLat);
                    estimatedBounds.minLon = Math.max(estimatedBounds.minLon, adm1Node.bounds.minLon);
                    estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, adm1Node.bounds.maxLat);
                    estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, adm1Node.bounds.maxLon);

                    // Ensure min <= max after clamping
                    if (estimatedBounds.maxLat < estimatedBounds.minLat) estimatedBounds.maxLat = estimatedBounds.minLat;
                    if (estimatedBounds.maxLon < estimatedBounds.minLon) estimatedBounds.maxLon = estimatedBounds.minLon;

                } else {
                    // If parent span is zero, just copy parent bounds
                    estimatedBounds.extend(adm1Node.bounds);
                }

                // Assign the estimated bounds if they are valid, otherwise fallback to parent bounds
                adm2ChildNode.bounds = estimatedBounds.isValid() ? estimatedBounds : adm1Node.bounds;
                boundsEstimated++;
            }
        }
        System.out.println(
                "Pass 6 complete. Estimated bounds for " + boundsEstimated + " ADM2 nodes that were missing them.");
    }

    // --- Helper Function: Sum population recursively (used before scaling) ---
    private long sumCurrentPopulation(TreeNode node) {
        if (node == null) return 0;
        // Base case: If it's a leaf node at this stage (ADM1 without ADM2 children, or ADM2)
        // return its current aggregatedPopulation (which came from PPL aggregation/distribution)
        if (node.children.isEmpty() || (node.type == TreeNode.NodeType.ADM1 && node.children.stream().noneMatch(c -> c.type == TreeNode.NodeType.ADM2))) {
             // If ADM1 has no ADM2 children, use its PPL-aggregated value.
             // If ADM2, use its PPL-aggregated/distributed value.
            return node.aggregatedPopulation;
        }

        // Recursive case: Sum population from children relevant to the aggregation path
        long sum = 0;
        for (TreeNode child : node.children) {
             // Only sum ADM1s under Countries, ADM2s under ADM1s for this pre-scaling sum
             if ((node.type == TreeNode.NodeType.COUNTRY && child.type == TreeNode.NodeType.ADM1) ||
                 (node.type == TreeNode.NodeType.ADM1 && child.type == TreeNode.NodeType.ADM2)) {
                 sum += sumCurrentPopulation(child);
             } else if (node.type == TreeNode.NodeType.CONTINENT && child.type == TreeNode.NodeType.COUNTRY) {
                 sum += sumCurrentPopulation(child); // Sum countries under continents
             } else if (node.type == TreeNode.NodeType.WORLD && child.type == TreeNode.NodeType.CONTINENT) {
                 sum += sumCurrentPopulation(child); // Sum continents under world
             }
             // Ignore artificial children (S_ADM3, S_ADM4) for this pre-scaling sum
        }
        // Important: Do NOT update node.aggregatedPopulation here, just return the sum
        return sum;
    }


    // --- Helper Function: Apply Scaling and Calculate Internet Population Recursively ---
    private void applyScalingAndInternetPop(TreeNode node, double scaleFactor, double countryPenetrationRate) {
        if (node == null) return;

        // Scale the aggregated population
        // Ensure population doesn't become negative due to floating point issues with tiny scale factors
        node.aggregatedPopulation = Math.max(0, Math.round(node.aggregatedPopulation * scaleFactor));

        // Calculate internet population based on the SCALED aggregated population
        // Ensure internet population doesn't exceed total population
        node.internetPopulation = Math.min(node.aggregatedPopulation,
                                           Math.max(0, Math.round(node.aggregatedPopulation * countryPenetrationRate)));

        // Recursively apply to all children (including artificial ones if they exist later)
        for (TreeNode child : node.children) {
            applyScalingAndInternetPop(child, scaleFactor, countryPenetrationRate);
        }
    }

    // --- Pass 7: Population Scaling and Internet Population Calculation ---
    void scaleAndCalculateInternetPop(TreeNode root) {
        System.out.println("Starting Pass 7: Scaling populations to match official PCLI figures and calculating internet population...");
        int countriesScaled = 0;

        if (root == null || root.children.isEmpty()) {
            System.err.println("Error: Root node is null or has no children. Cannot perform scaling.");
            return;
        }

        // Iterate through Continents -> Countries
        for (TreeNode continent : root.children) {
            if (continent.type != TreeNode.NodeType.CONTINENT) continue;
            for (TreeNode country : continent.children) {
                if (country.type != TreeNode.NodeType.COUNTRY) continue;

                long officialPop = country.officialPopulation;
                double penetrationRate = country.internetPenetrationRate;

                if (officialPop <= 0) {
                    System.err.println("Warning: Skipping scaling for country " + country.name + " (ID: " + country.geonameId + ") due to zero or missing official population.");
                    // Apply penetration rate even if scaling isn't done? Or set internet pop to 0?
                    // Let's calculate internet pop based on unscaled aggregated pop if official is zero.
                    double scaleFactor = 1.0; // No scaling
                    applyScalingAndInternetPop(country, scaleFactor, penetrationRate); // Still calculate internet pop
                    continue;
                }

                // Calculate the sum of populations currently under this country node
                // This sum is based on the PPL aggregations and distributions from previous passes
                long currentAggregatedTotal = sumCurrentPopulation(country);

                if (currentAggregatedTotal <= 0) {
                    System.err.println("Warning: Skipping scaling for country " + country.name + " (ID: " + country.geonameId + ") because current aggregated population is zero. Cannot determine scale factor.");
                     // Apply penetration rate to zero pop? Set internet pop to 0.
                    double scaleFactor = 1.0; // No scaling
                    applyScalingAndInternetPop(country, scaleFactor, penetrationRate); // Will result in 0 internet pop
                    continue;
                }

                // Calculate the scaling factor
                double scaleFactor = (double) officialPop / currentAggregatedTotal;

                // Apply scaling and calculate internet population recursively downwards
                applyScalingAndInternetPop(country, scaleFactor, penetrationRate);
                countriesScaled++;

                 // Optional: Log scaling factor
                 // System.out.printf("  Scaled %s (ID: %d): Official=%d, Aggregated=%d, Factor=%.4f%n",
                 //                  country.name, country.geonameId, officialPop, currentAggregatedTotal, scaleFactor);

            }
        }
         System.out.println("Pass 7 Complete. Applied population scaling and calculated internet population for " + countriesScaled + " countries.");
    }


    // --- Expansion Logic (Reusable for different thresholds) ---
    // Now operates on SCALED populations
    boolean expandLeafNodeIfNeeded(TreeNode leafNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) {
            return false; // Not a leaf, or below threshold
        }
        // Check if bounds are valid for estimation
        if (leafNode.bounds == null || !leafNode.bounds.isValid()) {
             System.err.println("Warning: Cannot expand leaf node " + leafNode.name + " (ID: " + leafNode.geonameId + ") because its bounds are invalid.");
            return false;
        }

        long parentPop = leafNode.aggregatedPopulation; // Use the current (potentially scaled) population
        long parentInternetPop = leafNode.internetPopulation; // Use the current internet population

        int numChildren;
        if (distributeEqually) {
             // Distribute population as equally as possible
            numChildren = (int) Math.max(2, Math.ceil((double) parentPop / threshold)); // Ensure at least 2 children if expanding
        } else {
             // Create children with population up to the threshold
             numChildren = (int) Math.max(2, (parentPop + threshold - 1) / threshold); // Ceiling division, ensure at least 2
        }

        if (numChildren <= 1) return false; // Should not happen with Math.max(2, ...)

        // Grid estimation for bounds
        int gridCols = (int) Math.ceil(Math.sqrt(numChildren));
        int gridRows = (int) Math.ceil((double) numChildren / gridCols);
        double totalLatSpan = leafNode.bounds.maxLat - leafNode.bounds.minLat;
        double totalLonSpan = leafNode.bounds.maxLon - leafNode.bounds.minLon;
        double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
        double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;

        long remainingPop = parentPop;
        long remainingInternetPop = parentInternetPop;
        long popSumCheck = 0;
        long internetPopSumCheck = 0;

        for (int i = 0; i < numChildren; i++) {
            String childName = leafNode.name + " part " + (i + 1);
            // Generate a unique negative ID for the artificial node
            int tempChildId = -(Objects.hash(leafNode.geonameId, childName)); // Use Objects.hash for better distribution

            TreeNode childNode = new TreeNode(tempChildId, childName, artificialChildType, leafNode.code, "ARTIFICIAL");

            // Distribute total population
            long childPop;
            if (distributeEqually) {
                childPop = parentPop / numChildren + (i < parentPop % numChildren ? 1 : 0);
            } else {
                // Assign up to threshold, handle last child
                childPop = (i == numChildren - 1) ? remainingPop : Math.min(remainingPop, threshold);
                remainingPop -= childPop;
            }
            childNode.aggregatedPopulation = childPop;
            popSumCheck += childPop;

            // Distribute internet population proportionally to the distributed total population
            long childInternetPop;
             if (parentPop > 0) { // Avoid division by zero
                 // Calculate proportion based on total pop distribution
                 childInternetPop = Math.round(((double) childPop / parentPop) * parentInternetPop);
             } else {
                 childInternetPop = 0; // If parent pop is 0, internet pop must be 0
             }
             // Ensure the sum doesn't exceed the original due to rounding - adjust last child
             if (i == numChildren - 1) {
                 childInternetPop = parentInternetPop - internetPopSumCheck;
             } else {
                 internetPopSumCheck += childInternetPop;
             }
             // Ensure internet pop is not negative and not more than total pop
             childNode.internetPopulation = Math.min(childPop, Math.max(0, childInternetPop));


            // Estimate bounds
            int r = i / gridCols;
            int c = i % gridCols;
            BoundingBox estimatedBounds = new BoundingBox();
            if (cellWidth > 0 || cellHeight > 0) {
                estimatedBounds.minLat = leafNode.bounds.minLat + r * cellHeight;
                estimatedBounds.minLon = leafNode.bounds.minLon + c * cellWidth;
                estimatedBounds.maxLat = leafNode.bounds.minLat + (r + 1) * cellHeight;
                estimatedBounds.maxLon = leafNode.bounds.minLon + (c + 1) * cellWidth;
                // Clamp and ensure min <= max
                estimatedBounds.minLat = Math.max(estimatedBounds.minLat, leafNode.bounds.minLat);
                estimatedBounds.minLon = Math.max(estimatedBounds.minLon, leafNode.bounds.minLon);
                estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, leafNode.bounds.maxLat);
                estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, leafNode.bounds.maxLon);
                if (estimatedBounds.maxLat < estimatedBounds.minLat) estimatedBounds.maxLat = estimatedBounds.minLat;
                if (estimatedBounds.maxLon < estimatedBounds.minLon) estimatedBounds.maxLon = estimatedBounds.minLon;
            } else {
                estimatedBounds.extend(leafNode.bounds); // Copy parent bounds if span is zero
            }
            childNode.bounds = estimatedBounds.isValid() ? estimatedBounds : leafNode.bounds; // Assign or fallback

            leafNode.addChild(childNode);
        }

        // Sanity checks for population distribution
        if (popSumCheck != parentPop) {
            System.err.printf("WARN: Total Pop distribution error for %s (ID: %d). Orig: %d, Dist: %d%n",
                              leafNode.name, leafNode.geonameId, parentPop, popSumCheck);
        }
         long finalInternetPopSum = 0;
         for(TreeNode child : leafNode.children) finalInternetPopSum += child.internetPopulation;
         if (finalInternetPopSum != parentInternetPop) {
             System.err.printf("WARN: Internet Pop distribution error for %s (ID: %d). Orig: %d, Dist: %d%n",
                               leafNode.name, leafNode.geonameId, parentInternetPop, finalInternetPopSum);
             // Attempt simple correction on the last child if sums don't match due to rounding issues
             if (!leafNode.children.isEmpty()) {
                 TreeNode lastChild = leafNode.children.get(leafNode.children.size() - 1);
                 long diff = parentInternetPop - finalInternetPopSum;
                 lastChild.internetPopulation = Math.min(lastChild.aggregatedPopulation, Math.max(0, lastChild.internetPopulation + diff));
             }
         }


        return true; // Expansion occurred
    }


    // Traverses the tree and calls expandLeafNodeIfNeeded
    // Now operates on SCALED populations
    void findAndExpandLeaves(TreeNode startNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType, String passIdentifier) {
        System.out.println("Starting " + passIdentifier + ": Expanding Leaves with Population > " + threshold + "...");
        List<TreeNode> leavesToExpand = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (startNode != null) {
            queue.add(startNode);
        }

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            // A node is a leaf if it has no children. Check its population against threshold.
            if (current.children.isEmpty() && current.aggregatedPopulation > threshold) {
                leavesToExpand.add(current);
            } else {
                // If not a leaf or not over threshold, add its children to the queue
                queue.addAll(current.children);
            }
        }

        System.out.println("  Found " + leavesToExpand.size() + " leaves exceeding threshold " + threshold + ".");
        int expandedCount = 0;
        for (TreeNode leaf : leavesToExpand) {
            if (expandLeafNodeIfNeeded(leaf, threshold, distributeEqually, artificialChildType)) {
                expandedCount++;
            }
        }
        System.out.println(passIdentifier + " Complete. Expanded " + expandedCount + " leaves.");
    }


    // --- Final Aggregation Pass ---
    // Recalculates populations from the bottom up AFTER all scaling and expansions

    // Aggregates total population
    long finalAggregateTotalPopulation(TreeNode node) {
        if (node == null) return 0;
        // If it's a leaf node in the final tree, return its current population
        if (node.children.isEmpty()) {
            return node.aggregatedPopulation;
        }

        // Recursive case: Sum population from children
        long totalAggregatedPopulation = 0;
        for (TreeNode child : node.children) {
            totalAggregatedPopulation += finalAggregateTotalPopulation(child);
        }
        // Update the node's population with the sum of its children
        node.aggregatedPopulation = totalAggregatedPopulation;
        return totalAggregatedPopulation;
    }

    // Aggregates internet population
    long finalAggregateInternetPopulation(TreeNode node) {
        if (node == null) return 0;
        // If it's a leaf node in the final tree, return its current internet population
        if (node.children.isEmpty()) {
            return node.internetPopulation;
        }

        // Recursive case: Sum internet population from children
        long totalInternetPopulation = 0;
        for (TreeNode child : node.children) {
            totalInternetPopulation += finalAggregateInternetPopulation(child);
        }
        // Update the node's internet population with the sum of its children
        node.internetPopulation = totalInternetPopulation;
        // Sanity check: ensure internet pop doesn't exceed total pop after aggregation
        if (node.internetPopulation > node.aggregatedPopulation) {
             System.err.printf("WARN: Final aggregation resulted in internetPop (%d) > totalPop (%d) for node %s (ID: %d). Clamping internetPop.%n",
                               node.internetPopulation, node.aggregatedPopulation, node.name, node.geonameId);
            node.internetPopulation = node.aggregatedPopulation;
        }
        return node.internetPopulation;
    }


    // Aggregates bounding boxes
    BoundingBox finalAggregateBounds(TreeNode node) {
        if (node == null) return new BoundingBox(); // Return invalid box
        // If it's a leaf node, return its bounds (or an invalid box if it has none)
        if (node.children.isEmpty()) {
            return node.bounds != null ? node.bounds : new BoundingBox();
        }

        // Recursive case: Aggregate bounds from children
        BoundingBox calculatedBounds = new BoundingBox();
        for (TreeNode child : node.children) {
            BoundingBox childBounds = finalAggregateBounds(child);
            calculatedBounds.extend(childBounds); // Extend the parent's bounds
        }
        // Update the node's bounds
        node.bounds = calculatedBounds;
        return calculatedBounds;
    }


    // --- Main Execution ---
    public static void main(String[] args) {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            System.err.println("Error: Could not determine user home directory.");
            return;
        }

        // --- File Paths ---
        String geonamesFileName = "allCountries.txt";
        String admin1FileName = "admin1CodesASCII.txt";
        String admin2FileName = "admin2Codes.txt"; // Required for ADM2 level
        String countryInfoFileName = "countryInfo.txt";
        String internetPenetrationFileName = "internet_penetration_iso2.csv"; // New file
        String outputFileName = "geonames_hierarchy_output.txt";

        String geonamesFilePath = homeDir + File.separator + geonamesFileName;
        String admin1FilePath = homeDir + File.separator + admin1FileName;
        String admin2FilePath = homeDir + File.separator + admin2FileName;
        String countryInfoFilePath = homeDir + File.separator + countryInfoFileName;
        String internetPenetrationFilePath = homeDir + File.separator + internetPenetrationFileName; // New path
        String outputFilePath = homeDir + File.separator + outputFileName;

        // --- Check File Existence ---
        if (!new File(geonamesFilePath).exists()) { System.err.println("Error: File not found: " + geonamesFilePath); return; }
        if (!new File(admin1FilePath).exists()) { System.err.println("Error: File not found: " + admin1FilePath); return; }
        if (!new File(admin2FilePath).exists()) { System.err.println("Error: File not found: " + admin2FilePath); return; }
        if (!new File(countryInfoFilePath).exists()) { System.err.println("Error: File not found: " + countryInfoFilePath); return; }
        if (!new File(internetPenetrationFilePath).exists()) { System.err.println("Error: File not found: " + internetPenetrationFilePath); return; } // Check new file

        FileBasedDynamicBuilder builder = new FileBasedDynamicBuilder();
        long overallStartTime = System.currentTimeMillis();

        System.out.println("--- Loading Index & Data Files ---");
        builder.loadCountryInfo(countryInfoFilePath);
        builder.loadAdminCodes(admin1FilePath, builder.admin1CodeToIdMap, "ADM1");
        builder.loadAdminCodes(admin2FilePath, builder.admin2CodeToIdMap, "ADM2");
        builder.loadInternetPenetration(internetPenetrationFilePath); // Load penetration data

        if (builder.countryToContinentMap.isEmpty() || builder.admin1CodeToIdMap.isEmpty()
                || builder.admin2CodeToIdMap.isEmpty() || builder.countryIsoToPenetrationMap.isEmpty()) {
            System.err.println("Failed to load essential index/data files. Exiting.");
            return;
        }

        System.out.println("\n--- Pass 1: Processing " + geonamesFileName + " ---");
        long startTime = System.currentTimeMillis();
        builder.processAllCountriesPass1(geonamesFilePath);
        long pass1Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 1 finished in " + (pass1Time / 1000.0) + " seconds.");
        if (builder.relevantAdminMap.isEmpty()) {
            System.err.println("No relevant PCLI/ADM1/ADM2 features found. Exiting.");
            return;
        }

        System.out.println("\n--- Pass 2: Building Initial ADM1 Hierarchy ---");
        startTime = System.currentTimeMillis();
        TreeNode root = builder.buildInitialAdm1Hierarchy();
        long pass2Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 2 finished in " + pass2Time + " ms.");
        if (root == null || root.children.isEmpty()) {
            System.err.println("Initial hierarchy building failed. Exiting.");
            return;
        }

        System.out.println("\n--- Pass 3: Assigning Initial PPL-Aggregated Pop/Bounds & Identifying Nodes > 1M ---");
        startTime = System.currentTimeMillis();
        List<TreeNode> nodesToExpand1M = new ArrayList<>();
        builder.assignInitialPopBoundsAndIdentifyExpansions(root, nodesToExpand1M);
        long pass3Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 3 finished in " + pass3Time + " ms. Identified " + nodesToExpand1M.size()
                + " ADM1 nodes > 1M initial pop to potentially expand with ADM2s.");

        System.out.println("\n--- Pass 4: Adding ADM2 Layer ---");
        startTime = System.currentTimeMillis();
        builder.addAdm2Layer(nodesToExpand1M);
        long pass4Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 4 finished in " + pass4Time + " ms.");

        System.out.println("\n--- Pass 5: Distributing Initial ADM1 Population to ADM2s ---");
        startTime = System.currentTimeMillis();
        builder.distributeAdm1Population(nodesToExpand1M);
        long pass5Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 5 finished in " + pass5Time + " ms.");

        System.out.println("\n--- Pass 6: Estimating Missing ADM2 Bounding Boxes ---");
        startTime = System.currentTimeMillis();
        builder.estimateMissingAdm2Bounds(nodesToExpand1M); // Pass the same list used for pop distribution
        long pass6Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 6 finished in " + pass6Time + " ms.");

        // --- NEW SCALING PASS ---
        System.out.println("\n--- Pass 7: Population Scaling and Internet Population Calculation ---");
        startTime = System.currentTimeMillis();
        builder.scaleAndCalculateInternetPop(root);
        long pass7Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 7 finished in " + pass7Time + " ms.");

        // --- EXPANSION PASSES (now operate on scaled populations) ---
        System.out.println("\n--- Pass 8: Expanding Leaves with Population > 1,000,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_1M, false, TreeNode.NodeType.S_ADM3, "Pass 8");
        long pass8Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 8 finished in " + pass8Time + " ms.");

        System.out.println("\n--- Pass 9: Expanding Leaves with Population > 10,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_10K, true, TreeNode.NodeType.S_ADM4, "Pass 9");
        long pass9Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 9 finished in " + pass9Time + " ms.");

        // --- FINAL AGGREGATION PASS ---
        System.out.println("\n--- Pass 10: Final Recalculation of Aggregated Data ---");
        startTime = System.currentTimeMillis();
        // Recalculate populations first by summing children AFTER scaling and expansions
        builder.finalAggregateTotalPopulation(root);
        builder.finalAggregateInternetPopulation(root);
        // Then recalculate bounds
        builder.finalAggregateBounds(root);
        long pass10Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 10 finished in " + pass10Time + " ms.");

        // --- Output ---
        System.out.println("\n--- Writing Final Hierarchy to File ---");
        System.out.println("Output file: " + outputFilePath);
        if (root != null) {
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFilePath)))) {
                 root.printTree(writer, "");
                 System.out.println("Successfully wrote hierarchy to " + outputFilePath);
            } catch (IOException e) {
                 System.err.println("Error writing hierarchy to file: " + e.getMessage());
                 e.printStackTrace();
                 System.out.println("\n--- Printing Fallback to Console ---");
                 try (PrintWriter consoleWriter = new PrintWriter(System.out)) {
                      root.printTree(consoleWriter, "");
                      consoleWriter.flush();
                 } catch (Exception fallbackEx) {
                      System.err.println("Error printing hierarchy to console fallback: " + fallbackEx.getMessage());
                      fallbackEx.printStackTrace();
                 }
            }
        } else {
            System.err.println("Root node is null, cannot print tree.");
        }

        long overallEndTime = System.currentTimeMillis();
        System.out.println("\nTotal execution time: " + (overallEndTime - overallStartTime) / 1000.0 + " seconds.");
    }
}