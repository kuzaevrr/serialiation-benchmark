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

    private void benchmarkFormat(String formatName, Object serializer) throws IOException {
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

        double avgSerializationTimeMs = (totalSerializationTime / (double) testData.size()) / 1_000_000.0;
        double avgDeserializationTimeMs = (totalDeserializationTime / (double) testData.size()) / 1_000_000.0;
        double avgSizeBytes = totalBytes / (double) testData.size();

        System.out.printf("Avg Serialization Time: %.3f ms%n", avgSerializationTimeMs);
        System.out.printf("Avg Deserialization Time: %.3f ms%n", avgDeserializationTimeMs);
        System.out.printf("Avg Data Size: %.2f bytes%n", avgSizeBytes);
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

        // Measure Protobuf memory
        runtime.gc();
        long memoryBeforeProto = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> protoData = new ArrayList<>();
        for (User user : testData) {
            protoData.add(protobufSerializer.serialize(user));
        }
        long memoryAfterProto = runtime.totalMemory() - runtime.freeMemory();
        long protoMemoryUsed = memoryAfterProto - memoryBeforeProto;

        System.out.printf("JSON Memory Used: %d bytes%n", jsonMemoryUsed);
        System.out.printf("Protobuf Memory Used: %d bytes%n", protoMemoryUsed);
        System.out.printf("Memory Ratio (JSON/Protobuf): %.2f%n",
            jsonMemoryUsed / (double) protoMemoryUsed);
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

        // Protobuf throughput
        long protoStart = System.nanoTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                protobufSerializer.serialize(user);
            }
        }
        long protoEnd = System.nanoTime();
        double protoThroughput = (throughputIterations * 100.0) / ((protoEnd - protoStart) / 1_000_000_000.0);

        System.out.printf("JSON Throughput: %.2f ops/sec%n", jsonThroughput);
        System.out.printf("Protobuf Throughput: %.2f ops/sec%n", protoThroughput);
        System.out.printf("Throughput Ratio (Protobuf/JSON): %.2f%n", protoThroughput / jsonThroughput);
    }

    public static void main(String[] args) throws IOException {
        SerializationBenchmark benchmark = new SerializationBenchmark();
        benchmark.runBenchmarks();
    }
}