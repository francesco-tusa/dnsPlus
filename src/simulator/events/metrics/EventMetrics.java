package simulator.events.metrics;

import java.io.Serializable;

public class EventMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long traceId;
    private int hops;

    public EventMetrics(long traceId) {
        this.traceId = traceId;
        this.hops = 0;
    }

    public EventMetrics(EventMetrics other) {
        this.traceId = other.traceId;
        this.hops = other.hops;
    }
    
    public void incrementHops() { 
        this.hops++; 
    }
    
    public int getHops() { 
        return hops; 
    }
    
    public long getTraceId() { 
        return traceId; 
    }
    
    public String getTraceIdString() {
        return String.valueOf(traceId);
    }
}