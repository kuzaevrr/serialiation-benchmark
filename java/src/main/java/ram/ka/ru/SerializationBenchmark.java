package ram.ka.ru;


import ram.ka.ru.formats.FlatBuffersSerializer;
import ram.ka.ru.formats.JsonSerializer;
import ram.ka.ru.formats.ProtobufSerializer;
import ram.ka.ru.formats.XmlSerializer;
import ram.ka.ru.models.User;

import java.util.*;
import java.io.IOException;

public class SerializationBenchmark {
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASUREMENT_ITERATIONS = 10000;
    private static final int DATA_SIZE = 1000;

    private final JsonSerializer jsonSerializer = new JsonSerializer();
    private final XmlSerializer xmlSerializer = new XmlSerializer();
    private final ProtobufSerializer protobufSerializer = new ProtobufSerializer();
    private final FlatBuffersSerializer flatBuffersSerializer = new FlatBuffersSerializer();
    private final List<User> testData;

    public SerializationBenchmark() {
        this.testData = generateTestData(DATA_SIZE);
    }

    private List<User> generateTestData(int size) {
        List<User> data = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < size; i++) {
            List<String> roles = Arrays.asList("user", "admin", "moderator");
            User user = new User(
                "user_" + i,
                "User Name " + i,
                "user" + i + "@example.com",
                20 + random.nextInt(50),
                random.nextBoolean(),
                roles,
                1000.0 + random.nextDouble() * 9000.0
            );
            data.add(user);
        }
        return data;
    }

    public void runBenchmarks() throws IOException {
        System.out.println("Starting Java Serialization Benchmark...");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Measurement iterations: " + MEASUREMENT_ITERATIONS);
        System.out.println("Data size: " + DATA_SIZE + " objects\n");

        // Warmup
        performWarmup();

        // Benchmark всех форматов
        benchmarkFormat("JSON", jsonSerializer);
        benchmarkFormat("XML", xmlSerializer);
        benchmarkFormat("Protobuf", protobufSerializer);
        benchmarkFormat("FlatBuffers", flatBuffersSerializer);

        // Memory usage comparison
        compareMemoryUsage();

        // Throughput test
        measureThroughput();
    }

    private void performWarmup() throws IOException {
        System.out.println("Performing warmup...");
        for (int i = 0; i < WARMUP_ITERATIONS / 100; i++) {
            for (User user : testData.subList(0, 10)) {
                jsonSerializer.serialize(user);
                xmlSerializer.serialize(user);
                protobufSerializer.serialize(user);
                flatBuffersSerializer.serialize(user);
            }
        }
    }
    private BenchmarkResult benchmarkFormat(String formatName, Object serializer) throws IOException {
        System.out.println("\n=== " + formatName + " Benchmark ===");

        long totalSerializationTime = 0;
        long totalDeserializationTime = 0;
        int totalBytes = 0;

        for (User user : testData) {
            byte[] serializedData = null;

            // Serialization
            long startSerialize = System.nanoTime();
            if (serializer instanceof JsonSerializer) {
                serializedData = ((JsonSerializer) serializer).serialize(user);
            } else if (serializer instanceof XmlSerializer) {
                serializedData = ((XmlSerializer) serializer).serialize(user);
            } else if (serializer instanceof ProtobufSerializer) {
                serializedData = ((ProtobufSerializer) serializer).serialize(user);
            } else if (serializer instanceof FlatBuffersSerializer) {
                serializedData = ((FlatBuffersSerializer) serializer).serialize(user);
            }
            long endSerialize = System.nanoTime();

            // Deserialization
            long startDeserialize = System.nanoTime();
            if (serializer instanceof JsonSerializer) {
                ((JsonSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof XmlSerializer) {
                ((XmlSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof ProtobufSerializer) {
                ((ProtobufSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof FlatBuffersSerializer) {
                ((FlatBuffersSerializer) serializer).deserialize(serializedData);
            }
            long endDeserialize = System.nanoTime();

            totalSerializationTime += (endSerialize - startSerialize);
            totalDeserializationTime += (endDeserialize - startDeserialize);
            totalBytes += serializedData.length;
        }

        long avgSerializationTime = totalSerializationTime / testData.size();
        long avgDeserializationTime = totalDeserializationTime / testData.size();
        double avgSizeBytes = totalBytes / (double) testData.size();

        System.out.printf("Avg Serialization Time: %.3f ms%n", avgSerializationTime / 1_000_000.0);
        System.out.printf("Avg Deserialization Time: %.3f ms%n", avgDeserializationTime / 1_000_000.0);
        System.out.printf("Avg Data Size: %.2f bytes%n", avgSizeBytes);

        // Measure memory usage
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        List<byte[]> serializedData = new ArrayList<>();
        for (User user : testData) {
            if (serializer instanceof JsonSerializer) {
                serializedData.add(((JsonSerializer) serializer).serialize(user));
            } else if (serializer instanceof XmlSerializer) {
                serializedData.add(((XmlSerializer) serializer).serialize(user));
            } else if (serializer instanceof ProtobufSerializer) {
                serializedData.add(((ProtobufSerializer) serializer).serialize(user));
            } else if (serializer instanceof FlatBuffersSerializer) {
                serializedData.add(((FlatBuffersSerializer) serializer).serialize(user));
            }
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        serializedData.clear();
        System.gc();

        System.out.printf("Memory Used: %d bytes%n", memoryUsed);

        // Measure throughput
        int throughputIterations = 10000;

        long start = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, Math.min(100, testData.size()))) {
                if (serializer instanceof JsonSerializer) {
                    ((JsonSerializer) serializer).serialize(user);
                } else if (serializer instanceof XmlSerializer) {
                    ((XmlSerializer) serializer).serialize(user);
                } else if (serializer instanceof ProtobufSerializer) {
                    ((ProtobufSerializer) serializer).serialize(user);
                } else if (serializer instanceof FlatBuffersSerializer) {
                    ((FlatBuffersSerializer) serializer).serialize(user);
                }
            }
        }
        long end = System.nanoTime();
        double throughput = (throughputIterations * Math.min(100, testData.size())) / ((end - start) / 1_000_000_000.0);

        System.out.printf("Throughput: %.4f ops/sec%n", throughput);

        return new BenchmarkResult("Java", formatName, avgSerializationTime, avgDeserializationTime,
                avgSizeBytes, memoryUsed, throughput);
    }

    private void compareMemoryUsage() throws IOException {
        System.out.println("\n=== Memory Usage Comparison ===");

        Runtime runtime = Runtime.getRuntime();

        // Measure JSON memory
        runtime.gc();
        long memoryBeforeJson = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> jsonData = new ArrayList<>();
        for (User user : testData) {
            jsonData.add(jsonSerializer.serialize(user));
        }
        long memoryAfterJson = runtime.totalMemory() - runtime.freeMemory();
        long jsonMemoryUsed = memoryAfterJson - memoryBeforeJson;
        jsonData.clear();

        // Measure JSON memory
        runtime.gc();
        long memoryBeforeXml = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> xmlData = new ArrayList<>();
        for (User user : testData) {
            xmlData.add(xmlSerializer.serialize(user));
        }
        long memoryAfterXml = runtime.totalMemory() - runtime.freeMemory();
        long xmlMemoryUsed = memoryAfterXml - memoryBeforeXml;
        xmlData.clear();

        // Measure Protobuf memory
        runtime.gc();
        long memoryBeforeProto = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> protoData = new ArrayList<>();
        for (User user : testData) {
            protoData.add(protobufSerializer.serialize(user));
        }
        long memoryAfterProto = runtime.totalMemory() - runtime.freeMemory();
        long protoMemoryUsed = memoryAfterProto - memoryBeforeProto;
        protoData.clear();

        // Measure Flat Buffer memory
        runtime.gc();
        long memoryBeforeFlatBuffer = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> flatBufferData = new ArrayList<>();
        for (User user : testData) {
            protoData.add(flatBuffersSerializer.serialize(user));
        }
        long memoryAfterFlatBuffer = runtime.totalMemory() - runtime.freeMemory();
        long flatBufferMemoryUsed = memoryAfterFlatBuffer - memoryBeforeFlatBuffer;
        flatBufferData.clear();

        runtime.gc();

        System.out.printf("JSON Memory Used: %d bytes (%s MB)%n", jsonMemoryUsed, toMB(jsonMemoryUsed));
        System.out.printf("XML Memory Used: %d bytes (%s MB)%n", xmlMemoryUsed, toMB(xmlMemoryUsed));
        System.out.printf("Protobuf Memory Used: %d bytes (%s MB)%n", protoMemoryUsed, toMB(protoMemoryUsed));
        System.out.printf("FlatBuffer Memory Used: %d bytes (%s MB)%n", flatBufferMemoryUsed, toMB(flatBufferMemoryUsed));
    }

    private String toMB(long bytes) {
        return String.format("%.4f", (double) bytes / (1024.0 * 1024.0));
    }

    private void measureThroughput() throws IOException {
        System.out.println("\n=== Throughput Test ===");
        int throughputIterations = 10000;

        // JSON throughput
        long jsonStart = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                jsonSerializer.serialize(user);
            }
        }
        long jsonEnd = System.nanoTime();
        double jsonThroughput = (throughputIterations * 100.0) / ((jsonEnd - jsonStart) / 1_000_000_000.0);

        // XML throughput
        long xmlStart = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                xmlSerializer.serialize(user);
            }
        }
        long xmlEnd = System.nanoTime();
        double xmlThroughput = (throughputIterations * 100.0) / ((xmlEnd - xmlStart) / 1_000_000_000.0);

        // Protobuf throughput
        long protoStart = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                protobufSerializer.serialize(user);
            }
        }
        long protoEnd = System.nanoTime();
        double protoThroughput = (throughputIterations * 100.0) / ((protoEnd - protoStart) / 1_000_000_000.0);

        // FlatBuffer throughput
        long flatBufferStart = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                flatBuffersSerializer.serialize(user);
            }
        }
        long flatBufferEnd = System.nanoTime();
        double flatBufferThroughput = (throughputIterations * 100.0) / ((flatBufferEnd - flatBufferStart) / 1_000_000_000.0);

        System.out.printf("JSON Throughput: %.4f ops/sec%n", jsonThroughput);
        System.out.printf("XML Throughput: %.4f ops/sec%n", xmlThroughput);
        System.out.printf("Protobuf Throughput: %.4f ops/sec%n", protoThroughput);
        System.out.printf("FlatBuffer Throughput: %.4f ops/sec%n", flatBufferThroughput);

    }

    public static void main(String[] args) throws IOException {
        SerializationBenchmark benchmark = new SerializationBenchmark();
        benchmark.runBenchmarks();
    }
}