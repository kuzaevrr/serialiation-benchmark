package ram.ka.ru;

import java.io.Serializable;

public class BenchmarkResult implements Serializable {
    private String language;
    private String format;
    private long avgSerializeTimeNs;
    private long avgDeserializeTimeNs;
    private double avgDataSizeBytes;
    private long memoryUsageBytes;
    private double throughputOpsSec;

    public BenchmarkResult(String language, String format, long serializeTime, long deserializeTime, 
                          double dataSize, long memoryUsage, double throughput) {
        this.language = language;
        this.format = format;
        this.avgSerializeTimeNs = serializeTime;
        this.avgDeserializeTimeNs = deserializeTime;
        this.avgDataSizeBytes = dataSize;
        this.memoryUsageBytes = memoryUsage;
        this.throughputOpsSec = throughput;
    }

    // Getters for JSON serialization
    public String getLanguage() { return language; }
    public String getFormat() { return format; }
    public long getAvgSerializeTimeNs() { return avgSerializeTimeNs; }
    public long getAvgDeserializeTimeNs() { return avgDeserializeTimeNs; }
    public double getAvgDataSizeBytes() { return avgDataSizeBytes; }
    public long getMemoryUsageBytes() { return memoryUsageBytes; }
    public double getThroughputOpsSec() { return throughputOpsSec; }


}