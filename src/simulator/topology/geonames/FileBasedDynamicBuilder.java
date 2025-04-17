package simulator.topology.geonames; // Added package declaration back

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
// Removed HashSet and Set as they weren't used after refactoring
// Removed java.util.Objects as it wasn't used after refactoring

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
        if (other.maxLon > maxLon)
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
    String countryCode;
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
            final int COUNTRY_CODE_IDX = 8;
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
                /* Keep NaN */ }
            try {
                this.longitude = Double.parseDouble(parts[LON_IDX].trim());
            } catch (NumberFormatException | NullPointerException e) {
                /* Keep NaN */ }
            String populationStr = parts[POPULATION_IDX].trim();
            if (populationStr.isEmpty()) {
                this.population = 0;
            } else {
                try {
                    this.population = Long.parseLong(populationStr);
                } catch (NumberFormatException nfe) {
                    this.population = 0;
                    /* Log */ }
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
    int geonameId; // 0 for World/Continent, negative for artificial
    String name;
    String featureCode; // Original feature code (PCLI, ADM1, CONT, ARTIFICIAL)
    String code; // admin code, country code, continent code, or inherited code
    NodeType type;
    long aggregatedPopulation;
    BoundingBox bounds;
    List<TreeNode> children = new ArrayList<>();

    // *** UPDATED ENUM with S_ADM3 and S_ADM4 ***
    enum NodeType {
        WORLD, CONTINENT, COUNTRY, ADM1, ADM2, S_ADM3, S_ADM4, PPL
    }

    // Main constructor for ALL node types
    TreeNode(int geonameId, String name, NodeType type, String code, String featureCode) {
        this.geonameId = geonameId;
        this.name = name;
        this.type = type;
        this.code = (code != null ? code : "");
        this.featureCode = (featureCode != null ? featureCode : "");
        this.aggregatedPopulation = 0; // Initialize pop to 0
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
        return name + " (" + type + (code != null && !code.isEmpty() ? ", code=" + code : "") + ", id=" + geonameId
                + ", pop=" + aggregatedPopulation + boundsStr
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

    // Resets population before final aggregation
    void resetAggregatedPopulation() {
        this.aggregatedPopulation = 0; // Reset to 0
        for (TreeNode child : children) {
            child.resetAggregatedPopulation();
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
    String code;
    String featureCode;
    String countryCode;
    String admin1Code;

    RelevantAdminInfo(GeonameEntry entry, TreeNode.NodeType type) {
        this.geonameId = entry.geonameId;
        this.name = entry.name;
        this.type = type;
        this.featureCode = entry.featureCode;
        this.countryCode = entry.countryCode;
        this.admin1Code = entry.admin1Code;
        switch (type) {
            case COUNTRY:
                this.code = entry.countryCode;
                break;
            case ADM1:
                this.code = entry.admin1Code;
                break;
            case ADM2:
                this.code = entry.admin2Code;
                break;
            default:
                this.code = "";
        }
    }
}

/**
 * Main class to build dynamically deep hierarchy from files.
 */
public class FileBasedDynamicBuilder { // Keep class name as user provided

    // --- Maps loaded from smaller files ---
    Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    Map<String, String> countryToContinentMap = new HashMap<>();
    Map<String, Integer> countryCodeToIdMap = new HashMap<>();

    // --- Data collected from Pass 1 ---
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

    // --- Loading methods ---
    void loadAdminCodes(String filePath, Map<String, Integer> map, String adminLevelName) {
        int count = 0;
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
            e.printStackTrace();
        }
        System.out.println("Loaded " + count + " " + adminLevelName + " code mappings from " + filePath);
    }

    void loadCountryInfo(String filePath) {
        final int ISO_CODE_IDX = 0;
        final int CONTINENT_CODE_IDX = 8;
        final int MIN_COUNTRY_PARTS = 9;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1);
                if (parts.length >= MIN_COUNTRY_PARTS) {
                    String isoCode = parts[ISO_CODE_IDX].trim();
                    String continentCode = parts[CONTINENT_CODE_IDX].trim();
                    if (!isoCode.isEmpty() && !continentCode.isEmpty()) {
                        countryToContinentMap.put(isoCode, continentCode);
                    }
                } else {
                    /* Warning */ }
            }
        } catch (IOException e) {
            System.err.println("Error reading country info file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded " + countryToContinentMap.size() + " country->continent mappings from " + filePath);
    }

    // --- Pass 1: Process allCountries.txt ---
    void processAllCountriesPass1(String filePath) {
        System.out.println(
                "Starting Pass 1: Identifying PCLI/ADM1/ADM2, aggregating PPL population & bounds by ADM1 & ADM2...");
        int lineCount = 0, pplCount = 0, pcliCount = 0, adm1Count = 0, adm2Count = 0;
        long totalPplPop = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 19) {
                    continue;
                }
                GeonameEntry entry = new GeonameEntry(parts);
                if (entry.geonameId == -1 || entry.countryCode == null)
                    continue;
                TreeNode.NodeType type = determineNodeType(entry.featureClass, entry.featureCode);
                if (type == null)
                    continue;
                if (type == TreeNode.NodeType.COUNTRY) {
                    RelevantAdminInfo info = new RelevantAdminInfo(entry, type);
                    relevantAdminMap.put(entry.geonameId, info);
                    countryCodeToIdMap.put(entry.countryCode, entry.geonameId);
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
                    if (entry.admin1Code != null && !entry.admin1Code.isEmpty()) {
                        String adm1Key = entry.countryCode + "." + entry.admin1Code;
                        parentAdm1Id = admin1CodeToIdMap.get(adm1Key);
                    }
                    if (entry.admin2Code != null && !entry.admin2Code.isEmpty() && parentAdm1Id != null) {
                        String adm2Key = entry.countryCode + "." + entry.admin1Code + "." + entry.admin2Code;
                        parentAdm2Id = admin2CodeToIdMap.get(adm2Key);
                    }
                    if (entry.population > 0) {
                        if (parentAdm1Id != null) {
                            adm1PopulationMap.put(parentAdm1Id,
                                    adm1PopulationMap.getOrDefault(parentAdm1Id, 0L) + entry.population);
                        }
                        if (parentAdm2Id != null) {
                            adm2PopulationMap.put(parentAdm2Id,
                                    adm2PopulationMap.getOrDefault(parentAdm2Id, 0L) + entry.population);
                        }
                        pplCount++;
                        totalPplPop += entry.population;
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
            e.printStackTrace();
        }
        System.out
                .println("Pass 1 Complete. Found PCLI: " + pcliCount + ", ADM1: " + adm1Count + ", ADM2: " + adm2Count);
        System.out.println("Aggregated pop from " + pplCount + " PPLs (" + totalPplPop + ") into "
                + adm1PopulationMap.size() + " ADM1 & " + adm2PopulationMap.size() + " ADM2 regions.");
    }

    private TreeNode.NodeType determineNodeType(String fcl, String fcode) {
        if ("PCLI".equals(fcode))
            return TreeNode.NodeType.COUNTRY;
        if ("ADM1".equals(fcode))
            return TreeNode.NodeType.ADM1;
        if ("ADM2".equals(fcode))
            return TreeNode.NodeType.ADM2;
        if ("P".equals(fcl) && fcode != null && fcode.startsWith("PPL"))
            return TreeNode.NodeType.PPL;
        return null;
    }

    // --- Pass 2: Build Initial Hierarchy (World -> Continent -> Country -> ADM1)
    // ---
    TreeNode buildInitialAdm1Hierarchy() {
        System.out.println("Starting Pass 2: Building initial hierarchy (World -> Continent -> Country -> ADM1)...");
        TreeNode worldRoot = new TreeNode("World", TreeNode.NodeType.WORLD, "WORLD");
        Map<String, TreeNode> continentNodes = new HashMap<>();
        nodeMap.clear();
        nodeMap.put(0, worldRoot);
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            if (info.type == TreeNode.NodeType.COUNTRY || info.type == TreeNode.NodeType.ADM1) {
                TreeNode node = new TreeNode(info.geonameId, info.name, info.type, info.code, info.featureCode);
                nodeMap.put(info.geonameId, node);
            }
        }
        System.out.println("  Created " + nodeMap.size() + " initial TreeNodes (World + PCLI + ADM1).");
        int countryLinks = 0, adm1Links = 0, failedLinks = 0;
        for (RelevantAdminInfo info : relevantAdminMap.values()) {
            TreeNode childNode = nodeMap.get(info.geonameId);
            if (childNode == null)
                continue;
            TreeNode parentNode = null;
            if (childNode.type == TreeNode.NodeType.ADM1) {
                Integer parentCountryId = countryCodeToIdMap.get(info.countryCode);
                if (parentCountryId != null)
                    parentNode = nodeMap.get(parentCountryId);
                if (parentNode != null) {
                    parentNode.addChild(childNode);
                    adm1Links++;
                } else {
                    failedLinks++;
                    /* Log */ }
            } else if (childNode.type == TreeNode.NodeType.COUNTRY) {
                String continentCode = countryToContinentMap.get(info.code);
                if (continentCode != null) {
                    parentNode = continentNodes.computeIfAbsent(continentCode, k -> {
                        String continentName = k;
                        if ("EU".equals(k))
                            continentName = "Europe";
                        else if ("AS".equals(k))
                            continentName = "Asia";
                        else if ("AF".equals(k))
                            continentName = "Africa";
                        else if ("NA".equals(k))
                            continentName = "North America";
                        else if ("SA".equals(k))
                            continentName = "South America";
                        else if ("OC".equals(k))
                            continentName = "Oceania";
                        else if ("AN".equals(k))
                            continentName = "Antarctica";
                        TreeNode newNode = new TreeNode(continentName, TreeNode.NodeType.CONTINENT, k);
                        worldRoot.addChild(newNode);
                        return newNode;
                    });
                } else {
                    parentNode = worldRoot;
                    /* Log */ }
                parentNode.addChild(childNode);
                countryLinks++;
            }
        }
        System.out.println("Initial hierarchy linking complete. Country links: " + countryLinks + ", ADM1 links: "
                + adm1Links + ", Failed links: " + failedLinks);
        return worldRoot;
    }

    // --- Pass 3: Assign initial populations/bounds to ADM1 and find nodes > 1M ---
    BoundingBox assignInitialPopBoundsAndIdentifyExpansions(TreeNode node, List<TreeNode> nodesToExpand) {
        BoundingBox nodeBounds = new BoundingBox();
        long totalPop = 0;
        if (node.type == TreeNode.NodeType.ADM1) {
            node.aggregatedPopulation = adm1PopulationMap.getOrDefault(node.geonameId, 0L);
            node.bounds = adm1BoundsMap.get(node.geonameId);
            if (node.aggregatedPopulation > POPULATION_THRESHOLD_1M) {
                nodesToExpand.add(node);
            }
            return node.bounds != null ? node.bounds : nodeBounds;
        }
        if (node.children != null && !node.children.isEmpty()) {
            for (TreeNode child : node.children) {
                BoundingBox childBounds = assignInitialPopBoundsAndIdentifyExpansions(child, nodesToExpand);
                totalPop += child.aggregatedPopulation;
                nodeBounds.extend(childBounds);
            }
        }
        node.aggregatedPopulation = totalPop;
        node.bounds = nodeBounds;
        return nodeBounds;
    }

    // --- Pass 4: Add ADM2 Layer for specific ADM1 nodes ---
    void addAdm2Layer(List<TreeNode> nodesToExpand) {
        System.out.println("Starting Pass 4: Adding ADM2 layer for " + nodesToExpand.size() + " ADM1 nodes...");
        int adm2Added = 0;
        if (relevantAdminMap == null || nodeMap == null || adm2PopulationMap == null || adm2BoundsMap == null) {
            System.err.println("Error: Maps not initialized.");
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
                    }
                }
            }
        }
        System.out.println("Pass 4 Complete. Added " + adm2Added + " ADM2 nodes.");
    }

    // --- Pass 5: Distribute Remaining ADM1 Population ONLY to Zero-Pop ADM2
    // Children ---
    void distributeAdm1Population(List<TreeNode> expandedAdm1Nodes) {
        System.out.println("Starting Pass 5: Distributing remaining ADM1 population to zero-pop ADM2 children...");
        int adm2PopDistributed = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            long totalAdm1Pop = adm1PopulationMap.getOrDefault(adm1Node.geonameId, 0L);
            if (totalAdm1Pop <= 0) {
                continue;
            }
            List<TreeNode> zeroPopAdm2Children = new ArrayList<>();
            long sumNonZeroAdm2Pop = 0;
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    if (child.aggregatedPopulation > 0) {
                        sumNonZeroAdm2Pop += child.aggregatedPopulation;
                    } else {
                        zeroPopAdm2Children.add(child);
                    }
                }
            }
            int numZeroPopAdm2 = zeroPopAdm2Children.size();
            if (numZeroPopAdm2 > 0) {
                long remainingAdm1Pop = totalAdm1Pop - sumNonZeroAdm2Pop;
                if (remainingAdm1Pop < 0) {
                    /* Warn & skip */ remainingAdm1Pop = 0;
                }
                if (remainingAdm1Pop > 0) {
                    long share = remainingAdm1Pop / numZeroPopAdm2;
                    long remainder = remainingAdm1Pop % numZeroPopAdm2;
                    for (int i = 0; i < numZeroPopAdm2; i++) {
                        TreeNode adm2Child = zeroPopAdm2Children.get(i);
                        long assignedPop = share + (i < remainder ? 1 : 0);
                        adm2Child.aggregatedPopulation = assignedPop;
                        adm2PopDistributed++;
                    }
                }
            }
        }
        System.out.println("Pass 5 complete. Assigned estimated population to " + adm2PopDistributed
                + " previously zero-pop ADM2 nodes.");
    }

    // --- Pass 6: Estimate Missing ADM2 Bounding Boxes ---
    void estimateMissingAdm2Bounds(List<TreeNode> expandedAdm1Nodes) {
        System.out.println("Starting Pass 6: Estimating missing bounds for ADM2 children (respecting existing)...");
        int boundsEstimated = 0;
        for (TreeNode adm1Node : expandedAdm1Nodes) {
            if (adm1Node.bounds == null || !adm1Node.bounds.isValid()) {
                continue;
            }
            List<TreeNode> allAdm2Children = new ArrayList<>();
            for (TreeNode child : adm1Node.children) {
                if (child.type == TreeNode.NodeType.ADM2) {
                    allAdm2Children.add(child);
                }
            }
            int totalAdm2Children = allAdm2Children.size();
            if (totalAdm2Children == 0) {
                continue;
            }
            int gridCols = (int) Math.ceil(Math.sqrt(totalAdm2Children));
            int gridRows = (int) Math.ceil((double) totalAdm2Children / gridCols);
            double totalLatSpan = adm1Node.bounds.maxLat - adm1Node.bounds.minLat;
            double totalLonSpan = adm1Node.bounds.maxLon - adm1Node.bounds.minLon;
            double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
            double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;
            boolean estimatedAny = false;
            allAdm2Children.sort(Comparator.comparing(a -> a.name));
            for (int i = 0; i < totalAdm2Children; i++) {
                TreeNode adm2ChildNode = allAdm2Children.get(i);
                if (adm2ChildNode.bounds != null && adm2ChildNode.bounds.isValid()) {
                    continue;
                }
                if (!estimatedAny) {
                    /* Log only once */ estimatedAny = true;
                }
                int r = i / gridCols;
                int c = i % gridCols;
                BoundingBox estimatedBounds = new BoundingBox();
                if (cellWidth > 0 || cellHeight > 0) {
                    estimatedBounds.minLat = adm1Node.bounds.minLat + r * cellHeight;
                    estimatedBounds.minLon = adm1Node.bounds.minLon + c * cellWidth;
                    estimatedBounds.maxLat = adm1Node.bounds.minLat + (r + 1) * cellHeight;
                    estimatedBounds.maxLon = adm1Node.bounds.minLon + (c + 1) * cellWidth;
                    if (estimatedBounds.maxLat < estimatedBounds.minLat)
                        estimatedBounds.maxLat = estimatedBounds.minLat;
                    if (estimatedBounds.maxLon < estimatedBounds.minLon)
                        estimatedBounds.maxLon = estimatedBounds.minLon;
                    estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, adm1Node.bounds.maxLat);
                    estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, adm1Node.bounds.maxLon);
                    estimatedBounds.minLat = Math.max(estimatedBounds.minLat, adm1Node.bounds.minLat);
                    estimatedBounds.minLon = Math.max(estimatedBounds.minLon, adm1Node.bounds.minLon);
                } else {
                    estimatedBounds.extend(adm1Node.bounds);
                }
                adm2ChildNode.bounds = estimatedBounds.isValid() ? estimatedBounds : adm1Node.bounds;
                boundsEstimated++;
            }
        }
        System.out.println(
                "Pass 6 complete. Estimated bounds for " + boundsEstimated + " ADM2 nodes that were missing them.");
    }

    // --- Expansion Logic (Reusable for different thresholds) ---
    boolean expandLeafNodeIfNeeded(TreeNode leafNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType) {
        if (leafNode == null || !leafNode.children.isEmpty() || leafNode.aggregatedPopulation <= threshold) {
            return false;
        }
        if (leafNode.bounds == null || !leafNode.bounds.isValid()) {
            /* Warning or skip */ return false;
        }
        long parentPop = leafNode.aggregatedPopulation;
        int numChildren;
        if (distributeEqually) {
            numChildren = (int) Math.ceil((double) parentPop / threshold);
        } else {
            numChildren = (int) (parentPop / threshold);
            if (parentPop % threshold > 0)
                numChildren++;
        }
        if (numChildren <= 1)
            return false;
        int gridCols = (int) Math.ceil(Math.sqrt(numChildren));
        int gridRows = (int) Math.ceil((double) numChildren / gridCols);
        double totalLatSpan = leafNode.bounds.maxLat - leafNode.bounds.minLat;
        double totalLonSpan = leafNode.bounds.maxLon - leafNode.bounds.minLon;
        double cellHeight = (gridRows > 0 && totalLatSpan > 1e-9) ? totalLatSpan / gridRows : 0;
        double cellWidth = (gridCols > 0 && totalLonSpan > 1e-9) ? totalLonSpan / gridCols : 0;
        long remainingPop = parentPop;
        long popSumCheck = 0;
        for (int i = 0; i < numChildren; i++) {
            String childName = leafNode.name + " part " + (i + 1);
            int tempChildId = -Math.abs((leafNode.geonameId + "_" + childName).hashCode());
            TreeNode childNode = new TreeNode(tempChildId, childName, artificialChildType, leafNode.code, "ARTIFICIAL");
            if (distributeEqually) {
                long share = parentPop / numChildren;
                long remainder = parentPop % numChildren;
                childNode.aggregatedPopulation = share + (i < remainder ? 1 : 0);
            } else {
                long childPop = Math.min(remainingPop, threshold);
                childNode.aggregatedPopulation = childPop;
                remainingPop -= childPop;
            }
            popSumCheck += childNode.aggregatedPopulation;
            int r = i / gridCols;
            int c = i % gridCols;
            BoundingBox estimatedBounds = new BoundingBox();
            if (cellWidth > 0 || cellHeight > 0) {
                estimatedBounds.minLat = leafNode.bounds.minLat + r * cellHeight;
                estimatedBounds.minLon = leafNode.bounds.minLon + c * cellWidth;
                estimatedBounds.maxLat = leafNode.bounds.minLat + (r + 1) * cellHeight;
                estimatedBounds.maxLon = leafNode.bounds.minLon + (c + 1) * cellWidth;
                if (estimatedBounds.maxLat < estimatedBounds.minLat)
                    estimatedBounds.maxLat = estimatedBounds.minLat;
                if (estimatedBounds.maxLon < estimatedBounds.minLon)
                    estimatedBounds.maxLon = estimatedBounds.minLon;
                estimatedBounds.maxLat = Math.min(estimatedBounds.maxLat, leafNode.bounds.maxLat);
                estimatedBounds.maxLon = Math.min(estimatedBounds.maxLon, leafNode.bounds.maxLon);
                estimatedBounds.minLat = Math.max(estimatedBounds.minLat, leafNode.bounds.minLat);
                estimatedBounds.minLon = Math.max(estimatedBounds.minLon, leafNode.bounds.minLon);
            } else {
                estimatedBounds.extend(leafNode.bounds);
            }
            childNode.bounds = estimatedBounds.isValid() ? estimatedBounds : leafNode.bounds;
            leafNode.addChild(childNode);
        }
        if (popSumCheck != parentPop) {
            System.err.println("WARN: Pop distribution error for " + leafNode.name + ". Orig: " + parentPop + ", Dist: "
                    + popSumCheck);
        }
        return true;
    }

    // Traverses the tree and calls expandLeafNodeIfNeeded
    void findAndExpandLeaves(TreeNode startNode, long threshold, boolean distributeEqually,
            TreeNode.NodeType artificialChildType) {
        System.out.println("Finding leaves with pop > " + threshold + " starting from " + startNode.name + "...");
        List<TreeNode> leavesToExpand = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(startNode);
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
        System.out.println("  Expanded " + expandedCount + " leaves.");
    }

    // --- Final Aggregation Pass ---
    long finalAggregatePopulation(TreeNode node) {
        if (node.children == null || node.children.isEmpty()) {
            return node.aggregatedPopulation;
        }
        long totalAggregatedPopulation = 0;
        for (TreeNode child : node.children) {
            totalAggregatedPopulation += finalAggregatePopulation(child);
        }
        node.aggregatedPopulation = totalAggregatedPopulation;
        return totalAggregatedPopulation;
    }

    BoundingBox finalAggregateBounds(TreeNode node) {
        if (node.children == null || node.children.isEmpty()) {
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

    // --- Main Execution ---
    public static void main(String[] args) {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            System.err.println("Error: Could not determine user home directory.");
            return;
        }

        String geonamesFilePath = homeDir + File.separator + "allCountries.txt";
        String admin1FilePath = homeDir + File.separator + "admin1CodesASCII.txt";
        String admin2FilePath = homeDir + File.separator + "admin2Codes.txt"; // Required
        String countryInfoFilePath = homeDir + File.separator + "countryInfo.txt";
        String outputFilePath = homeDir + File.separator + "geonames_hierarchy_output.txt"; // Output file path

        if (!new File(geonamesFilePath).exists() || !new File(admin1FilePath).exists() ||
                !new File(admin2FilePath).exists() || !new File(countryInfoFilePath).exists()) {
            System.err.println("Error: One or more required GeoNames files not found in " + homeDir);
            return;
        }

        FileBasedDynamicBuilder builder = new FileBasedDynamicBuilder(); // Use current class name

        System.out.println("--- Loading Index Files ---");
        builder.loadCountryInfo(countryInfoFilePath);
        builder.loadAdminCodes(admin1FilePath, builder.admin1CodeToIdMap, "ADM1");
        builder.loadAdminCodes(admin2FilePath, builder.admin2CodeToIdMap, "ADM2"); // Load ADM2

        if (builder.countryToContinentMap.isEmpty() || builder.admin1CodeToIdMap.isEmpty()
                || builder.admin2CodeToIdMap.isEmpty()) {
            System.err.println("Failed to load essential index files. Exiting.");
            return;
        }

        System.out.println("\n--- Pass 1: Processing " + geonamesFilePath + " ---");
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

        System.out.println("\n--- Pass 3: Assigning Initial Pop/Bounds & Identifying Nodes > 1M ---");
        startTime = System.currentTimeMillis();
        List<TreeNode> nodesToExpand1M = new ArrayList<>();
        builder.assignInitialPopBoundsAndIdentifyExpansions(root, nodesToExpand1M);
        long pass3Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 3 finished in " + pass3Time + " ms. Identified " + nodesToExpand1M.size()
                + " ADM1 nodes > 1M pop to expand.");

        System.out.println("\n--- Pass 4: Adding ADM2 Layer ---");
        startTime = System.currentTimeMillis();
        builder.addAdm2Layer(nodesToExpand1M);
        long pass4Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 4 finished in " + pass4Time + " ms.");

        System.out.println("\n--- Pass 5: Distributing ADM1 Population to ADM2s ---");
        startTime = System.currentTimeMillis();
        builder.distributeAdm1Population(nodesToExpand1M);
        long pass5Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 5 finished in " + pass5Time + " ms.");

        System.out.println("\n--- Pass 6: Estimating Missing ADM2 Bounding Boxes ---");
        startTime = System.currentTimeMillis();
        builder.estimateMissingAdm2Bounds(nodesToExpand1M);
        long pass6Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 6 finished in " + pass6Time + " ms.");

        // Pass 7: Expand > 1M leaves (could be ADM1 or ADM2 now)
        System.out.println("\n--- Pass 7: Expanding Leaves with Population > 1,000,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_1M, false, TreeNode.NodeType.S_ADM3);
        long pass7Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 7 finished in " + pass7Time + " ms.");

        // Pass 8: Expand > 10K leaves (could be ADM1, ADM2, or S_ADM3)
        System.out.println("\n--- Pass 8: Expanding Leaves with Population > 10,000 ---");
        startTime = System.currentTimeMillis();
        builder.findAndExpandLeaves(root, builder.POPULATION_THRESHOLD_10K, true, TreeNode.NodeType.S_ADM4);
        long pass8Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 8 finished in " + pass8Time + " ms.");

        // Pass 9: Final Recalculation of Aggregated Data
        System.out.println("\n--- Pass 9: Final Recalculation of Aggregated Data ---");
        startTime = System.currentTimeMillis();
        builder.finalAggregatePopulation(root); // Recalculate populations first
        builder.finalAggregateBounds(root); // Then recalculate bounds
        long pass9Time = System.currentTimeMillis() - startTime;
        System.out.println("Pass 9 finished in " + pass9Time + " ms.");

        // --- Output ---
        System.out.println("\n--- Writing Final Hierarchy to File ---");
        System.out.println("Output file: " + outputFilePath);
        if (root != null) {
            // Try writing to file first
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFilePath)))) {
                 root.printTree(writer, ""); // Use the PrintWriter version
                 System.out.println("Successfully wrote hierarchy to " + outputFilePath);
            } catch (IOException e) {
                 // If file writing fails, print error and fallback to console
                 System.err.println("Error writing hierarchy to file: " + e.getMessage());
                 e.printStackTrace();
                 System.out.println("\n--- Printing Fallback to Console ---");
                 // Use a PrintWriter wrapping System.out for console output
                 try (PrintWriter consoleWriter = new PrintWriter(System.out)) {
                      root.printTree(consoleWriter, ""); // Use the SAME PrintWriter version
                      consoleWriter.flush(); // Ensure console output is flushed
                 } catch (Exception fallbackEx) {
                      // Catch potential errors during console printing
                      System.err.println("Error printing hierarchy to console fallback: " + fallbackEx.getMessage());
                      fallbackEx.printStackTrace();
                 }
            }
        } else {
            System.err.println("Root node is null, cannot print tree.");
        }
    } 
}