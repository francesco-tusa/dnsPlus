package simulator.topology.geonames.builder;

import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

public class GeoNamesEntry {
    public int geonameId;
    public String name;
    public String featureClass;
    public String featureCode;
    public String countryCode;
    public String admin1Code;
    public String admin2Code;
    public long population;
    public int elevation;
    public double latitude = Double.NaN;
    public double longitude = Double.NaN;

    private static final Logger logger = CustomLogger.getLogger(GeoNamesEntry.class.getName());

    public GeoNamesEntry(String[] parts) {
        try {
            if (parts.length < 15) { // Basic validation
                this.geonameId = -1; return;
            }
            this.geonameId = Integer.parseInt(parts[0].trim());
            this.name = parts[1].trim();
            this.latitude = parseDouble(parts[4]);
            this.longitude = parseDouble(parts[5]);
            this.featureClass = parts[6].trim();
            this.featureCode = parts[7].trim();
            this.countryCode = parts[8].trim();
            this.admin1Code = parts[10].trim();
            this.admin2Code = parts[11].trim();
            this.population = parseLong(parts[14]);
            if (parts.length > 15) this.elevation = parseInt(parts[15]);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Parse error for line: " + (parts.length > 0 ? parts[0] : "empty"), e);
            this.geonameId = -1;
        }
    }
    
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch(Exception e) { return Double.NaN; }}
    private long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch(Exception e) { return 0; }}
    private int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e) { return 0; }}
}