package simulator.topology.geonames;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter; // Used for text output fallback
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// --- Jackson Imports ---
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

// This class is part of FileBasedDynamicBuilder.java

/**
 * Helper class to store and update bounding box coordinates.
 */
class BoundingBox {
    public double minLat = 91.0;  // Sentinel: higher than any valid latitude
    public double maxLat = -91.0; // Sentinel: lower than any valid latitude
    public double minLon = 181.0; // Sentinel: higher than any valid longitude
    public double maxLon = -181.0;// Sentinel: lower than any valid longitude

    public BoundingBox() {
    }

    /**
     * Extends the bounding box to include the given latitude and longitude.
     * @param lat The latitude of the point.
     * @param lon The longitude of the point.
     */
    void extend(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return; // Ignore NaN values
        }
        // Update minLat
        if (lat < minLat) {
            minLat = lat;
        }
        // Update maxLat
        if (lat > maxLat) {
            maxLat = lat;
        }
        // Update minLon
        if (lon < minLon) {
            minLon = lon;
        }
        // Update maxLon
        if (lon > maxLon) {
            maxLon = lon;
        }
    }

    /**
     * Extends this bounding box to include the 'other' bounding box.
     * @param other The other BoundingBox to include.
     */
    void extend(BoundingBox other) {
        if (other == null) {
            return;
        }
        // If the 'other' box itself is uninitialized (still has sentinel values
        // that make it invalid in the sense of min > max), we might choose to ignore it,
        // or extend by its individual valid coordinates if any.
        // However, the isValid() check below handles the case where 'other' might be
        // partially valid but overall !isValid().
        // A simpler approach is to extend by each coordinate if 'other' is not null.

        // If 'other' is valid or partially valid (even if its own isValid() is false
        // due to one bad coordinate like a stuck maxLon), we still try to incorporate its "better" parts.
        if (other.minLat < this.minLat) {
            this.minLat = other.minLat;
        }
        if (other.maxLat > this.maxLat) {
            this.maxLat = other.maxLat;
        }
        if (other.minLon < this.minLon) {
            this.minLon = other.minLon;
        }
        // *** CORRECTED LOGIC FOR MAXLON ***
        if (other.maxLon > this.maxLon) {
            this.maxLon = other.maxLon;
        }
    }

    /**
     * Checks if the bounding box has been initialized with valid, consistent coordinates.
     * Valid means min <= max for both latitude and longitude, and coordinates are within
     * standard geographic ranges.
     * @return true if the bounds are valid, false otherwise.
     */
    boolean isValid() {
        // Check if coordinates are within standard geographic ranges
        boolean latRangeOk = minLat >= -90.0 && minLat <= 90.0 &&
                             maxLat >= -90.0 && maxLat <= 90.0;
        boolean lonRangeOk = minLon >= -180.0 && minLon <= 180.0 &&
                             maxLon >= -180.0 && maxLon <= 180.0;

        // Check for consistency (min <= max)
        boolean latConsistent = minLat <= maxLat;
        boolean lonConsistent = minLon <= maxLon;

        return latRangeOk && lonRangeOk && latConsistent && lonConsistent;
    }

    @Override
    public String toString() {
        // Check against initial sentinel values to determine if it's truly uninitialized
        if (minLat == 91.0 && maxLat == -91.0 && minLon == 181.0 && maxLon == -181.0) {
            return "Uninitialized BBox";
        }
        if (!isValid()) { // Use the refined isValid()
            return String.format("Invalid BBox [(%.4f, %.4f) - (%.4f, %.4f)]", minLat, minLon, maxLat, maxLon);
        }
        return String.format("[(%.4f, %.4f) - (%.4f, %.4f)]", minLat, minLon, maxLat, maxLon);
    }
}

/**
 * Represents an entry parsed from a GeoNames data file (like allCountries.txt).
 */
class GeonameEntry {
    // ... (GeonameEntry class remains unchanged) ...
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
            this.countryCode = parts[COUNTRY_CODE_IDX].trim();
            this.admin1Code = parts[ADMIN1_CODE_IDX].trim();
            this.admin2Code = parts[ADMIN2_CODE_IDX].trim();
            try {
                this.latitude = Double.parseDouble(parts[LAT_IDX].trim());
            } catch (NumberFormatException | NullPointerException e) {
                /* Keep default NaN */ }
            try {
                this.longitude = Double.parseDouble(parts[LON_IDX].trim());
            } catch (NumberFormatException | NullPointerException e) {
                /* Keep default NaN */ }
            String populationStr = parts[POPULATION_IDX].trim();
            if (populationStr.isEmpty()) {
                this.population = 0;
            } else {
                try {
                    this.population = Long.parseLong(populationStr);
                } catch (NumberFormatException nfe) {
                    this.population = 0;
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
 */
class TreeNode {
    // ... (TreeNode class remains unchanged) ...
    public int geonameId;
    public String name;
    public String featureCode;
    public String code;
    public NodeType type;
    public long aggregatedPopulation; // Always present
    public long internetPopulation; // Always present (calculated)
    public Long officialPopulation; // Use wrapper type, init to null
    public Double internetPenetrationRate; // Use wrapper type, init to null
    public BoundingBox bounds;
    public List<TreeNode> children = new ArrayList<>();

    public TreeNode() {
    }

    public enum NodeType {
        WORLD, CONTINENT, COUNTRY, ADM1, ADM2, S_ADM3, S_ADM4, PPL
    }

    // Main constructor
    TreeNode(int geonameId, String name, NodeType type, String code, String featureCode) {
        this.geonameId = geonameId;
        this.name = name;
        this.type = type;
        this.code = (code != null ? code : "");
        this.featureCode = (featureCode != null ? featureCode : "");
        this.aggregatedPopulation = 0;
        this.internetPopulation = 0;
        this.officialPopulation = null;
        this.internetPenetrationRate = null;
        this.bounds = null;
    }

    // Constructor for Continent/World
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
        String officialPopStr = (officialPopulation != null) ? ", officialPop=" + officialPopulation : "";
        String rateStr = (internetPenetrationRate != null) ? String.format(", rate=%.3f", internetPenetrationRate) : "";
        return name + " (" + type + (code != null && !code.isEmpty() ? ", code=" + code : "") + ", id=" + geonameId
                + ", pop=" + aggregatedPopulation
                + ", internetPop=" + internetPopulation
                + officialPopStr + rateStr
                + boundsStr
                + (children.isEmpty() ? "" : ", children=" + children.size()) + ")";
    }

    public void printTree(PrintWriter writer, String indent) {
        writer.println(indent + this);
        children.sort(Comparator.comparing(a -> a.name));
        for (TreeNode child : children) {
            child.printTree(writer, indent + "  ");
        }
    }
}

/**
 * Lightweight holder for PCLI/ADM1/ADM2 info during Pass 1.
 */
class RelevantAdminInfo {
    // ... (RelevantAdminInfo class remains unchanged) ...
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
                this.officialPopulation = entry.population; // Store official pop from PCLI entry
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
 * Main class to build the GeoNames hierarchy. Includes logic to download
 * required GeoNames files if they are missing.
 */
public class FileBasedDynamicBuilder {

    // --- Maps for reference data ---
    Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    Map<String, String> countryToContinentMap = new HashMap<>();
    Map<String, Integer> countryCodeToIdMap = new HashMap<>();
    Map<String, Double> countryIsoToPenetrationMap = new HashMap<>();
    Map<Integer, Long> countryIdToOfficialPopulationMap = new HashMap<>();

    // --- Maps for aggregated data from Pass 1 ---
    Map<Integer, RelevantAdminInfo> relevantAdminMap = new HashMap<>();
    Map<Integer, Long> adm1PopulationMap = new HashMap<>();
    Map<Integer, BoundingBox> adm1BoundsMap = new HashMap<>();
    Map<Integer, Long> adm2PopulationMap = new HashMap<>();
    Map<Integer, BoundingBox> adm2BoundsMap = new HashMap<>();

    // --- Final Tree structure ---
    Map<Integer, TreeNode> nodeMap = new HashMap<>();

    // --- Constants ---
    final long POPULATION_THRESHOLD_1M = 1_000_000;
    final long POPULATION_THRESHOLD_10K = 10_000;
    final String GEONAMES_BASE_URL = "https://download.geonames.org/export/dump/";

    // --- Loading Methods ---
    void loadAdminCodes(String filePath, Map<String, Integer> map, String adminLevelName) {
        int count = 0;
        System.out.println("Loading " + adminLevelName + " codes from " + filePath + "...");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    String key = parts[0].trim();
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
            e.printStackTrace(); // Consider more robust error handling or re-throwing
        }
        System.out.println("Loaded " + count + " " + adminLevelName + " code mappings.");
    }

    void loadCountryInfo(String filePath) {
        final int ISO_CODE_IDX = 0;
        final int CONTINENT_CODE_IDX = 8;
        final int GEONAMEID_IDX = 16;
        final int MIN_COUNTRY_PARTS = 17;
        System.out.println("Loading country info from " + filePath + "...");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1);
                if (parts.length >= MIN_COUNTRY_PARTS) {
                    String isoCode = parts[ISO_CODE_IDX].trim();
                    String continentCode = parts[CONTINENT_CODE_IDX].trim();
                    String geonameIdStr = parts[GEONAMEID_IDX].trim();
                    if (!isoCode.isEmpty() && !continentCode.isEmpty()) {
                        countryToContinentMap.put(isoCode, continentCode);
                    }
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
            e.printStackTrace(); // Consider more robust error handling
        }
        System.out.println("Loaded " + countryToContinentMap.size() + " country->continent mappings.");
        System.out.println("Loaded " + countryCodeToIdMap.size() + " country ISO2->GeonameID mappings.");
    }

    void loadInternetPenetration(String filePath) {
        System.out.println("Loading Internet Penetration data from " + filePath + "...");
        countryIsoToPenetrationMap.clear();
        int validRatesLoaded = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Error: Internet penetration file is empty.");
                return;
            }
            String[] headers = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            List<Integer> yearIndices = new ArrayList<>();
            List<Integer> years = new ArrayList<>();
            int isoCodeIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].replace("\"", "").trim();
                if ("ISO2 Code".equalsIgnoreCase(header)) {
                    isoCodeIndex = i;
                } else if (header.matches("\\d{4} \\[YR\\d{4}\\]")) {
                    try {
                        int year = Integer.parseInt(header.substring(0, 4));
                        yearIndices.add(i);
                        years.add(year);
                    } catch (NumberFormatException e) {
                    }
                }
            }
            if (isoCodeIndex == -1) {
                System.err.println("Error: Could not find 'ISO2 Code' column in penetration file.");
                return;
            }
            if (yearIndices.isEmpty()) {
                System.err.println("Error: Could not find any valid year columns in penetration file.");
                return;
            }
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length > isoCodeIndex) {
                    String isoCode = parts[isoCodeIndex].replace("\"", "").trim();
                    if (isoCode.isEmpty())
                        continue;
                    double latestRate = -1.0;
                    for (int i = yearIndices.size() - 1; i >= 0; i--) {
                        int colIndex = yearIndices.get(i);
                        if (parts.length > colIndex) {
                            String rateStr = parts[colIndex].replace("\"", "").trim();
                            if (!rateStr.isEmpty() && !rateStr.equals("..")) {
                                try {
                                    latestRate = Double.parseDouble(rateStr) / 100.0;
                                    break;
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    }
                    if (latestRate >= 0.0) {
                        countryIsoToPenetrationMap.put(isoCode, latestRate);
                        validRatesLoaded++;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading internet penetration file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded latest internet penetration rates for " + validRatesLoaded + " countries.");
    }

    // --- Processing Passes ---
    void processAllCountriesPass1(String filePath) {
        System.out.println("Starting Pass 1: Processing " + filePath + "...");
        int lineCount = 0, pplCount = 0, pcliCount = 0, adm1Count = 0, adm2Count = 0;
        long totalPplPop = 0;
        long skippedPplPop = 0;
        int skippedPplCount = 0;
        countryIdToOfficialPopulationMap.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1);
                GeonameEntry entry = new GeonameEntry(parts);
                if (entry.geonameId == -1 || entry.countryCode == null || entry.countryCode.isEmpty()) {
                    continue;
                }
                TreeNode.NodeType type = determineNodeType(entry.featureClass, entry.featureCode);
                if (type == null)
                    continue;

                if (type == TreeNode.NodeType.COUNTRY) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info);
                    if (entry.population > 0) {
                        countryIdToOfficialPopulationMap.put(entry.geonameId, entry.population);
                    }
                    pcliCount++;
                } else if (type == TreeNode.NodeType.ADM1) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info);
                    adm1Count++;
                } else if (type == TreeNode.NodeType.ADM2) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info);
                    adm2Count++;
                } else if (type == TreeNode.NodeType.PPL) {
                    Integer parentAdm1Id = null;
                    Integer parentAdm2Id = null;
                    String adm1Key = null;
                    if (entry.admin1Code != null && !entry.admin1Code.isEmpty()) {
                        adm1Key = entry.countryCode + "." + entry.admin1Code;
                        parentAdm1Id = admin1CodeToIdMap.get(adm1Key);
                    }
                    if (parentAdm1Id != null && entry.admin2Code != null && !entry.admin2Code.isEmpty()) {
                        String adm2Key = entry.countryCode + "." + entry.admin1Code + "." + entry.admin2Code;
                        parentAdm2Id = admin2CodeToIdMap.get(adm2Key);
                    }
                    if (entry.population > 0) {
                        boolean popAdded = false;
                        if (parentAdm1Id != null) {
                            adm1PopulationMap.put(parentAdm1Id,
                                    adm1PopulationMap.getOrDefault(parentAdm1Id, 0L) + entry.population);
                            popAdded = true;
                        }
                        if (parentAdm2Id != null) {
                            adm2PopulationMap.put(parentAdm2Id,
                                    adm2PopulationMap.getOrDefault(parentAdm2Id, 0L) + entry.population);
                        }
                        if (popAdded) {
                            pplCount++;
                            totalPplPop += entry.population;
                        } else {
                            skippedPplCount++;
                            skippedPplPop += entry.population;
                        }
                    }
                    if (!Double.isNaN(entry.latitude) && !Double.isNaN(entry.longitude)) {
                        if (parentAdm1Id != null) {
                            BoundingBox bbox1 = adm1BoundsMap.computeIfAbsent(parentAdm1Id, k -> new BoundingBox());
                            bbox1.extend(entry.latitude, entry.longitude);
                        }
                        if (parentAdm2Id != null) {
                            BoundingBox bbox2 = adm2BoundsMap.computeIfAbsent(parentAdm2Id, k -> new BoundingBox());
                            bbox2.extend(entry.latitude, entry.longitude);
                        }
                    }
                }
                if (lineCount % 1000000 == 0) {
                    System.out.println("  Processed " + lineCount + " lines...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading geonames file in Pass 1: " + filePath);
            e.printStackTrace();
        }

        System.out
                .println("Pass 1 Complete. Found PCLI: " + pcliCount + ", ADM1: " + adm1Count + ", ADM2: " + adm2Count);
        System.out.println("Aggregated population from " + pplCount + " PPLs: " + totalPplPop + " into "
                + adm1PopulationMap.size() + " ADM1 & " + adm2PopulationMap.size() + " ADM2 regions.");
        System.out.println("Skipped " + skippedPplCount + " PPLs with total population " + skippedPplPop
                + " due to missing/unmatched ADM1 link.");
        System.out.println(
                "Stored official population for " + countryIdToOfficialPopulationMap.size() + " PCLI entries.");
    }

    private TreeNode.NodeType determineNodeType(String fcl, String fcode) {
        if ("PCLI".equals(fcode))
            return TreeNode.NodeType.COUNTRY;
        if ("ADM1".equals(fcode))
            return TreeNode.NodeType.ADM1;
        if ("ADM2".equals(fcode))
            return TreeNode.NodeType.ADM2;
        if ("P".equals(fcl) && fcode != null
                && (fcode.startsWith("PPL") || fcode.equals("PPLA") || fcode.equals("PPLC")))
            return TreeNode.NodeType.PPL;
        return null;
    }

    TreeNode buildInitialAdm1Hierarchy() {
        System.out.println("Starting Pass 2: Building initial hierarchy (World -> Continent -> Country -> ADM1)...");
        TreeNode worldRoot = new TreeNode("World", TreeNode.NodeType.WORLD, "WORLD");
        Map<String, TreeNode> continentNodes = new HashMap<>();
        nodeMap.clear();
        nodeMap.put(0, worldRoot);

        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            if (info.type == TreeNode.NodeType.COUNTRY || info.type == TreeNode.NodeType.ADM1) {
                TreeNode node = new TreeNode(info.geonameId, info.name, info.type, info.code, info.featureCode);
                if (node.type == TreeNode.NodeType.COUNTRY) {
                    Long officialPop = countryIdToOfficialPopulationMap.get(node.geonameId);
                    if (officialPop != null && officialPop > 0) {
                        node.officialPopulation = officialPop;
                    } else {
                        System.err.println("Warning: Country " + node.name + " has missing/zero official population.");
                    }
                    Double penetrationRate = countryIsoToPenetrationMap.get(node.code);
                    if (penetrationRate != null && penetrationRate > 0.0) {
                        node.internetPenetrationRate = penetrationRate;
                    } else {
                        System.err.println(
                                "Warning: Country " + node.name + " has missing/zero internet penetration rate.");
                    }
                }
                nodeMap.put(info.geonameId, node);
            }
        }
        System.out.println("  Created " + (nodeMap.size() - 1) + " initial TreeNodes (PCLI + ADM1).");

        int countryLinks = 0, adm1Links = 0, failedLinks = 0;
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            TreeNode childNode = nodeMap.get(info.geonameId);
            if (childNode == null)
                continue;
            TreeNode parentNode = null;
            if (childNode.type == TreeNode.NodeType.ADM1) {
                Integer parentCountryId = countryCodeToIdMap.get(info.countryCode);
                if (parentCountryId != null) {
                    parentNode = nodeMap.get(parentCountryId);
                }
                if (parentNode != null && parentNode.type == TreeNode.NodeType.COUNTRY) {
                    parentNode.addChild(childNode);
                    adm1Links++;
                } else {
                    failedLinks++;
                    System.err.println("Failed to link ADM1: " + childNode.name + " - Parent country (ISO: "
                            + info.countryCode + ") not found or invalid.");
                }
            } else if (childNode.type == TreeNode.NodeType.COUNTRY) {
                String continentCode = countryToContinentMap.get(childNode.code);
                if (continentCode != null) {
                    parentNode = continentNodes.computeIfAbsent(continentCode, k -> {
                        String continentName = switch (k) {
                            case "EU" -> "Europe";
                            case "AS" -> "Asia";
                            case "AF" -> "Africa";
                            case "NA" -> "North America";
                            case "SA" -> "South America";
                            case "OC" -> "Oceania";
                            case "AN" -> "Antarctica";
                            default -> k;
                        };
                        TreeNode newNode = new TreeNode(continentName, TreeNode.NodeType.CONTINENT, k);
                        worldRoot.addChild(newNode);
                        return newNode;
                    });
                } else {
                    parentNode = worldRoot;
                    System.err.println(
                            "Warning: Continent not found for country: " + childNode.name + ". Linking to World.");
                }
                parentNode.addChild(childNode);
                countryLinks++;
            }
        }
        System.out.println("Initial hierarchy linking complete. Country links: " + countryLinks + ", ADM1 links: "
                + adm1Links + ", Failed links: " + failedLinks);
        return worldRoot;
    }

    BoundingBox assignInitialPopBoundsAndIdentifyExpansions(TreeNode node, List<TreeNode> nodesToExpand) {
        BoundingBox nodeBounds = new BoundingBox();
        long currentAggregatedPop = 0;
        if (node.type == TreeNode.NodeType.ADM1) {
            node.aggregatedPopulation = adm1PopulationMap.getOrDefault(node.geonameId, 0L);
            node.bounds = adm1BoundsMap.get(node.geonameId);
            if (node.aggregatedPopulation > POPULATION_THRESHOLD_1M) {
                nodesToExpand.add(node);
            }
            return node.bounds != null && node.bounds.isValid() ? node.bounds : new BoundingBox();
        }
        if (node.children != null && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                BoundingBox childBounds = assignInitialPopBoundsAndIdentifyExpansions(child, nodesToExpand);
                currentAggregatedPop += child.aggregatedPopulation;
                nodeBounds.extend(childBounds);
            }
        }
        node.aggregatedPopulation = currentAggregatedPop;
        node.bounds = nodeBounds;
        return nodeBounds;
    }

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
                        "Warning: Could not find info for ADM1 node: " + adm1Node.name + ". Skipping ADM2 expansion.");
                continue;
            }
            String parentCountryCode = adm1NodeInfo.countryCode;
            String parentAdm1Code = adm1Node.code;
            for (RelevantAdminInfo adm2Info : relevantAdminMap.values()) {
                if (adm2Info.type == TreeNode.NodeType.ADM2 && Objects.equals(adm2Info.countryCode, parentCountryCode)
                        && Objects.equals(adm2Info.admin1Code, parentAdm1Code)) {
                    if (!nodeMap.containsKey(adm2Info.geonameId)) {
                        TreeNode adm2Node = new TreeNode(adm2Info.geonameId, adm2Info.name, adm2Info.type,
                                adm2Info.code, adm2Info.featureCode);
                        adm2Node.aggregatedPopulation = adm2PopulationMap.getOrDefault(adm2Info.geonameId, 0L);
                        adm2Node.bounds = adm2BoundsMap.get(adm2Info.geonameId);
                        nodeMap.put(adm2Info.geonameId, adm2Node);
                        adm1Node.addChild(adm2Node);
                        adm2Added++;
                    } else {
                        System.err.println("Warning: ADM2 Node already exists in map: " + adm2Info.name);
                    }
                }
            }
        }
        System.out.println("Pass 4 Complete. Added " + adm2Added + " ADM2 nodes.");
    }

    void distributeAdm1Population(List<TreeNode> expandedAdm1Nodes) {
        System.out.println(
                "Starting Pass 5: Distributing remaining initial ADM1 population to zero-pop ADM2 children...");
        int adm2PopDistributed = 0;
        long totalPopDistributed = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            long totalAdm1PopFromPPLs = adm1PopulationMap.getOrDefault(adm1Node.geonameId, 0L);
            if (totalAdm1PopFromPPLs <= 0) {
                continue;
            }
            List<TreeNode> zeroPopAdm2Children = new ArrayList<>();
            long sumCurrentAdm2Pop = 0;
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    if (child.aggregatedPopulation > 0) {
                        sumCurrentAdm2Pop += child.aggregatedPopulation;
                    } else {
                        zeroPopAdm2Children.add(child);
                    }
                }
            }
            int numZeroPopAdm2 = zeroPopAdm2Children.size();
            if (numZeroPopAdm2 > 0) {
                long remainingAdm1Pop = totalAdm1PopFromPPLs - sumCurrentAdm2Pop;
                if (remainingAdm1Pop < 0) {
                    System.err.println("Warning: ADM2 populations sum exceeds ADM1 PPL-aggregated population for "
                            + adm1Node.name + ". Cannot distribute negative remainder.");
                    remainingAdm1Pop = 0;
                }
                if (remainingAdm1Pop > 0) {
                    long share = remainingAdm1Pop / numZeroPopAdm2;
                    long remainder = remainingAdm1Pop % numZeroPopAdm2;
                    for (int i = 0; i < numZeroPopAdm2; i++) {
                        TreeNode adm2Child = zeroPopAdm2Children.get(i);
                        long assignedPop = share + (i < remainder ? 1 : 0);
                        adm2Child.aggregatedPopulation = assignedPop;
                        adm2PopDistributed++;
                        totalPopDistributed += assignedPop;
                    }
                }
            }
        }
        System.out.println("Pass 5 complete. Assigned estimated population (" + totalPopDistributed + ") to "
                + adm2PopDistributed + " previously zero-pop ADM2 nodes.");
    }

    void estimateMissingAdm2Bounds(List<TreeNode> expandedAdm1Nodes) {
        System.out.println("Starting Pass 6: Estimating missing bounds for ADM2 children...");
        int boundsEstimated = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            if (adm1Node.bounds == null || !adm1Node.bounds.isValid()) {
                System.err.println("Warning: Cannot estimate ADM2 bounds for children of " + adm1Node.name
                        + " - parent bounds invalid.");
                continue;
            }
            List<TreeNode> adm2ChildrenMissingBounds = new ArrayList<>();
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2 && (child.bounds == null || !child.bounds.isValid())) {
                    adm2ChildrenMissingBounds.add(child);
                }
            }
            int totalAdm2ToEstimate = adm2ChildrenMissingBounds.size();
            if (totalAdm2ToEstimate == 0) {
                continue;
            }
            int gridCols = (int) Math.ceil(Math.sqrt(totalAdm2ToEstimate));
            int gridRows = (int) Math.ceil((double) totalAdm2ToEstimate / gridCols);
            double totalLatSpan = adm1Node.bounds.maxLat - adm1Node.bounds.minLat;
            double totalLonSpan = adm1Node.bounds.maxLon - adm1Node.bounds.minLon;
            double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
            double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;
            adm2ChildrenMissingBounds.sort(Comparator.comparing(a -> a.name));
            boolean estimationLogged = false;
            for (int i = 0; i < totalAdm2ToEstimate; i++) {
                TreeNode adm2ChildNode = adm2ChildrenMissingBounds.get(i);
                if (!estimationLogged) {
                    System.out.println("  Estimating bounds for ADM2 children under " + adm1Node.name);
                    estimationLogged = true;
                }
                int r = i / gridCols;
                int c = i % gridCols;
                BoundingBox estimatedBounds = new BoundingBox();
                if (cellWidth > 0 || cellHeight > 0) {
                    estimatedBounds.minLat = adm1Node.bounds.minLat + r * cellHeight;
                    estimatedBounds.minLon = adm1Node.bounds.minLon + c * cellWidth;
                    estimatedBounds.maxLat = adm1Node.bounds.minLat + (r + 1) * cellHeight;
                    estimatedBounds.maxLon = adm1Node.bounds.minLon + (c + 1) * cellWidth;
                    estimatedBounds.minLat = Math.max(estimatedBounds.minLat, adm1Node.bounds.minLat);
                    estimatedBounds.minLon = Math.max(estimatedBounds.minLon, adm1Node.bounds.minLon);
                    estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, adm1Node.bounds.maxLat);
                    estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, adm1Node.bounds.maxLon);
                    if (estimatedBounds.maxLat < estimatedBounds.minLat)
                        estimatedBounds.maxLat = estimatedBounds.minLat;
                    if (estimatedBounds.maxLon < estimatedBounds.minLon)
                        estimatedBounds.maxLon = estimatedBounds.minLon;
                } else {
                    estimatedBounds.extend(adm1Node.bounds);
                }
                adm2ChildNode.bounds = estimatedBounds.isValid() ? estimatedBounds : adm1Node.bounds;
                boundsEstimated++;
            }
        }
        System.out.println("Pass 6 complete. Estimated bounds for " + boundsEstimated + " ADM2 nodes.");
    }

    private long sumCurrentPopulation(TreeNode node) {
        if (node == null)
            return 0;
        boolean hasAdm2Child = false;
        if (node.type == TreeNode.NodeType.ADM1) {
            for (TreeNode child : node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    hasAdm2Child = true;
                    break;
                }
            }
        }
        if (node.children.isEmpty() || (node.type == TreeNode.NodeType.ADM1 && !hasAdm2Child)) {
            return node.aggregatedPopulation;
        }
        long sum = 0;
        for (TreeNode child : node.children) {
            if ((node.type == TreeNode.NodeType.COUNTRY && child.type == TreeNode.NodeType.ADM1)
                    || (node.type == TreeNode.NodeType.ADM1 && child.type == TreeNode.NodeType.ADM2)
                    || (node.type == TreeNode.NodeType.CONTINENT && child.type == TreeNode.NodeType.COUNTRY)
                    || (node.type == TreeNode.NodeType.WORLD && child.type == TreeNode.NodeType.CONTINENT)) {
                sum += sumCurrentPopulation(child);
            }
        }
        return sum;
    }

    private void applyScalingAndInternetPop(TreeNode node, double scaleFactor, double countryPenetrationRate) {
        if (node == null)
            return;
        node.aggregatedPopulation = Math.max(0, Math.round(node.aggregatedPopulation * scaleFactor));
        node.internetPopulation = Math.min(node.aggregatedPopulation,
                Math.max(0, Math.round(node.aggregatedPopulation * countryPenetrationRate)));
        for (TreeNode child : node.children) {
            applyScalingAndInternetPop(child, scaleFactor, countryPenetrationRate);
        }
    }

    void scaleAndCalculateInternetPop(TreeNode root) {
        System.out.println("Starting Pass 7: Scaling populations and calculating internet population...");
        int countriesProcessed = 0;
        if (root == null || root.children.isEmpty()) {
            System.err.println("Error: Root node is null or has no children.");
            return;
        }
        for (TreeNode continent : root.children) {
            if (continent.type != TreeNode.NodeType.CONTINENT)
                continue;
            for (TreeNode country : continent.children) {
                if (country.type != TreeNode.NodeType.COUNTRY)
                    continue;
                countriesProcessed++;
                long officialPop = (country.officialPopulation != null) ? country.officialPopulation : 0L;
                double penetrationRate = (country.internetPenetrationRate != null) ? country.internetPenetrationRate
                        : 0.0;
                if (officialPop <= 0) {
                    System.err.println("Warning: Skipping scaling for country " + country.name
                            + " - zero/missing official population.");
                    applyScalingAndInternetPop(country, 1.0, penetrationRate);
                    continue;
                }
                long currentAggregatedTotal = sumCurrentPopulation(country);
                if (currentAggregatedTotal <= 0) {
                    System.err.println("Warning: Skipping scaling for country " + country.name
                            + " - zero current aggregated population.");
                    applyScalingAndInternetPop(country, 1.0, penetrationRate);
                    continue;
                }
                double scaleFactor = (double) officialPop / currentAggregatedTotal;
                applyScalingAndInternetPop(country, scaleFactor, penetrationRate);
            }
        }
        System.out.println("Pass 7 Complete. Processed population scaling and internet population calculation for "
                + countriesProcessed + " countries.");
    }

    boolean expandLeafNodeIfNeeded(TreeNode leafNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) {
            return false;
        }
        if (leafNode.bounds == null || !leafNode.bounds.isValid()) {
            System.err.println("Warning: Cannot expand leaf node " + leafNode.name + " - invalid bounds.");
            return false;
        }
        long parentPop = leafNode.aggregatedPopulation;
        long parentInternetPop = leafNode.internetPopulation;
        int numChildren;
        if (distributeEqually) {
            numChildren = (int) Math.max(2, Math.ceil((double) parentPop / threshold));
        } else {
            numChildren = (int) Math.max(2, (parentPop + threshold - 1) / threshold);
        }
        int gridCols = (int) Math.ceil(Math.sqrt(numChildren));
        int gridRows = (int) Math.ceil((double) numChildren / gridCols);
        double totalLatSpan = leafNode.bounds.maxLat - leafNode.bounds.minLat;
        double totalLonSpan = leafNode.bounds.maxLon - leafNode.bounds.minLon;
        double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
        double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;
        long remainingPop = parentPop;
        long popSumCheck = 0;
        long internetPopSumCheck = 0;
        for (int i = 0; i < numChildren; i++) {
            String childName = leafNode.name + " part " + (i + 1);
            int tempChildId = -(Objects.hash(leafNode.geonameId, childName));
            TreeNode childNode = new TreeNode(tempChildId, childName, artificialChildType, leafNode.code, "ARTIFICIAL");
            long childPop;
            if (distributeEqually) {
                childPop = parentPop / numChildren + (i < parentPop % numChildren ? 1 : 0);
            } else {
                childPop = (i == numChildren - 1) ? remainingPop : Math.min(remainingPop, threshold);
                remainingPop -= childPop;
            }
            childNode.aggregatedPopulation = childPop;
            popSumCheck += childPop;
            long childInternetPop;
            if (parentPop > 0) {
                childInternetPop = Math.round(((double) childPop / parentPop) * parentInternetPop);
            } else {
                childInternetPop = 0;
            }
            if (i == numChildren - 1) {
                childInternetPop = parentInternetPop - internetPopSumCheck;
            }
            childNode.internetPopulation = Math.min(childPop, Math.max(0, childInternetPop));
            internetPopSumCheck += childNode.internetPopulation;
            int r = i / gridCols;
            int c = i % gridCols;
            BoundingBox estimatedBounds = new BoundingBox();
            if (cellWidth > 0 || cellHeight > 0) {
                estimatedBounds.minLat = leafNode.bounds.minLat + r * cellHeight;
                estimatedBounds.minLon = leafNode.bounds.minLon + c * cellWidth;
                estimatedBounds.maxLat = leafNode.bounds.minLat + (r + 1) * cellHeight;
                estimatedBounds.maxLon = leafNode.bounds.minLon + (c + 1) * cellWidth;
                estimatedBounds.minLat = Math.max(estimatedBounds.minLat, leafNode.bounds.minLat);
                estimatedBounds.minLon = Math.max(estimatedBounds.minLon, leafNode.bounds.minLon);
                estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, leafNode.bounds.maxLat);
                estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, leafNode.bounds.maxLon);
                if (estimatedBounds.maxLat < estimatedBounds.minLat)
                    estimatedBounds.maxLat = estimatedBounds.minLat;
                if (estimatedBounds.maxLon < estimatedBounds.minLon)
                    estimatedBounds.maxLon = estimatedBounds.minLon;
            } else {
                estimatedBounds.extend(leafNode.bounds);
            }
            childNode.bounds = estimatedBounds.isValid() ? estimatedBounds : leafNode.bounds;
            leafNode.addChild(childNode);
        }
        if (popSumCheck != parentPop) {
            System.err.printf("WARN: Total Pop distribution error for %s. Orig: %d, Dist: %d%n", leafNode.name,
                    parentPop, popSumCheck);
        }
        if (internetPopSumCheck != parentInternetPop) {
            System.err.printf("WARN: Internet Pop distribution error for %s. Orig: %d, Dist: %d%n", leafNode.name,
                    parentInternetPop, internetPopSumCheck);
        }
        return true;
    }

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
            if (current.children.isEmpty() && current.aggregatedPopulation > threshold) {
                leavesToExpand.add(current);
            } else {
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

    long finalAggregateTotalPopulation(TreeNode node) {
        if (node == null)
            return 0;
        if (node.children.isEmpty()) {
            return node.aggregatedPopulation;
        }
        long totalAggregatedPopulation = 0;
        for (TreeNode child : node.children) {
            totalAggregatedPopulation += finalAggregateTotalPopulation(child);
        }
        node.aggregatedPopulation = totalAggregatedPopulation;
        return totalAggregatedPopulation;
    }

    long finalAggregateInternetPopulation(TreeNode node) {
        if (node == null)
            return 0;
        if (node.children.isEmpty()) {
            return node.internetPopulation;
        }
        long totalInternetPopulation = 0;
        for (TreeNode child : node.children) {
            totalInternetPopulation += finalAggregateInternetPopulation(child);
        }
        node.internetPopulation = totalInternetPopulation;
        if (node.internetPopulation > node.aggregatedPopulation) {
            System.err.printf("WARN: Final aggregation internetPop > totalPop for %s. Clamping.%n", node.name);
            node.internetPopulation = node.aggregatedPopulation;
        }
        return node.internetPopulation;
    }

    BoundingBox finalAggregateBounds(TreeNode node) {
        if (node == null)
            return new BoundingBox();
        if (node.children.isEmpty()) {
            return node.bounds != null ? node.bounds : new BoundingBox();
        }
        BoundingBox calculatedBounds = new BoundingBox();
        for (TreeNode child : node.children) {
            BoundingBox childBounds = finalAggregateBounds(child);
            calculatedBounds.extend(childBounds);
        }
        node.bounds = calculatedBounds;
        return calculatedBounds;
    }

    // --- File Download and Extraction Logic ---

    /**
     * Checks if required GeoNames data files exist in the resources directory.
     * If a file is missing, attempts to download it from the official GeoNames
     * source.
     * 
     * @param resourcesDirName The name of the resources directory.
     * @return true if all required files are present or successfully downloaded,
     *         false otherwise.
     */
    private boolean ensureDataFilesExist(String resourcesDirName) {
        System.out.println("\n--- Checking for required GeoNames data files ---");

        // Create the resources directory if it doesn't exist
        File resourcesDir = new File(resourcesDirName);
        if (!resourcesDir.exists()) {
            System.out.println("Creating directory: " + resourcesDir.getAbsolutePath());
            if (!resourcesDir.mkdirs()) {
                System.err.println("Error: Failed to create resources directory: " + resourcesDir.getAbsolutePath());
                return false; // Cannot proceed without resources directory
            }
        }

        // Define required files, their URLs, and if they are zipped
        // Structure: { Filename, URL, IsZipped, ZipEntryName (if zipped) }
        String[][] requiredFiles = {
                { "allCountries.txt", GEONAMES_BASE_URL + "allCountries.zip", "true", "allCountries.txt" },
                { "admin1CodesASCII.txt", GEONAMES_BASE_URL + "admin1CodesASCII.txt", "false", null },
                { "admin2Codes.txt", GEONAMES_BASE_URL + "admin2Codes.txt", "false", null },
                { "countryInfo.txt", GEONAMES_BASE_URL + "countryInfo.txt", "false", null }
                // Note: internet_penetration_iso2.csv is assumed to be provided manually
        };

        boolean allFilesOk = true;
        for (String[] fileInfo : requiredFiles) {
            String fileName = fileInfo[0];
            String fileUrl = fileInfo[1];
            boolean isZipped = Boolean.parseBoolean(fileInfo[2]);
            String zipEntryName = fileInfo[3];
            Path destinationPath = Paths.get(resourcesDirName, fileName);

            if (!Files.exists(destinationPath)) {
                System.out.println("File not found: " + destinationPath + ". Attempting download...");
                boolean success = downloadAndExtractFile(fileUrl, destinationPath, isZipped, zipEntryName);
                if (!success) {
                    System.err.println("Failed to download or extract: " + fileName);
                    allFilesOk = false; // Mark as failure but continue checking other files
                }
            } else {
                System.out.println("File found: " + destinationPath);
            }
        }

        // Final check for the manually provided CSV file
        Path penetrationPath = Paths.get(resourcesDirName, "internet_penetration_iso2.csv");
        if (!Files.exists(penetrationPath)) {
            System.err.println("Error: Required file internet_penetration_iso2.csv not found in " + resourcesDirName);
            System.err.println("Please add this file manually to the resources directory.");
            allFilesOk = false;
        } else {
            System.out.println("File found: " + penetrationPath);
        }

        if (!allFilesOk) {
            System.err.println("One or more required data files are missing or could not be downloaded.");
        }
        return allFilesOk;
    }

    /**
     * Downloads a file from a URL, optionally extracting a specific entry if it's a
     * zip file.
     * 
     * @param fileUrl         URL to download from.
     * @param destinationPath Path where the final file should be saved.
     * @param isZipped        True if the URL points to a zip file.
     * @param zipEntryName    The name of the file to extract from the zip (required
     *                        if isZipped is true).
     * @return true if the file was successfully downloaded/extracted, false
     *         otherwise.
     */
    private boolean downloadAndExtractFile(String fileUrl, Path destinationPath, boolean isZipped,
            String zipEntryName) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Path tempZipPath = null; // Path for temporary zip file download

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // Optional: Set timeouts
            // connection.setConnectTimeout(15000); // 15 seconds
            // connection.setReadTimeout(30000); // 30 seconds

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                long fileSize = connection.getContentLengthLong();
                System.out.printf("  Downloading %s (Size: %s)...%n",
                        destinationPath.getFileName(),
                        fileSize > 0 ? String.format("%,d bytes", fileSize) : "Unknown");

                if (isZipped) {
                    // Download zip to a temporary file first
                    tempZipPath = Files.createTempFile("geonames_", ".zip");
                    Files.copy(inputStream, tempZipPath, StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close(); // Close stream after copy

                    System.out.println("  Extracting " + zipEntryName + " from " + tempZipPath.getFileName() + "...");
                    // Extract the required entry from the temporary zip file
                    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZipPath))) {
                        ZipEntry entry;
                        boolean entryFound = false;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.getName().equals(zipEntryName)) {
                                // Found the target entry, extract it to the final destination
                                try (OutputStream fos = new FileOutputStream(destinationPath.toFile())) {
                                    byte[] buffer = new byte[8192]; // 8KB buffer
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                System.out.println("  Successfully extracted to " + destinationPath);
                                entryFound = true;
                                break; // Stop after finding the entry
                            }
                            zis.closeEntry(); // Close current entry
                        }
                        if (!entryFound) {
                            System.err.println(
                                    "Error: Entry '" + zipEntryName + "' not found in downloaded zip file: " + fileUrl);
                            return false;
                        }
                    }
                } else {
                    // Download non-zip file directly to destination
                    Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("  Successfully downloaded to " + destinationPath);
                }
                return true; // Success

            } else {
                System.err.println("Error: Failed to download file. Server responded with code: " + responseCode
                        + " for URL: " + fileUrl);
                return false;
            }

        } catch (IOException e) {
            System.err.println("Error during download/extraction for " + fileUrl + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Clean up resources
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    /* Ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
            // Delete temporary zip file if created
            if (tempZipPath != null) {
                try {
                    Files.deleteIfExists(tempZipPath);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to delete temporary file: " + tempZipPath);
                }
            }
        }
    }

    // --- Main Execution Logic ---
    public static void main(String[] args) {
        // Define input/output directories relative to project root
        String resourcesDirName = "resources/world";
        String outputDirName = "output";

        // Define filenames
        String geonamesFileName = "allCountries.txt";
        String admin1FileName = "admin1CodesASCII.txt";
        String admin2FileName = "admin2Codes.txt";
        String countryInfoFileName = "countryInfo.txt";
        String internetPenetrationFileName = "internet_penetration_iso2.csv";
        String outputTextFileName = "geonames_hierarchy_output.txt";
        String outputJsonFileName = "geonames_topology.json";

        // Construct full paths relative to the execution directory
        String geonamesFilePath = resourcesDirName + File.separator + geonamesFileName;
        String admin1FilePath = resourcesDirName + File.separator + admin1FileName;
        String admin2FilePath = resourcesDirName + File.separator + admin2FileName;
        String countryInfoFilePath = resourcesDirName + File.separator + countryInfoFileName;
        String internetPenetrationFilePath = resourcesDirName + File.separator + internetPenetrationFileName;
        String outputTextFilePath = outputDirName + File.separator + outputTextFileName;
        String outputJsonFilePath = outputDirName + File.separator + outputJsonFileName;

        // --- Start Build Process ---
        FileBasedDynamicBuilder builder = new FileBasedDynamicBuilder();
        long overallStartTime = System.currentTimeMillis();

        // --- Ensure Data Files Exist (Download if necessary) ---
        if (!builder.ensureDataFilesExist(resourcesDirName)) {
            System.err.println("Cannot proceed without required data files. Exiting.");
            return; // Stop execution if files are missing and couldn't be downloaded
        }

        // --- Load Reference Data ---
        System.out.println("\n--- Loading Index & Data Files ---");
        builder.loadCountryInfo(countryInfoFilePath);
        builder.loadAdminCodes(admin1FilePath, builder.admin1CodeToIdMap, "ADM1");
        builder.loadAdminCodes(admin2FilePath, builder.admin2CodeToIdMap, "ADM2");
        builder.loadInternetPenetration(internetPenetrationFilePath); // Load the manually provided file
        // Basic check if maps loaded correctly
        if (builder.countryToContinentMap.isEmpty() || builder.admin1CodeToIdMap.isEmpty()
                || builder.admin2CodeToIdMap.isEmpty() || builder.countryIsoToPenetrationMap.isEmpty()) {
            System.err.println("Failed to load essential index/data files after checking/downloading. Exiting.");
            return;
        }

        // --- Execute Processing Passes (1-10) ---
        System.out.println("\n--- Pass 1: Processing " + geonamesFileName + " ---");
        long startTime = System.currentTimeMillis();
        builder.processAllCountriesPass1(geonamesFilePath);
        System.out.println("Pass 1 finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");
        if (builder.relevantAdminMap.isEmpty()) {
            System.err.println("No relevant PCLI/ADM1/ADM2 features found. Exiting.");
            return;
        }
        System.out.println("\n--- Pass 2: Building Initial ADM1 Hierarchy ---");
        startTime = System.currentTimeMillis();
        TreeNode root = builder.buildInitialAdm1Hierarchy();
        System.out.println("Pass 2 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        if (root == null || root.children.isEmpty()) {
            System.err.println("Initial hierarchy building failed. Exiting.");
            return;
        }
        System.out.println("\n--- Pass 3: Assigning Initial PPL-Aggregated Pop/Bounds & Identifying Nodes > 1M ---");
        startTime = System.currentTimeMillis();
        List<TreeNode> nodesToExpand1M = new ArrayList<>();
        builder.assignInitialPopBoundsAndIdentifyExpansions(root, nodesToExpand1M);
        System.out.println("Pass 3 finished in " + (System.currentTimeMillis() - startTime) + " ms. Identified "
                + nodesToExpand1M.size() + " ADM1 nodes > 1M initial pop.");
        System.out.println("\n--- Pass 4: Adding ADM2 Layer ---");
        startTime = System.currentTimeMillis();
        builder.addAdm2Layer(nodesToExpand1M);
        System.out.println("Pass 4 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 5: Distributing Initial ADM1 Population to ADM2s ---");
        startTime = System.currentTimeMillis();
        builder.distributeAdm1Population(nodesToExpand1M);
        System.out.println("Pass 5 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 6: Estimating Missing ADM2 Bounding Boxes ---");
        startTime = System.currentTimeMillis();
        builder.estimateMissingAdm2Bounds(nodesToExpand1M);
        System.out.println("Pass 6 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 7: Population Scaling and Internet Population Calculation ---");
        startTime = System.currentTimeMillis();
        builder.scaleAndCalculateInternetPop(root);
        System.out.println("Pass 7 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 8: Expanding Leaves with Population > 1,000,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_1M, false, TreeNode.NodeType.S_ADM3, "Pass 8");
        System.out.println("Pass 8 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 9: Expanding Leaves with Population > 10,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_10K, true, TreeNode.NodeType.S_ADM4, "Pass 9");
        System.out.println("Pass 9 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        System.out.println("\n--- Pass 10: Final Recalculation of Aggregated Data ---");
        startTime = System.currentTimeMillis();
        builder.finalAggregateTotalPopulation(root);
        builder.finalAggregateInternetPopulation(root);
        builder.finalAggregateBounds(root);
        System.out.println("Pass 10 finished in " + (System.currentTimeMillis() - startTime) + " ms.");

        // --- Write Output JSON ---
        System.out.println("\n--- Writing Final Hierarchy to JSON File ---");
        System.out.println("Output JSON file: " + outputJsonFilePath);
        if (root != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                File outputFile = new File(outputJsonFilePath);
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    System.out.println("Creating output directory: " + outputDir.getAbsolutePath());
                    if (!outputDir.mkdirs()) {
                        System.err.println("Error: Failed to create output directory: " + outputDir.getAbsolutePath());
                    }
                }
                if (outputDir == null || outputDir.exists()) {
                    objectMapper.writeValue(outputFile, root);
                    System.out.println("Successfully wrote hierarchy to " + outputJsonFilePath);
                }
            } catch (IOException e) {
                System.err.println("Error writing hierarchy to JSON file: " + e.getMessage());
                e.printStackTrace();
                // Fallback to Text Output
                System.out.println("\n--- JSON writing failed. Falling back to Text Output ---");
                System.out.println("Output Text file: " + outputTextFilePath);
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputTextFilePath)))) {
                    File textOutputFile = new File(outputTextFilePath);
                    File textOutputDir = textOutputFile.getParentFile();
                    if (textOutputDir != null && !textOutputDir.exists()) {
                        System.out.println(
                                "Creating output directory for text fallback: " + textOutputDir.getAbsolutePath());
                        if (!textOutputDir.mkdirs()) {
                            System.err.println("Error: Failed to create output directory for text fallback.");
                        }
                    }
                    if (textOutputDir == null || textOutputDir.exists()) {
                        root.printTree(writer, "");
                        System.out.println("Successfully wrote hierarchy to text file: " + outputTextFilePath);
                    }
                } catch (IOException textEx) {
                    System.err.println("Error writing hierarchy to fallback text file: " + textEx.getMessage());
                    textEx.printStackTrace();
                }
            }
        } else {
            System.err.println("Root node is null, cannot write output.");
        }

        // --- Finish ---
        long overallEndTime = System.currentTimeMillis();
        System.out.println("\nTotal execution time: " + (overallEndTime - overallStartTime) / 1000.0 + " seconds.");
    }
}