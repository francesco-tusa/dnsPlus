package simulator.events.metrics;

import java.io.Serializable;

public class EventMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long traceId;

    public EventMetrics(long traceId) {
        this.traceId = traceId;
    }

    public EventMetrics(EventMetrics other) {
        this.traceId = other.traceId;
    }
    
    public long getTraceId() { 
        return traceId; 
    }
    
    public String getTraceIdString() {
        return String.valueOf(traceId);
    }
}