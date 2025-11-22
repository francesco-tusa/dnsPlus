package simulator.topology.geonames.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import simulator.topology.TopologyPaths;
import utils.CustomLogger;

public class GeoNamesDataLoader {
    private static final Logger logger = CustomLogger.getLogger(GeoNamesDataLoader.class.getName());
    
    public final Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    public final Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    public final Map<String, String> countryToContinentMap = new HashMap<>();
    public final Map<String, Integer> countryCodeToIdMap = new HashMap<>();
    public final Map<String, String> countryCodeToNameMap = new HashMap<>();
    public final Map<String, Long> countryCodeToPopulationMap = new HashMap<>();
    public final Map<String, Double> countryIsoToPenetrationMap = new HashMap<>();
    
    private final String GEONAMES_BASE_URL = "https://download.geonames.org/export/dump/";

    public void loadAll() {
        ensureDataFilesExist();
        
        logger.info("--- Loading Data Files ---");
        loadCountryInfo(TopologyPaths.COUNTRY_INFO_FILE);
        loadAdminCodes(TopologyPaths.ADMIN1_CODES_FILE, admin1CodeToIdMap);
        loadAdminCodes(TopologyPaths.ADMIN2_CODES_FILE, admin2CodeToIdMap);
        loadInternetPenetration(TopologyPaths.INTERNET_PENETRATION_FILE);
        
        logger.info("--- Data Loading Summary ---");
        logger.info("Countries Loaded: " + countryCodeToIdMap.size());
        logger.info("Official Populations Loaded: " + countryCodeToPopulationMap.size());
        logger.info("Internet Rates Loaded: " + countryIsoToPenetrationMap.size());
    }

    /**
     * Provides a BufferedReader for the raw allCountries.txt file.
     * Useful for strategies that perform their own line-by-line parsing (e.g., Political).
     */
    public BufferedReader getRawDataReader() throws IOException {
        return new BufferedReader(new FileReader(TopologyPaths.ALL_COUNTRIES_FILE));
    }

    /**
     * Loads all Populated Places (PPL) into a list.
     * Useful for strategies that require the full dataset in memory (e.g., R-Tree).
     */
    public List<GeoNamesEntry> loadAllPopulatedPlaces() {
        List<GeoNamesEntry> list = new ArrayList<>();
        logger.info("Loading all Populated Places from: " + TopologyPaths.ALL_COUNTRIES_FILE);
        
        try (BufferedReader reader = getRawDataReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 15) continue; 
                
                String fcl = parts[6];
                if (!"P".equals(fcl)) continue; // Only PPL
                
                GeoNamesEntry entry = new GeoNamesEntry(parts);
                if (entry.geonameId != -1 && entry.population > 0) {
                    list.add(entry);
                }
            }
        } catch (Exception e) {
            logger.severe("Error loading PPLs: " + e.getMessage());
        }
        return list;
    }

    private void loadCountryInfo(String filePath) {
        logger.info("Loading country info from " + filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 17) continue;
                
                String iso = parts[0].trim();
                if (!iso.isEmpty()) {
                    countryToContinentMap.put(iso, parts[8].trim());
                    countryCodeToNameMap.put(iso, parts[4].trim());
                    try {
                        long pop = Long.parseLong(parts[7].trim());
                        countryCodeToPopulationMap.put(iso, pop);
                    } catch (NumberFormatException e) { }

                    try { 
                        countryCodeToIdMap.put(iso, Integer.parseInt(parts[16].trim())); 
                    } catch(Exception e){}
                }
            }
        } catch (Exception e) { logger.severe("Error loading country info: " + e.getMessage()); }
    }

    private void loadAdminCodes(String filePath, Map<String, Integer> map) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    try {
                        map.put(parts[0].trim(), Integer.parseInt(parts[3].trim()));
                    } catch (NumberFormatException e) { }
                }
            }
        } catch (Exception e) { logger.warning("Error loading admin codes: " + e.getMessage()); }
    }

    private void loadInternetPenetration(String filePath) {
        logger.info("Loading Internet Penetration data from " + filePath + "...");
        countryIsoToPenetrationMap.clear();
        int validRatesLoaded = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); 
            if (line == null) return;
            
            String[] headers = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            List<Integer> yearIndices = new ArrayList<>();
            int isoCodeIndex = -1;
            
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].replace("\"", "").trim();
                if ("ISO2 Code".equalsIgnoreCase(header) || "Country Code".equalsIgnoreCase(header)) {
                    isoCodeIndex = i;
                } else if (header.matches(".*\\d{4}.*")) {
                    yearIndices.add(i);
                }
            }
            
            if (isoCodeIndex == -1) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length > isoCodeIndex) {
                    String isoCode = parts[isoCodeIndex].replace("\"", "").trim();
                    if (isoCode.isEmpty()) continue;
                    
                    double latestRate = -1.0;
                    for (int i = yearIndices.size() - 1; i >= 0; i--) {
                        int colIndex = yearIndices.get(i);
                        if (parts.length > colIndex) {
                            String rateStr = parts[colIndex].replace("\"", "").trim();
                            if (!rateStr.isEmpty() && !rateStr.equals("..")) {
                                try {
                                    latestRate = Double.parseDouble(rateStr) / 100.0;
                                    break;
                                } catch (NumberFormatException e) { }
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
            logger.severe("Error reading internet penetration file: " + e.getMessage());
        }
    }

    private boolean ensureDataFilesExist() {
        File resourcesDir = new File(TopologyPaths.RESOURCES_DIR);
        if (!resourcesDir.exists()) resourcesDir.mkdirs();

        String[][] requiredFiles = {
            { "allCountries.txt", GEONAMES_BASE_URL + "allCountries.zip", "true", "allCountries.txt" },
            { "admin1CodesASCII.txt", GEONAMES_BASE_URL + "admin1CodesASCII.txt", "false", null },
            { "admin2Codes.txt", GEONAMES_BASE_URL + "admin2Codes.txt", "false", null },
            { "countryInfo.txt", GEONAMES_BASE_URL + "countryInfo.txt", "false", null }
        };

        for (String[] fileInfo : requiredFiles) {
            Path dest = Paths.get(TopologyPaths.RESOURCES_DIR, fileInfo[0]);
            if (!Files.exists(dest)) {
                logger.info("Downloading " + fileInfo[0] + "...");
                downloadAndExtract(fileInfo[1], dest, Boolean.parseBoolean(fileInfo[2]), fileInfo[3]);
            }
        }
        return true;
    }

    private void downloadAndExtract(String urlStr, Path dest, boolean unzip, String zipEntryName) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            if (unzip) {
                Path tempZip = Files.createTempFile("geo", ".zip");
                Files.copy(in, tempZip, StandardCopyOption.REPLACE_EXISTING);
                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZip))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().equals(zipEntryName)) {
                            Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                            break;
                        }
                    }
                }
                Files.delete(tempZip);
            } else {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) { logger.severe("Download failed: " + urlStr); }
    }
}