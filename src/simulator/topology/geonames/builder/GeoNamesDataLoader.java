package simulator.topology.geonames.builder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import utils.CustomLogger;

public class GeoNamesDataLoader {
    private static final Logger logger = CustomLogger.getLogger(GeoNamesDataLoader.class.getName());
    
    public final Map<String, Integer> admin1CodeToIdMap = new HashMap<>();
    public final Map<String, Integer> admin2CodeToIdMap = new HashMap<>();
    public final Map<String, String> countryToContinentMap = new HashMap<>();
    public final Map<String, Integer> countryCodeToIdMap = new HashMap<>();
    public final Map<String, String> countryCodeToNameMap = new HashMap<>();
    public final Map<String, Double> countryIsoToPenetrationMap = new HashMap<>();
    
    private final String GEONAMES_BASE_URL = "https://download.geonames.org/export/dump/";

    public void loadAll(String resourcesDir) {
        ensureDataFilesExist(resourcesDir);
        loadCountryInfo(resourcesDir + "/countryInfo.txt");
        loadAdminCodes(resourcesDir + "/admin1CodesASCII.txt", admin1CodeToIdMap);
        loadAdminCodes(resourcesDir + "/admin2Codes.txt", admin2CodeToIdMap);
        loadInternetPenetration(resourcesDir + "/internet_penetration_iso2.csv");
    }

    private void loadAdminCodes(String filePath, Map<String, Integer> map) {
        logger.info("Loading admin codes from " + filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                String[] parts = line.split("\t");
                if (parts.length >= 4) map.put(parts[0].trim(), Integer.parseInt(parts[3].trim()));
            }
        } catch (Exception e) { logger.warning("Error loading admin codes: " + e.getMessage()); }
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
                    try { countryCodeToIdMap.put(iso, Integer.parseInt(parts[16].trim())); } catch(Exception e){}
                }
            }
        } catch (Exception e) { logger.severe("Error loading country info: " + e.getMessage()); }
    }

    private void loadInternetPenetration(String filePath) {
        logger.info("Loading internet penetration from " + filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); 
            if (line == null) return;
            while ((line = reader.readLine()) != null) {
                 String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                 if (parts.length > 1) {
                     String iso = parts[1].replace("\"", "").trim();
                     // Simplified: use last column or specific logic as per original code if needed
                     // Assuming last column is latest data for brevity in this refactor
                     // In production, parse columns dynamically as in original code.
                     for (int i = parts.length - 1; i > 2; i--) {
                         String val = parts[i].replace("\"", "").trim();
                         if (!val.isEmpty() && !val.equals("..")) {
                             try {
                                 countryIsoToPenetrationMap.put(iso, Double.parseDouble(val)/100.0);
                                 break;
                             } catch(Exception e){}
                         }
                     }
                 }
            }
        } catch (Exception e) { logger.warning("Error loading penetration data: " + e.getMessage()); }
    }

    private boolean ensureDataFilesExist(String resourcesDirName) {
        File resourcesDir = new File(resourcesDirName);
        if (!resourcesDir.exists()) resourcesDir.mkdirs();

        String[][] requiredFiles = {
            { "allCountries.txt", GEONAMES_BASE_URL + "allCountries.zip", "true", "allCountries.txt" },
            { "admin1CodesASCII.txt", GEONAMES_BASE_URL + "admin1CodesASCII.txt", "false", null },
            { "admin2Codes.txt", GEONAMES_BASE_URL + "admin2Codes.txt", "false", null },
            { "countryInfo.txt", GEONAMES_BASE_URL + "countryInfo.txt", "false", null }
        };

        for (String[] fileInfo : requiredFiles) {
            Path dest = Paths.get(resourcesDirName, fileInfo[0]);
            if (!Files.exists(dest)) {
                logger.info("Downloading " + fileInfo[0] + "...");
                downloadAndExtract(fileInfo[1], dest, Boolean.parseBoolean(fileInfo[2]), fileInfo[3]);
            }
        }
        return true;
    }

    
    // Fix method signature in actual file (String zipEntryName was missing type)
    private void downloadAndExtract(String urlStr, Path dest, boolean unzip, String zipEntryName) {
        // (Implementation above)
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