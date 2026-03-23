package marketplace.workload.traces;

public record AzureTraceRecord(long functionId, long invocationCount, double duration, double memory) {}
