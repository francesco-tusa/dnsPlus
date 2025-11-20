package simulator.events.metrics;

import java.io.Serializable;

public class EventMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String traceId;
    private int hops;

    /**
     * Called by the Originator (Publisher/Subscriber) to start a new trace.
     */
    public EventMetrics(String traceId) {
        this.traceId = traceId;
        this.hops = 0;
    }

    /**
     * Copy constructor for deep copying metrics during branching.
     */
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
    
    public String getTraceId() { 
        return traceId; 
    }
}