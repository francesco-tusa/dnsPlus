package simulator.events.metrics;

import java.io.Serializable;

/**
 * Holds metadata for event tracing and metrics.
 * <p>
 * Optimization Note: This object is shared by reference across multiple
 * SimulationPublication/Subscription instances during propagation.
 * Fields like originalSourceName should only be populated if 
 * event tracing is explicitly enabled in SimConfiguration.
 * The traceId is always required for latency and ground-truth calculations.
 */
public class EventMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long traceId;
    
    // These fields are lazily populated by generators only if tracing is enabled.
    // They are effectively immutable once set for a given event.
    private String originalSourceName;
    private String originalCountry;
    private Double originalLongitude;
    private Double originalLatitude;

    public EventMetrics(long traceId) {
        this.traceId = traceId;
    }

    public EventMetrics(EventMetrics other) {
        this.traceId = other.traceId;
        this.originalSourceName = other.originalSourceName;
        this.originalCountry = other.originalCountry;
        this.originalLongitude = other.originalLongitude;
        this.originalLatitude = other.originalLatitude;
    }
    
    public long getTraceId() { return traceId; }
    public String getTraceIdString() { return String.valueOf(traceId); }
    
    public void setOriginalSourceInfo(String name, String country, Double lon, Double lat) {
        this.originalSourceName = name;
        this.originalCountry = country;
        this.originalLongitude = lon;
        this.originalLatitude = lat;
    }

    public String getOriginalSourceName() { return originalSourceName; }
    public String getOriginalCountry() { return originalCountry; }
    public Double getOriginalLongitude() { return originalLongitude; }
    public Double getOriginalLatitude() { return originalLatitude; }
}