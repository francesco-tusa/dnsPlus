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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// --- Jackson Imports ---
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import utils.CustomLogger;

// --- Imports for FIXED Bounding Box logic ---
import simulator.core.Location;
import simulator.regions.Region; 

import com.fasterxml.jackson.annotation.JsonInclude;

// This class is part of FileBasedDynamicBuilder.java

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

    private static final Logger logger = CustomLogger.getLogger(GeonameEntry.class.getName());

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
            logger.log(Level.WARNING, "Critical parse error: " + String.join("|", parts), e);
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
    public Region bounds;
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
    
    // Copy constructor for subset generation
    public TreeNode(TreeNode other) {
        this.geonameId = other.geonameId;
        this.name = other.name;
        this.featureCode = other.featureCode;
        this.code = other.code;
        this.type = other.type;
        this.aggregatedPopulation = other.aggregatedPopulation;
        this.internetPopulation = other.internetPopulation;
        this.officialPopulation = other.officialPopulation;
        this.internetPenetrationRate = other.internetPenetrationRate;
        if (other.bounds != null) {
            this.bounds = new Region(other.bounds);
        }
        this.children = new ArrayList<>();
    }

    @Override
    public String toString() {
        String boundsStr = (bounds != null && bounds.getBottomLeft() != null) ? ", bounds=" + bounds.toShortString() : "";
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

    private static final Logger logger = CustomLogger.getLogger(FileBasedDynamicBuilder.class.getName());

    // --- Maps for reference data ---
    Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    Map<String, String> countryToContinentMap = new HashMap<>();
    Map<String, Integer> countryCodeToIdMap = new HashMap<>();
    // --- NEW: Map to store names for all country-level entities ---
    Map<String, String> countryCodeToNameMap = new HashMap<>();
    Map<String, Double> countryIsoToPenetrationMap = new HashMap<>();
    Map<Integer, Long> countryIdToOfficialPopulationMap = new HashMap<>();

    // --- Maps for aggregated data from Pass 1 ---
    Map<Integer, RelevantAdminInfo> relevantAdminMap = new HashMap<>();
    Map<Integer, Long> adm1PopulationMap = new HashMap<>();
    Map<Integer, Region> adm1BoundsMap = new HashMap<>();
    Map<Integer, Long> adm2PopulationMap = new HashMap<>();
    Map<Integer, Region> adm2BoundsMap = new HashMap<>();

    // --- Final Tree structure ---
    Map<Integer, TreeNode> nodeMap = new HashMap<>();

    // --- Constants ---
    final long POPULATION_THRESHOLD_1M = 1_000_000;
    final long POPULATION_THRESHOLD_10K = 10_000;
    final String GEONAMES_BASE_URL = "https://download.geonames.org/export/dump/";

    // --- Loading Methods ---
    void loadAdminCodes(String filePath, Map<String, Integer> map, String adminLevelName) {
        int count = 0;
        logger.info("Loading " + adminLevelName + " codes from " + filePath + "...");
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
                        logger.log(Level.WARNING, "Skipping invalid " + adminLevelName + " line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading " + adminLevelName + " codes file: " + filePath, e);
        }
        logger.info("Loaded " + count + " " + adminLevelName + " code mappings.");
    }

    // --- MODIFICATION: Updated to load country names ---
    void loadCountryInfo(String filePath) {
        final int ISO_CODE_IDX = 0;
        final int ISO_NAME_IDX = 4; // Index of the country name
        final int CONTINENT_CODE_IDX = 8;
        final int GEONAMEID_IDX = 16;
        final int MIN_COUNTRY_PARTS = 17;
        logger.info("Loading country info from " + filePath + "...");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1);
                if (parts.length >= MIN_COUNTRY_PARTS) {
                    String isoCode = parts[ISO_CODE_IDX].trim();
                    String countryName = parts[ISO_NAME_IDX].trim();
                    String continentCode = parts[CONTINENT_CODE_IDX].trim();
                    String geonameIdStr = parts[GEONAMEID_IDX].trim();

                    if (!isoCode.isEmpty() && !continentCode.isEmpty()) {
                        countryToContinentMap.put(isoCode, continentCode);
                    }
                    if (!isoCode.isEmpty() && !countryName.isEmpty()) {
                        countryCodeToNameMap.put(isoCode, countryName); // Store name
                    }
                    if (!isoCode.isEmpty() && !geonameIdStr.isEmpty()) {
                        try {
                            int geonameId = Integer.parseInt(geonameIdStr);
                            countryCodeToIdMap.put(isoCode, geonameId);
                        } catch (NumberFormatException e) {
                            logger.warning("Skipping country line due to invalid Geoname ID: " + line);
                        }
                    }
                } else {
                    logger.warning("Skipping short country info line: " + line);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading country info file: " + filePath, e);
        }
        logger.info("Loaded " + countryToContinentMap.size() + " country->continent mappings.");
        logger.info("Loaded " + countryCodeToIdMap.size() + " country ISO2->GeonameID mappings.");
        logger.info("Loaded " + countryCodeToNameMap.size() + " country ISO2->Name mappings.");
    }

    void loadInternetPenetration(String filePath) {
        logger.info("Loading Internet Penetration data from " + filePath + "...");
        countryIsoToPenetrationMap.clear();
        int validRatesLoaded = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line == null) {
                logger.severe("Error: Internet penetration file is empty.");
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
                logger.severe("Error: Could not find 'ISO2 Code' column in penetration file.");
                return;
            }
            if (yearIndices.isEmpty()) {
                logger.severe("Error: Could not find any valid year columns in penetration file.");
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
            logger.log(Level.SEVERE, "Error reading internet penetration file: " + filePath, e);
        }
        logger.info("Loaded latest internet penetration rates for " + validRatesLoaded + " countries.");
    }

    // --- Processing Passes ---
    void processAllCountriesPass1(String filePath) {
        logger.info("Starting Pass 1: Processing " + filePath + "...");
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
                    // --- This is key: we store the official population by GeonameID ---
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
                        Location point = new Location(entry.longitude, entry.latitude, 0);
                        
                        if (parentAdm1Id != null) {
                            Region region1 = adm1BoundsMap.computeIfAbsent(parentAdm1Id, k -> new Region());
                            region1.expand(point);
                        }
                        if (parentAdm2Id != null) {
                            Region region2 = adm2BoundsMap.computeIfAbsent(parentAdm2Id, k -> new Region());
                            region2.expand(point);
                        }
                    }
                }
                if (lineCount % 1000000 == 0) {
                    logger.info("  Processed " + lineCount + " lines...");
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading geonames file in Pass 1: " + filePath, e);
        }

        logger.info("Pass 1 Complete. Found PCLI: " + pcliCount + ", ADM1: " + adm1Count + ", ADM2: " + adm2Count);
        logger.info("Aggregated population from " + pplCount + " PPLs: " + totalPplPop + " into "
                + adm1PopulationMap.size() + " ADM1 & " + adm2PopulationMap.size() + " ADM2 regions.");
        logger.info("Skipped " + skippedPplCount + " PPLs with total population " + skippedPplPop
                + " due to missing/unmatched ADM1 link.");
        logger.info(
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

    // --- MODIFICATION: This entire method is replaced ---
    TreeNode buildInitialAdm1Hierarchy() {
        logger.info("Starting Pass 2: Building initial hierarchy (World -> Continent -> Country/Territory -> ADM1)...");
        TreeNode worldRoot = new TreeNode("World", TreeNode.NodeType.WORLD, "WORLD");
        Map<String, TreeNode> continentNodes = new HashMap<>();
        nodeMap.clear();
        nodeMap.put(0, worldRoot); // Add the root

        // --- NEW Step A: Create nodes for ALL entries in countryInfo.txt (countries AND territories) ---
        logger.info("  Creating " + countryCodeToIdMap.size() + " country-level nodes from countryInfo.txt...");
        for (String isoCode : countryCodeToIdMap.keySet()) {
            Integer geonameId = countryCodeToIdMap.get(isoCode);
            if (geonameId == null) continue;

            String name = countryCodeToNameMap.get(isoCode);
            if (name == null || name.isEmpty()) name = isoCode; // Fallback to ISO code if name is missing

            // Create the node with type COUNTRY (this now represents any country-level entity)
            // The featureCode "COUNTRY_LEVEL" is arbitrary to distinguish from PCLI if needed
            TreeNode node = new TreeNode(geonameId, name, TreeNode.NodeType.COUNTRY, isoCode, "COUNTRY_LEVEL");

            // Check if this entity is an independent nation (PCLI) to get its official population
            // We get this from the map populated in Pass 1
            Long officialPop = countryIdToOfficialPopulationMap.get(geonameId);
            if (officialPop != null && officialPop > 0) {
                node.officialPopulation = officialPop;
            }

            // Get and set internet penetration rate
            Double penetrationRate = countryIsoToPenetrationMap.get(isoCode);
            if (penetrationRate != null && penetrationRate > 0.0) {
                node.internetPenetrationRate = penetrationRate;
            } else {
                logger.warning(
                        "Warning: Country/Territory " + node.name + " (" + isoCode + ") has missing/zero internet penetration rate.");
            }

            // Add the new Country/Territory node to the main map
            nodeMap.put(geonameId, node);
        }
        logger.info("  Created " + (nodeMap.size() - 1) + " country-level TreeNodes.");

        // --- NEW Step B: Create nodes for all ADM1s ---
        int adm1NodesCreated = 0;
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            if (info.type == TreeNode.NodeType.ADM1) {
                if (!nodeMap.containsKey(info.geonameId)) { // Avoid overwriting
                    TreeNode node = new TreeNode(info.geonameId, info.name, info.type, info.code, info.featureCode);
                    nodeMap.put(info.geonameId, node);
                    adm1NodesCreated++;
                }
            }
        }
        logger.info("  Created " + adm1NodesCreated + " ADM1 TreeNodes.");


        // --- NEW Step C: Link ADM1 nodes to their Country/Territory parents ---
        int countryLinks = 0, adm1Links = 0, failedLinks = 0;
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            // Find the child node (must be ADM1)
            if (info.type != TreeNode.NodeType.ADM1) continue;
            
            TreeNode childNode = nodeMap.get(info.geonameId);
            if (childNode == null) continue; 

            TreeNode parentNode = null;
            // Find its parent (which is a COUNTRY_LEVEL node) using its countryCode
            Integer parentCountryId = countryCodeToIdMap.get(info.countryCode);
            if (parentCountryId != null) {
                parentNode = nodeMap.get(parentCountryId);
            }
            
            if (parentNode != null && parentNode.type == TreeNode.NodeType.COUNTRY) {
                parentNode.addChild(childNode);
                adm1Links++;
            } else {
                failedLinks++;
                logger.warning("Failed to link ADM1: " + childNode.name + " - Parent country (ISO: "
                        + info.countryCode + ") not found in nodeMap.");
            }
        }
        
        // --- NEW Step D: Link all COUNTRY_LEVEL nodes to their GEOGRAPHICAL Continents ---
        for (TreeNode childNode : nodeMap.values()) {
             if (childNode.type == TreeNode.NodeType.COUNTRY) {
                // childNode.code is the ISO code (e.g., "FR", "GF", "PF")
                String continentCode = countryToContinentMap.get(childNode.code); 
                TreeNode parentNode = null;
                
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
                    // Handle countries/territories with no continent code (e.g., Antarctica 'AQ')
                    if ("AQ".equals(childNode.code)) {
                         parentNode = continentNodes.computeIfAbsent("AN", k -> {
                             TreeNode newNode = new TreeNode("Antarctica", TreeNode.NodeType.CONTINENT, "AN");
                             worldRoot.addChild(newNode);
                             return newNode;
                         });
                    } else {
                        parentNode = worldRoot;
                        logger.warning(
                                "Warning: Continent not found for country: " + childNode.name + ". Linking to World.");
                    }
                }
                parentNode.addChild(childNode);
                countryLinks++;
            }
        }

        logger.info("Initial hierarchy linking complete. Country/Territory links to Continents: " + countryLinks + ", ADM1 links to Countries: "
                + adm1Links + ", Failed links: " + failedLinks);
        return worldRoot;
    }

    Region assignInitialPopBoundsAndIdentifyExpansions(TreeNode node, List<TreeNode> nodesToExpand) {
        Region nodeBounds = new Region(); // Use new Region()
        long currentAggregatedPop = 0;

        if (node.type == TreeNode.NodeType.ADM1) {
            node.aggregatedPopulation = adm1PopulationMap.getOrDefault(node.geonameId, 0L);
            node.bounds = adm1BoundsMap.get(node.geonameId); // This is a Region now
            if (node.aggregatedPopulation > POPULATION_THRESHOLD_1M) {
                nodesToExpand.add(node);
            }
            // Check if bounds are initialized
            return node.bounds != null && node.bounds.getBottomLeft() != null ? node.bounds : new Region();
        }

        if (node.children != null && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                Region childBounds = assignInitialPopBoundsAndIdentifyExpansions(child, nodesToExpand);
                currentAggregatedPop += child.aggregatedPopulation;
                nodeBounds.expand(childBounds); // Use Region.expand(BaseRegion)
            }
        }
        node.aggregatedPopulation = currentAggregatedPop;
        node.bounds = nodeBounds;
        return nodeBounds;
    }

    void addAdm2Layer(List<TreeNode> nodesToExpand) {
        logger.info("Starting Pass 4: Adding ADM2 layer for " + nodesToExpand.size() + " ADM1 nodes...");
        int adm2Added = 0;
        if (relevantAdminMap == null || nodeMap == null || adm2PopulationMap == null || adm2BoundsMap == null) {
            logger.severe("Error: Maps not initialized for ADM2 layer addition.");
            return;
        }
        for (TreeNode adm1Node : nodesToExpand) {
            RelevantAdminInfo adm1NodeInfo = relevantAdminMap.get(adm1Node.geonameId);
            if (adm1NodeInfo == null || adm1NodeInfo.countryCode == null) {
                logger.warning(
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
                        logger.warning("Warning: ADM2 Node already exists in map: " + adm2Info.name);
                    }
                }
            }
        }
        logger.info("Pass 4 Complete. Added " + adm2Added + " ADM2 nodes.");
    }

    void distributeAdm1Population(List<TreeNode> expandedAdm1Nodes) {
        logger.info(
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
                    logger.warning("Warning: ADM2 populations sum exceeds ADM1 PPL-aggregated population for "
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
        logger.info("Pass 5 complete. Assigned estimated population (" + totalPopDistributed + ") to "
                + adm2PopDistributed + " previously zero-pop ADM2 nodes.");
    }

    /**
     * Normalizes a longitude value to be within the [-180, 180] range.
     * @param lon The longitude to normalize.
     * @return The normalized longitude.
     */
    private double normalizeLongitude(double lon) {
        while (lon <= -180.0) lon += 360.0;
        while (lon > 180.0) lon -= 360.0;
        return lon;
    }

    void estimateMissingAdm2Bounds(List<TreeNode> expandedAdm1Nodes) {
        logger.info("Starting Pass 6: Estimating missing bounds for ADM2 children...");
        int boundsEstimated = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            if (adm1Node.bounds == null || adm1Node.bounds.getBottomLeft() == null) {
                logger.warning("Warning: Cannot estimate ADM2 bounds for children of " + adm1Node.name
                        + " - parent bounds uninitialized.");
                continue;
            }

            List<TreeNode> adm2ChildrenMissingBounds = new ArrayList<>();
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2 && (child.bounds == null || child.bounds.getBottomLeft() == null)) {
                    adm2ChildrenMissingBounds.add(child);
                }
            }

            int totalAdm2ToEstimate = adm2ChildrenMissingBounds.size();
            if (totalAdm2ToEstimate == 0) {
                continue;
            }

            int gridCols = (int) Math.ceil(Math.sqrt(totalAdm2ToEstimate));
            int gridRows = (int) Math.ceil((double) totalAdm2ToEstimate / gridCols);

            double startLat = adm1Node.bounds.getBottomLeft().getY();
            double totalLatSpan = adm1Node.bounds.getHeight();
            double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;

            double startLon = adm1Node.bounds.getBottomLeft().getX(); 
            double totalLonSpan = adm1Node.bounds.getWidth(); // Wrap-aware
            double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;
            
            adm2ChildrenMissingBounds.sort(Comparator.comparing(a -> a.name));
            boolean estimationLogged = false;

            for (int i = 0; i < totalAdm2ToEstimate; i++) {
                TreeNode adm2ChildNode = adm2ChildrenMissingBounds.get(i);
                if (!estimationLogged) {
                    logger.info("  Estimating " + totalAdm2ToEstimate + " ADM2 bounds under " + adm1Node.name + " using " + gridRows + "x" + gridCols + " grid.");
                    estimationLogged = true;
                }
                
                int r = i / gridCols;
                int c = i % gridCols;

                double newMinLat = startLat + r * cellHeight;
                double newMaxLat = startLat + (r + 1) * cellHeight;
                
                double newMinLon = normalizeLongitude(startLon + c * cellWidth);
                double newMaxLon = normalizeLongitude(startLon + (c + 1) * cellWidth);

                newMinLat = Math.max(newMinLat, adm1Node.bounds.getBottomLeft().getY());
                newMaxLat = Math.min(newMaxLat, adm1Node.bounds.getTopRight().getY());
                
                if (newMaxLat < newMinLat) newMaxLat = newMinLat;
                
                if (Math.abs(newMinLon - newMaxLon) > 359.999) {
                     adm2ChildNode.bounds = new Region(adm1Node.bounds);
                } else if (newMinLon == 180.0 && newMaxLon == -180.0) {
                     newMaxLon = 180.0; 
                     adm2ChildNode.bounds = new Region(new Location(newMinLon, newMinLat, 0), new Location(newMaxLon, newMaxLat, 0));
                } else {
                     adm2ChildNode.bounds = new Region(new Location(newMinLon, newMinLat, 0), new Location(newMaxLon, newMaxLat, 0));
                }

                boundsEstimated++;
            }
        }
        logger.info("Pass 6 complete. Estimated grid-based bounds for " + boundsEstimated + " ADM2 nodes.");
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
        logger.info("Starting Pass 7: Scaling populations and calculating internet population...");
        int countriesProcessed = 0;
        if (root == null || root.children.isEmpty()) {
            logger.severe("Error: Root node is null or has no children.");
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
                
                long currentAggregatedTotal = sumCurrentPopulation(country);
                
                if (officialPop <= 0) {
                     if (currentAggregatedTotal <= 0) {
                        logger.warning("Warning: Skipping scaling for " + country.name
                                + " - zero official and zero aggregated population.");
                     } else {
                         logger.warning("Warning: Skipping scaling for " + country.name
                                + " - zero/missing official population. Using aggregated pop as base.");
                     }
                    applyScalingAndInternetPop(country, 1.0, penetrationRate);
                    continue;
                }
                
                if (currentAggregatedTotal <= 0) {
                    logger.warning("Warning: Skipping scaling for country " + country.name
                            + " - zero current aggregated population. Cannot scale.");
                    applyScalingAndInternetPop(country, 1.0, penetrationRate);
                    continue;
                }
                double scaleFactor = (double) officialPop / currentAggregatedTotal;
                applyScalingAndInternetPop(country, scaleFactor, penetrationRate);
            }
        }
        logger.info("Pass 7 Complete. Processed population scaling and internet population calculation for "
                + countriesProcessed + " countries.");
    }

    boolean expandLeafNodeIfNeeded(TreeNode leafNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) {
            return false;
        }
        
        if (leafNode.bounds == null || leafNode.bounds.getBottomLeft() == null) {
            logger.warning("Warning: Cannot expand leaf node " + leafNode.name + " - invalid/uninitialized bounds.");
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

        double startLat = leafNode.bounds.getBottomLeft().getY();
        double totalLatSpan = leafNode.bounds.getHeight();
        double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;

        double startLon = leafNode.bounds.getBottomLeft().getX(); 
        double totalLonSpan = leafNode.bounds.getWidth(); // Wrap-aware
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

            double newMinLat = startLat + r * cellHeight;
            double newMaxLat = startLat + (r + 1) * cellHeight;
            
            double newMinLon = normalizeLongitude(startLon + c * cellWidth);
            double newMaxLon = normalizeLongitude(startLon + (c + 1) * cellWidth);

            newMinLat = Math.max(newMinLat, leafNode.bounds.getBottomLeft().getY());
            newMaxLat = Math.min(newMaxLat, leafNode.bounds.getTopRight().getY());
            if (newMaxLat < newMinLat) newMaxLat = newMinLat;
                
            if (Math.abs(newMinLon - newMaxLon) > 359.999) {
                 childNode.bounds = new Region(leafNode.bounds);
            } else if (newMinLon == 180.0 && newMaxLon == -180.0) {
                 newMaxLon = 180.0; 
                 childNode.bounds = new Region(new Location(newMinLon, newMinLat, 0), new Location(newMaxLon, newMaxLat, 0));
            } else {
                 childNode.bounds = new Region(new Location(newMinLon, newMinLat, 0), new Location(newMaxLon, newMaxLat, 0));
            }

            leafNode.addChild(childNode);
        }
        if (popSumCheck != parentPop) {
            logger.warning(String.format("WARN: Total Pop distribution error for %s. Orig: %d, Dist: %d", leafNode.name,
                    parentPop, popSumCheck));
        }
        if (internetPopSumCheck != parentInternetPop) {
            logger.warning(String.format("WARN: Internet Pop distribution error for %s. Orig: %d, Dist: %d", leafNode.name,
                    parentInternetPop, internetPopSumCheck));
        }
        return true;
    }

    void findAndExpandLeaves(TreeNode startNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType, String passIdentifier) {
        logger.info("Starting " + passIdentifier + ": Expanding Leaves with Population > " + threshold + "...");
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
        logger.info("  Found " + leavesToExpand.size() + " leaves exceeding threshold " + threshold + ".");
        int expandedCount = 0;
        for (TreeNode leaf : leavesToExpand) {
            if (expandLeafNodeIfNeeded(leaf, threshold, distributeEqually, artificialChildType)) {
                expandedCount++;
            }
        }
        logger.info(passIdentifier + " Complete. Expanded " + expandedCount + " leaves.");
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
            logger.warning(String.format("WARN: Final aggregation internetPop > totalPop for %s. Clamping.", node.name));
            node.internetPopulation = node.aggregatedPopulation;
        }
        return totalInternetPopulation;
    }

    Region finalAggregateBounds(TreeNode node) {
        if (node == null)
            return new Region();
        
        if (node.children.isEmpty()) {
            return node.bounds != null && node.bounds.getBottomLeft() != null ? node.bounds : new Region();
        }

        Region calculatedBounds = new Region();
        for (TreeNode child : node.children) {
            Region childBounds = finalAggregateBounds(child);
            calculatedBounds.expand(childBounds); 
        }
        node.bounds = calculatedBounds;
        return calculatedBounds;
    }

    // --- File Download and Extraction Logic ---

    private boolean ensureDataFilesExist(String resourcesDirName) {
        logger.info("\n--- Checking for required GeoNames data files ---");

        File resourcesDir = new File(resourcesDirName);
        if (!resourcesDir.exists()) {
            logger.info("Creating directory: " + resourcesDir.getAbsolutePath());
            if (!resourcesDir.mkdirs()) {
                logger.severe("Error: Failed to create resources directory: " + resourcesDir.getAbsolutePath());
                return false;
            }
        }

        String[][] requiredFiles = {
                { "allCountries.txt", GEONAMES_BASE_URL + "allCountries.zip", "true", "allCountries.txt" },
                { "admin1CodesASCII.txt", GEONAMES_BASE_URL + "admin1CodesASCII.txt", "false", null },
                { "admin2Codes.txt", GEONAMES_BASE_URL + "admin2Codes.txt", "false", null },
                { "countryInfo.txt", GEONAMES_BASE_URL + "countryInfo.txt", "false", null }
        };

        boolean allFilesOk = true;
        for (String[] fileInfo : requiredFiles) {
            String fileName = fileInfo[0];
            String fileUrl = fileInfo[1];
            boolean isZipped = Boolean.parseBoolean(fileInfo[2]);
            String zipEntryName = fileInfo[3];
            Path destinationPath = Paths.get(resourcesDirName, fileName);

            if (!Files.exists(destinationPath)) {
                logger.info("File not found: " + destinationPath + ". Attempting download...");
                boolean success = downloadAndExtractFile(fileUrl, destinationPath, isZipped, zipEntryName);
                if (!success) {
                    logger.severe("Failed to download or extract: " + fileName);
                    allFilesOk = false;
                }
            } else {
                logger.info("File found: " + destinationPath);
            }
        }

        Path penetrationPath = Paths.get(resourcesDirName, "internet_penetration_iso2.csv");
        if (!Files.exists(penetrationPath)) {
            logger.severe("Error: Required file internet_penetration_iso2.csv not found in " + resourcesDirName);
            logger.severe("Please add this file manually to the resources directory.");
            allFilesOk = false;
        } else {
            logger.info("File found: " + penetrationPath);
        }

        if (!allFilesOk) {
            logger.severe("One or more required data files are missing or could not be downloaded.");
        }
        return allFilesOk;
    }

    private boolean downloadAndExtractFile(String fileUrl, Path destinationPath, boolean isZipped,
            String zipEntryName) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Path tempZipPath = null; 

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                long fileSize = connection.getContentLengthLong();
                logger.info(String.format("  Downloading %s (Size: %s)...",
                        destinationPath.getFileName(),
                        fileSize > 0 ? String.format("%,d bytes", fileSize) : "Unknown"));

                if (isZipped) {
                    tempZipPath = Files.createTempFile("geonames_", ".zip");
                    Files.copy(inputStream, tempZipPath, StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close(); 

                    logger.info("  Extracting " + zipEntryName + " from " + tempZipPath.getFileName() + "...");
                    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZipPath))) {
                        ZipEntry entry;
                        boolean entryFound = false;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.getName().equals(zipEntryName)) {
                                try (OutputStream fos = new FileOutputStream(destinationPath.toFile())) {
                                    byte[] buffer = new byte[8192]; 
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                logger.info("  Successfully extracted to " + destinationPath);
                                entryFound = true;
                                break; 
                            }
                            zis.closeEntry(); 
                        }
                        if (!entryFound) {
                            logger.severe(
                                    "Error: Entry '" + zipEntryName + "' not found in downloaded zip file: " + fileUrl);
                            return false;
                        }
                    }
                } else {
                    Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("  Successfully downloaded to " + destinationPath);
                }
                return true; 

            } else {
                logger.severe("Error: Failed to download file. Server responded with code: " + responseCode
                        + " for URL: " + fileUrl);
                return false;
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during download/extraction for " + fileUrl, e);
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    /* Ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
            if (tempZipPath != null) {
                try {
                    Files.deleteIfExists(tempZipPath);
                } catch (IOException e) {
                    logger.warning("Warning: Failed to delete temporary file: " + tempZipPath);
                }
            }
        }
    }
    
    // --- New method to create a subset of the topology ---
    TreeNode createSubsetTopology(TreeNode fullRoot) {
        if (fullRoot == null) return null;

        // Create new root for the subset
        TreeNode subsetRoot = new TreeNode(fullRoot);
        subsetRoot.children = new ArrayList<>();

        for (TreeNode continent : fullRoot.children) {
            if (continent.type == TreeNode.NodeType.CONTINENT && "AS".equals(continent.code)) {
                TreeNode subsetContinent = new TreeNode(continent);
                subsetRoot.addChild(subsetContinent);
                
                for (TreeNode country : continent.children) {
                    // Keep Bangladesh (BD) and all its descendants
                    if ("BD".equals(country.code)) {
                        subsetContinent.addChild(cloneSubtree(country));
                    } 
                    // Keep China (CN) but only the Beijing branch
                    else if ("CN".equals(country.code)) {
                        TreeNode subsetChina = new TreeNode(country);
                        subsetContinent.addChild(subsetChina);
                        
                        for (TreeNode adm1 : country.children) {
                            // Beijing usually has code '19' or '22' depending on version, 
                            // but name is safer: "Beijing"
                            if (adm1.name.contains("Beijing")) {
                                subsetChina.addChild(cloneSubtree(adm1));
                            }
                        }
                        // If no Beijing found, China node remains empty (but exists)
                    }
                }
            }
        }
        return subsetRoot;
    }
    
    // Deep copy a node and all its descendants
    private TreeNode cloneSubtree(TreeNode root) {
        if (root == null) return null;
        TreeNode copy = new TreeNode(root);
        for (TreeNode child : root.children) {
            copy.addChild(cloneSubtree(child));
        }
        return copy;
    }


    // --- Main Execution Logic ---
    public static void main(String[] args) {
        CustomLogger.setGlobalLogLevel(Level.INFO, "geonames-builder");

        String resourcesDirName = "resources/world";
        String outputDirName = "output";

        String geonamesFileName = "allCountries.txt";
        String admin1FileName = "admin1CodesASCII.txt";
        String admin2FileName = "admin2Codes.txt";
        String countryInfoFileName = "countryInfo.txt";
        String internetPenetrationFileName = "internet_penetration_iso2.csv";
        String outputTextFileName = "geonames_hierarchy_output.txt";
        String outputJsonFileName = "geonames_topology.json";
        
        // New output file for the subset
        String outputSubsetJsonFileName = "geonames_subset_bangladesh_beijing.json";

        String geonamesFilePath = resourcesDirName + File.separator + geonamesFileName;
        String admin1FilePath = resourcesDirName + File.separator + admin1FileName;
        String admin2FilePath = resourcesDirName + File.separator + admin2FileName;
        String countryInfoFilePath = resourcesDirName + File.separator + countryInfoFileName;
        String internetPenetrationFilePath = resourcesDirName + File.separator + internetPenetrationFileName;
        String outputTextFilePath = outputDirName + File.separator + outputTextFileName;
        String outputJsonFilePath = outputDirName + File.separator + outputJsonFileName;
        String outputSubsetJsonFilePath = outputDirName + File.separator + outputSubsetJsonFileName;

        FileBasedDynamicBuilder builder = new FileBasedDynamicBuilder();
        long overallStartTime = System.currentTimeMillis();

        if (!builder.ensureDataFilesExist(resourcesDirName)) {
            logger.severe("Cannot proceed without required data files. Exiting.");
            return; 
        }

        logger.info("\n--- Loading Index & Data Files ---");
        builder.loadCountryInfo(countryInfoFilePath); // Modified
        builder.loadAdminCodes(admin1FilePath, builder.admin1CodeToIdMap, "ADM1");
        builder.loadAdminCodes(admin2FilePath, builder.admin2CodeToIdMap, "ADM2");
        builder.loadInternetPenetration(internetPenetrationFilePath); 
        if (builder.countryToContinentMap.isEmpty() || builder.admin1CodeToIdMap.isEmpty()
                || builder.admin2CodeToIdMap.isEmpty() || builder.countryIsoToPenetrationMap.isEmpty()) {
            logger.severe("Failed to load essential index/data files after checking/downloading. Exiting.");
            return;
        }

        logger.info("\n--- Pass 1: Processing " + geonamesFileName + " ---");
        long startTime = System.currentTimeMillis();
        builder.processAllCountriesPass1(geonamesFilePath);
        logger.info("Pass 1 finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds.");
        if (builder.relevantAdminMap.isEmpty()) {
            logger.severe("No relevant PCLI/ADM1/ADM2 features found. Exiting.");
            return;
        }
        logger.info("\n--- Pass 2: Building Initial ADM1 Hierarchy ---");
        startTime = System.currentTimeMillis();
        TreeNode root = builder.buildInitialAdm1Hierarchy(); // Modified
        logger.info("Pass 2 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        if (root == null || root.children.isEmpty()) {
            logger.severe("Initial hierarchy building failed. Exiting.");
            return;
        }
        logger.info("\n--- Pass 3: Assigning Initial PPL-Aggregated Pop/Bounds & Identifying Nodes > 1M ---");
        startTime = System.currentTimeMillis();
        List<TreeNode> nodesToExpand1M = new ArrayList<>();
        builder.assignInitialPopBoundsAndIdentifyExpansions(root, nodesToExpand1M);
        logger.info("Pass 3 finished in " + (System.currentTimeMillis() - startTime) + " ms. Identified "
                + nodesToExpand1M.size() + " ADM1 nodes > 1M initial pop.");
        logger.info("\n--- Pass 4: Adding ADM2 Layer ---");
        startTime = System.currentTimeMillis();
        builder.addAdm2Layer(nodesToExpand1M);
        logger.info("Pass 4 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 5: Distributing Initial ADM1 Population to ADM2s ---");
        startTime = System.currentTimeMillis();
        builder.distributeAdm1Population(nodesToExpand1M);
        logger.info("Pass 5 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 6: Estimating Missing ADM2 Bounding Boxes ---");
        startTime = System.currentTimeMillis();
        builder.estimateMissingAdm2Bounds(nodesToExpand1M);
        logger.info("Pass 6 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 7: Population Scaling and Internet Population Calculation ---");
        startTime = System.currentTimeMillis();
        builder.scaleAndCalculateInternetPop(root);
        logger.info("Pass 7 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 8: Expanding Leaves with Population > 1,000,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_1M, false, TreeNode.NodeType.S_ADM3, "Pass 8");
        logger.info("Pass 8 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 9: Expanding Leaves with Population > 10,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_10K, true, TreeNode.NodeType.S_ADM4, "Pass 9");
        logger.info("Pass 9 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        logger.info("\n--- Pass 10: Final Recalculation of Aggregated Data ---");
        startTime = System.currentTimeMillis();
        builder.finalAggregateTotalPopulation(root);
        builder.finalAggregateInternetPopulation(root);
        builder.finalAggregateBounds(root);
        logger.info("Pass 10 finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        
        // --- Subset Generation ---
        logger.info("\n--- Generating Subset Topology (Bangladesh & Beijing) ---");
        TreeNode subsetRoot = builder.createSubsetTopology(root);
        if (subsetRoot != null) {
            builder.finalAggregateTotalPopulation(subsetRoot); // Recalculate totals for subset
            builder.finalAggregateInternetPopulation(subsetRoot);
            builder.finalAggregateBounds(subsetRoot);
        }

        logger.info("\n--- Writing Final Hierarchies to JSON Files ---");
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            File outputDir = new File(outputDirName);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // Write Full Topology
            logger.info("Writing full topology to: " + outputJsonFilePath);
            objectMapper.writeValue(new File(outputJsonFilePath), root);
            
            // Write Subset Topology
            if (subsetRoot != null) {
                logger.info("Writing subset topology to: " + outputSubsetJsonFilePath);
                objectMapper.writeValue(new File(outputSubsetJsonFilePath), subsetRoot);
            } else {
                logger.warning("Subset root was null. Skipping subset file generation.");
            }
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing hierarchy to JSON file", e);
        }

        long overallEndTime = System.currentTimeMillis();
        logger.info("\nTotal execution time: " + (overallEndTime - overallStartTime) / 1000.0 + " seconds.");
    }
}