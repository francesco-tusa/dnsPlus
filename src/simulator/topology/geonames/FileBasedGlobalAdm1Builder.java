package simulator.topology.geonames;

import java.io.BufferedReader;
 import java.io.FileReader;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.io.File;

 // --- TreeNode Class ---
 class TreeNode {
     int geonameId; // 0 for World/Continent
     String name;
     String featureCode; // Original feature code (PCLI, ADM1, CONT)
     String code; // admin code, country code, or continent code
     NodeType type;
     long aggregatedPopulation;
     List<TreeNode> children = new ArrayList<>();

     enum NodeType { WORLD, CONTINENT, COUNTRY, ADM1 } // Simplified for this scope

     // Constructor for World/Continent/Country/ADM1
     TreeNode(int geonameId, String name, NodeType type, String code, String featureCode) {
         this.geonameId = geonameId;
         this.name = name;
         this.type = type;
         this.code = code;
         this.featureCode = featureCode;
         this.aggregatedPopulation = 0; // Will be calculated
     }
      // Constructor for Continent/World specifically
      TreeNode(String name, NodeType type, String code) {
          this(0, name, type, code, (type == TreeNode.NodeType.CONTINENT ? "CONT" : "WORLD"));
      }

     void addChild(TreeNode child) { if (child != null) children.add(child); }

     @Override
     public String toString() {
          return name + " (" + type + (code != null && !code.isEmpty() ? ", code=" + code : "") + ", id=" + geonameId + ", pop=" + aggregatedPopulation + (children.isEmpty() ? "" : ", children=" + children.size()) + ")";
     }

     // printTree method
     public void printTree(String indent) {
         System.out.println(indent + this);
         children.sort(Comparator.comparing(a -> a.name));
         for (TreeNode child : children) {
             // Stop printing below ADM1 level for this specific request's output
             if (this.type != NodeType.ADM1) {
                child.printTree(indent + "  ");
             }
         }
          // Optional: Indicate if an ADM1 has children (PPLs) even if not printing them
         // if(this.type == NodeType.ADM1 && !children.isEmpty()) {
         //     System.out.println(indent + "  " + "[...PPLs...]");
         // }
     }
 }

 // --- GeonameEntry Class (Use V4 Robust Version) ---
 class GeonameEntry {
     // --- Include the full robust GeonameEntry class definition here ---
     // (Same as the one used in FileBasedEuropeAdm1Builder)
     int geonameId; String name; String featureClass; String featureCode;
     String countryCode; String admin1Code; String admin2Code;
     long population; int elevation;
     public GeonameEntry(String[] parts) {
          try {
             final int ID_IDX = 0; final int NAME_IDX = 1; final int FEAT_CLASS_IDX = 6; final int FEAT_CODE_IDX = 7;
             final int COUNTRY_CODE_IDX = 8; final int ADMIN1_CODE_IDX = 10; final int ADMIN2_CODE_IDX = 11;
             final int POPULATION_IDX = 14; final int ELEVATION_IDX = 15; final int MIN_EXPECTED_PARTS = 19;
             if (parts.length < MIN_EXPECTED_PARTS) { this.geonameId = -1; return; } // Exit if too short
             this.geonameId = Integer.parseInt(parts[ID_IDX].trim());
             this.name = parts[NAME_IDX].trim();
             this.featureClass = parts[FEAT_CLASS_IDX].trim();
             this.featureCode = parts[FEAT_CODE_IDX].trim();
             this.countryCode = parts[COUNTRY_CODE_IDX].trim();
             this.admin1Code = parts[ADMIN1_CODE_IDX].trim();
             this.admin2Code = parts[ADMIN2_CODE_IDX].trim(); // Keep reading even if not used in hierarchy
             String populationStr = parts[POPULATION_IDX].trim();
             if (populationStr.isEmpty()) { this.population = 0; }
             else { try { this.population = Long.parseLong(populationStr); } catch (NumberFormatException nfe) { this.population = 0; /* System.err.println("Pop parse error ID " + this.geonameId); */ }}
             String elevationStr = parts[ELEVATION_IDX].trim();
             if (elevationStr.isEmpty()) { this.elevation = 0; } else { try { this.elevation = Integer.parseInt(elevationStr); } catch (NumberFormatException nfe) { this.elevation = 0; } }
         } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) { System.err.println("Critical parse error: " + String.join("|", parts) + " - " + e.getMessage()); this.geonameId = -1; }
     }
 }

 // --- Simplified Data Holder for PCLI and ADM1 features ---
 class RelevantAdminInfo {
     int geonameId;
     String name;
     TreeNode.NodeType type;
     String code; // admin1 or country code
     String featureCode;
     String countryCode; // Needed for linking ADM1 to Country

     RelevantAdminInfo(GeonameEntry entry, TreeNode.NodeType type) {
         this.geonameId = entry.geonameId;
         this.name = entry.name;
         this.type = type;
         this.featureCode = entry.featureCode;
         this.countryCode = entry.countryCode;
         this.code = (type == TreeNode.NodeType.COUNTRY) ? entry.countryCode : entry.admin1Code;
     }
 }

 // --- Main Application Logic ---
 public class FileBasedGlobalAdm1Builder { // Renamed class

     // --- Maps loaded from smaller files ---
     Map<String, Integer> admin1CodeToIdMap = new HashMap<>(); // "GB.ENG" -> geonameId of ADM1
     Map<String, String> countryToContinentMap = new HashMap<>(); // "GB" -> "EU"
     Map<String, Integer> countryCodeToIdMap = new HashMap<>(); // "GB" -> geonameId of PCLI

     // --- Data collected from Pass 1 ---
     Map<Integer, RelevantAdminInfo> relevantAdminMap = new HashMap<>(); // geonameId -> Info for PCLI, ADM1
     Map<Integer, Long> adm1PopulationMap = new HashMap<>(); // ADM1 geonameId -> Aggregated PPL Population

     // --- Final Tree structure ---
     Map<Integer, TreeNode> nodeMap = new HashMap<>(); // geonameid -> TreeNode (only Adm1 and above)

     // --- Loading methods for smaller index files ---
     void loadAdmin1Codes(String filePath) {
         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) { String line; while ((line = reader.readLine()) != null) { if (line.startsWith("#") || line.trim().isEmpty()) continue; String[] parts = line.split("\t"); if (parts.length >= 4) { String key = parts[0].trim(); try { int geonameId = Integer.parseInt(parts[3].trim()); admin1CodeToIdMap.put(key, geonameId); } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) { System.err.println("Skipping invalid admin1 line: " + line + " - " + e.getMessage()); }}}} catch (IOException e) { e.printStackTrace(); } System.out.println("Loaded " + admin1CodeToIdMap.size() + " admin1 code mappings from " + filePath); }

     void loadCountryInfo(String filePath) {
          final int ISO_CODE_IDX = 0; final int CONTINENT_CODE_IDX = 8; final int MIN_COUNTRY_PARTS = 9; try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) { String line; while ((line = reader.readLine()) != null) { if (line.startsWith("#") || line.trim().isEmpty()) continue; String[] parts = line.split("\t", -1); if (parts.length >= MIN_COUNTRY_PARTS) { String isoCode = parts[ISO_CODE_IDX].trim(); String continentCode = parts[CONTINENT_CODE_IDX].trim(); if (!isoCode.isEmpty() && !continentCode.isEmpty()) { countryToContinentMap.put(isoCode, continentCode); }} else { System.err.println("Warning: Skipping countryInfo line: " + line); }}} catch (IOException e) { e.printStackTrace(); } System.out.println("Loaded " + countryToContinentMap.size() + " country->continent mappings from " + filePath); }


     // --- Pass 1: Process allCountries.txt ---
     void processAllCountriesPass1(String filePath) {
         System.out.println("Starting Pass 1: Identifying PCLI/ADM1 features and aggregating PPL population by ADM1...");
         int lineCount = 0;
         int pplCount = 0;
         long totalPplPop = 0;
         int pcliCount = 0;
         int adm1Count = 0;

         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
             String line;
             while ((line = reader.readLine()) != null) {
                 lineCount++;
                 if (line.startsWith("#") || line.trim().isEmpty()) continue;
                 String[] parts = line.split("\t", -1);
                 final int MIN_EXPECTED_PARTS = 19;
                 if (parts.length < MIN_EXPECTED_PARTS) { continue; }

                 GeonameEntry entry = new GeonameEntry(parts);
                 if (entry.geonameId == -1 || entry.countryCode == null) continue; // Skip invalid entries

                 // Process PCLI (Country) Features
                 if ("PCLI".equals(entry.featureCode)) {
                     RelevantAdminInfo info = new RelevantAdminInfo(entry, TreeNode.NodeType.COUNTRY);
                     relevantAdminMap.put(entry.geonameId, info);
                     countryCodeToIdMap.put(entry.countryCode, entry.geonameId); // Store mapping
                     pcliCount++;
                 }
                 // Process ADM1 Features
                 else if ("ADM1".equals(entry.featureCode)) {
                      RelevantAdminInfo info = new RelevantAdminInfo(entry, TreeNode.NodeType.ADM1);
                      relevantAdminMap.put(entry.geonameId, info);
                      adm1Count++;
                 }
                 // Process PPL Features for Population Aggregation
                 else if ("P".equals(entry.featureClass) && entry.featureCode != null && entry.featureCode.startsWith("PPL")) {
                      if (entry.population > 0 && entry.admin1Code != null && !entry.admin1Code.isEmpty()) {
                           // Find the geonameid of the parent ADM1
                           String adm1Key = entry.countryCode + "." + entry.admin1Code;
                           Integer parentAdm1Id = admin1CodeToIdMap.get(adm1Key);

                           if (parentAdm1Id != null) {
                               adm1PopulationMap.put(parentAdm1Id, adm1PopulationMap.getOrDefault(parentAdm1Id, 0L) + entry.population);
                               pplCount++;
                               totalPplPop += entry.population;
                           } else {
                              // Optional: Track population for PPLs whose ADM1 ID couldn't be found
                              // Could aggregate at country level as fallback?
                           }
                      }
                 }

                 if (lineCount % 1000000 == 0) { // Progress indicator for large file
                     System.out.println("  Processed " + lineCount + " lines... PCLI: " + pcliCount + ", ADM1: " + adm1Count + ", PPLs aggregated: " + pplCount);
                 }
             }
         } catch (IOException e) {
             System.err.println("Error reading geonames file in Pass 1: " + filePath);
             e.printStackTrace();
         }
         System.out.println("Pass 1 Complete. Found " + pcliCount + " PCLI, " + adm1Count + " ADM1 features.");
         System.out.println("Aggregated population from " + pplCount + " PPLs: " + totalPplPop + " into " + adm1PopulationMap.size() + " ADM1 regions.");
         System.out.println("Mapped " + countryCodeToIdMap.size() + " country codes to Ids.");
     }

     // --- Pass 2: Build Hierarchy (World -> Continent -> Country -> ADM1) ---
     TreeNode buildGlobalAdm1Hierarchy() {
         System.out.println("Starting Pass 2: Building hierarchy (World -> Continent -> Country -> ADM1)...");
         TreeNode worldRoot = new TreeNode("World", TreeNode.NodeType.WORLD, "WORLD");
         Map<String, TreeNode> continentNodes = new HashMap<>();
         nodeMap.clear(); // Clear previous map if any
         nodeMap.put(0, worldRoot);

         // Create all Country and ADM1 nodes from the collected relevant info
         for (RelevantAdminInfo info : relevantAdminMap.values()) {
              TreeNode node = new TreeNode(info.geonameId, info.name, info.type, info.code, info.featureCode);
              nodeMap.put(info.geonameId, node);
         }
          System.out.println("  Created " + nodeMap.size() + " TreeNodes (PCLI/ADM1).");

         // Link nodes
         int countryLinks = 0;
         int adm1Links = 0;
         int failedLinks = 0;
         for (RelevantAdminInfo info : relevantAdminMap.values()) {
              TreeNode childNode = nodeMap.get(info.geonameId);
              if (childNode == null) continue; // Should not happen

             TreeNode parentNode = null;

             if (childNode.type == TreeNode.NodeType.ADM1) { // Link ADM1 to Country
                  Integer parentCountryId = countryCodeToIdMap.get(info.countryCode);
                  if (parentCountryId != null) {
                      parentNode = nodeMap.get(parentCountryId);
                  }
             } else if (childNode.type == TreeNode.NodeType.COUNTRY) { // Link Country to Continent
                 String continentCode = countryToContinentMap.get(info.code); // info.code is country code here
                 if (continentCode != null) {
                     parentNode = continentNodes.get(continentCode);
                     if (parentNode == null) { // Create continent node if it doesn't exist
                          String continentName = continentCode; // Simple name mapping
                          if ("EU".equals(continentCode)) continentName = "Europe";
                          else if ("AS".equals(continentCode)) continentName = "Asia";
                          else if ("AF".equals(continentCode)) continentName = "Africa";
                          else if ("NA".equals(continentCode)) continentName = "North America";
                          else if ("SA".equals(continentCode)) continentName = "South America";
                          else if ("OC".equals(continentCode)) continentName = "Oceania";
                          else if ("AN".equals(continentCode)) continentName = "Antarctica";
                          parentNode = new TreeNode(continentName, TreeNode.NodeType.CONTINENT, continentCode);
                          continentNodes.put(continentCode, parentNode);
                          worldRoot.addChild(parentNode);
                     }
                 } else {
                      parentNode = worldRoot; // Link to world if continent unknown
                      System.err.println("Warning: No continent found for country: " + childNode.name + " (Code: "+info.code+"). Linking to World.");
                 }
             }

             // Add child to parent
             if (parentNode != null) {
                  if (parentNode.geonameId != childNode.geonameId) {
                       parentNode.addChild(childNode);
                       if(childNode.type == TreeNode.NodeType.ADM1) adm1Links++;
                       else if(childNode.type == TreeNode.NodeType.COUNTRY) countryLinks++;
                  }
             } else {
                   if (childNode.type == TreeNode.NodeType.ADM1){
                       System.err.println("Warning: Could not link parent for ADM1 node: " + childNode.name + " (ID: " + childNode.geonameId + ")");
                       failedLinks++;
                   }
                   // Don't warn for unlinked countries if continent code was missing
             }
         }
         System.out.println("Hierarchy linking complete. Country links: " + countryLinks + ", ADM1 links: " + adm1Links + ", Failed links: " + failedLinks);
         return worldRoot;
     }

     // --- Pass 3: Assign aggregated populations and aggregate up ---
     void assignAndAggregatePopulation(TreeNode node) {
         long calculatedPop = 0;
         if (node.type == TreeNode.NodeType.ADM1) {
             // Assign pre-aggregated population from Pass 1
             calculatedPop = adm1PopulationMap.getOrDefault(node.geonameId, 0L);
         } else if (!node.children.isEmpty()) {
             // Recursively call for children FIRST, then sum their results
             for (TreeNode child : node.children) {
                 assignAndAggregatePopulation(child);
                 calculatedPop += child.aggregatedPopulation;
             }
         }
         node.aggregatedPopulation = calculatedPop;
     }


     // --- Main Execution ---
     public static void main(String[] args) {
         String homeDir = System.getProperty("user.home");
         if (homeDir == null) { System.err.println("Error: Could not determine user home directory."); return; }

         // Paths to REAL, FULL GeoNames files in home directory
         String geonamesFilePath = homeDir + File.separator + "allCountries.txt";
         String admin1FilePath = homeDir + File.separator + "admin1CodesASCII.txt";
         String countryInfoFilePath = homeDir + File.separator + "countryInfo.txt";

         // Basic file existence check
         if (!new File(geonamesFilePath).exists() || !new File(admin1FilePath).exists() || !new File(countryInfoFilePath).exists()) {
              System.err.println("Error: One or more required GeoNames files not found in " + homeDir);
              System.err.println("Please download 'allCountries.zip', 'admin1CodesASCII.txt', 'countryInfo.txt' from download.geonames.org/export/dump/ and unzip.");
              return;
         }

         FileBasedGlobalAdm1Builder builder = new FileBasedGlobalAdm1Builder();

         System.out.println("--- Loading Index Files ---");
         builder.loadCountryInfo(countryInfoFilePath); // Load country/continent map
         builder.loadAdmin1Codes(admin1FilePath);     // Load ADM1 code map

         if (builder.countryToContinentMap.isEmpty() || builder.admin1CodeToIdMap.isEmpty()) {
              System.err.println("Failed to load essential index files. Exiting.");
              return;
         }

         System.out.println("\n--- Pass 1: Processing " + geonamesFilePath + " ---");
         long startTime = System.currentTimeMillis();
         builder.processAllCountriesPass1(geonamesFilePath);
         long pass1Time = System.currentTimeMillis() - startTime;
         System.out.println("Pass 1 finished in " + (pass1Time / 1000.0) + " seconds.");

         if (builder.relevantAdminMap.isEmpty()) {
              System.err.println("No relevant PCLI/ADM1 features found or loaded. Exiting.");
              return;
         }

         System.out.println("\n--- Pass 2: Building In-Memory Hierarchy ---");
         startTime = System.currentTimeMillis();
         TreeNode root = builder.buildGlobalAdm1Hierarchy();
         long pass2Time = System.currentTimeMillis() - startTime;
         System.out.println("Pass 2 finished in " + pass2Time + " ms.");

         if (root == null || root.children.isEmpty()) {
             System.err.println("Hierarchy building failed or resulted in empty structure. Exiting.");
             return;
         }

         System.out.println("\n--- Pass 3: Assigning and Aggregating Population ---");
         startTime = System.currentTimeMillis();
         builder.assignAndAggregatePopulation(root); // Aggregate starting from root
         long pass3Time = System.currentTimeMillis() - startTime;
         System.out.println("Pass 3 finished in " + pass3Time + " ms.");


         System.out.println("\n--- Final Populated Hierarchy (World to ADM1) ---");
         // Print the whole tree down to ADM1 level
         root.printTree("");
     }
 }
