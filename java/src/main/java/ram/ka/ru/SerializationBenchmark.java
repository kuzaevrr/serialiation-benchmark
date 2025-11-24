package ram.ka.ru;


import ram.ka.ru.concurrent.PerformanceMeasurer;
import ram.ka.ru.formats.FlatBuffersSerializer;
import ram.ka.ru.formats.JsonSerializer;
import ram.ka.ru.formats.ProtobufSerializer;
import ram.ka.ru.formats.XmlSerializer;
import ram.ka.ru.models.User;

import java.util.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SerializationBenchmark {
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int MEASUREMENT_ITERATIONS = 1000000;
    private static final int DATA_SIZE = 100*1024; //100 kb
    private static final int THREAD_COUNT = 6;

    private final JsonSerializer jsonSerializer = new JsonSerializer();
    private final XmlSerializer xmlSerializer = new XmlSerializer();
    private final ProtobufSerializer protobufSerializer = new ProtobufSerializer();
    private final FlatBuffersSerializer flatBuffersSerializer = new FlatBuffersSerializer();
    private final List<User> testData;
    private final ThreadMXBean threadBean; // Добавлен ThreadMXBean для измерения CPU

    public SerializationBenchmark() {
        this.testData = generateTestData(DATA_SIZE);
        this.threadBean = ManagementFactory.getThreadMXBean();
        // Включаем поддержку измерения CPU time
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
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

    public void runBenchmarks() throws IOException, InterruptedException {
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

        // CPU usage comparison (НОВЫЙ МЕТОД)
        compareCpuUsage();
        compareCpuUsageMultiThreads();

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
        long totalSerializationCpuTime = 0; // Новая метрика
        long totalDeserializationCpuTime = 0; // Новая метрика
        int totalBytes = 0;

        for (User user : testData) {
            byte[] serializedData = null;

            // Serialization с измерением CPU time
            long startSerializeWall = System.nanoTime();
            long startSerializeCpu = threadBean.getCurrentThreadCpuTime(); // CPU time before

            if (serializer instanceof JsonSerializer) {
                serializedData = ((JsonSerializer) serializer).serialize(user);
            } else if (serializer instanceof XmlSerializer) {
                serializedData = ((XmlSerializer) serializer).serialize(user);
            } else if (serializer instanceof ProtobufSerializer) {
                serializedData = ((ProtobufSerializer) serializer).serialize(user);
            } else if (serializer instanceof FlatBuffersSerializer) {
                serializedData = ((FlatBuffersSerializer) serializer).serialize(user);
            }

            long endSerializeCpu = threadBean.getCurrentThreadCpuTime(); // CPU time after
            long endSerializeWall = System.nanoTime();

            // Deserialization с измерением CPU time
            long startDeserializeWall = System.nanoTime();
            long startDeserializeCpu = threadBean.getCurrentThreadCpuTime(); // CPU time before

            if (serializer instanceof JsonSerializer) {
                ((JsonSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof XmlSerializer) {
                ((XmlSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof ProtobufSerializer) {
                ((ProtobufSerializer) serializer).deserialize(serializedData);
            } else if (serializer instanceof FlatBuffersSerializer) {
                ((FlatBuffersSerializer) serializer).deserialize(serializedData);
            }

            long endDeserializeCpu = threadBean.getCurrentThreadCpuTime(); // CPU time after
            long endDeserializeWall = System.nanoTime();

            // Accumulate times
            totalSerializationTime += (endSerializeWall - startSerializeWall);
            totalDeserializationTime += (endDeserializeWall - startDeserializeWall);
            totalSerializationCpuTime += (endSerializeCpu - startSerializeCpu);
            totalDeserializationCpuTime += (endDeserializeCpu - startDeserializeCpu);
            totalBytes += serializedData.length;
        }

        long avgSerializationTime = totalSerializationTime / testData.size();
        long avgDeserializationTime = totalDeserializationTime / testData.size();
        long avgSerializationCpuTime = totalSerializationCpuTime / testData.size();
        long avgDeserializationCpuTime = totalDeserializationCpuTime / testData.size();
        double avgSizeBytes = totalBytes / (double) testData.size();

        // Calculate CPU utilization percentage
        double serializationCpuUtilization = (avgSerializationCpuTime > 0 && avgSerializationTime > 0)
                ? (avgSerializationCpuTime * 100.0 / avgSerializationTime) : 0.0;
        double deserializationCpuUtilization = (avgDeserializationCpuTime > 0 && avgDeserializationTime > 0)
                ? (avgDeserializationCpuTime * 100.0 / avgDeserializationTime) : 0.0;

        System.out.printf("Avg Serialization Time: %.3f ms (CPU time: %.3f ms, utilization: %.2f%%)%n",
                avgSerializationTime / 1_000_000.0, avgSerializationCpuTime / 1_000_000.0, serializationCpuUtilization);
        System.out.printf("Avg Deserialization Time: %.3f ms (CPU time: %.3f ms, utilization: %.2f%%)%n",
                avgDeserializationTime / 1_000_000.0, avgDeserializationCpuTime / 1_000_000.0, deserializationCpuUtilization);
        System.out.printf("Avg Data Size: %.2f bytes%n", avgSizeBytes);

        // Memory usage measurement (без изменений)
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

        // Throughput measurement с CPU utilization (ОБНОВЛЕНО)
        int throughputIterations = 10000;

        long startWall = System.nanoTime();
        long startCpu = threadBean.getCurrentThreadCpuTime();

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

        long endCpu = threadBean.getCurrentThreadCpuTime();
        long endWall = System.nanoTime();

        double throughput = (throughputIterations * Math.min(100, testData.size())) / ((endWall - startWall) / 1_000_000_000.0);
        long totalCpuTime = endCpu - startCpu;
        double throughputCpuUtilization = (totalCpuTime > 0 && (endWall - startWall) > 0)
                ? (totalCpuTime * 100.0 / (endWall - startWall)) : 0.0;

        System.out.printf("Throughput: %.4f ops/sec (CPU utilization: %.2f%%)%n", throughput, throughputCpuUtilization);

        return new BenchmarkResult("Java", formatName, avgSerializationTime, avgDeserializationTime,
                avgSizeBytes, memoryUsed, throughput);
    }

    private void compareMemoryUsage() throws IOException {
        System.out.println("\n=== Memory Usage Comparison ===");

        Runtime runtime = Runtime.getRuntime();

        // JSON memory
        runtime.gc();
        long memoryBeforeJson = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> jsonData = new ArrayList<>();
        for (User user : testData) {
            jsonData.add(jsonSerializer.serialize(user));
        }
        long memoryAfterJson = runtime.totalMemory() - runtime.freeMemory();
        long jsonMemoryUsed = memoryAfterJson - memoryBeforeJson;
        jsonData.clear();

        // XML memory
        runtime.gc();
        long memoryBeforeXml = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> xmlData = new ArrayList<>();
        for (User user : testData) {
            xmlData.add(xmlSerializer.serialize(user));
        }
        long memoryAfterXml = runtime.totalMemory() - runtime.freeMemory();
        long xmlMemoryUsed = memoryAfterXml - memoryBeforeXml;
        xmlData.clear();

        // Protobuf memory
        runtime.gc();
        long memoryBeforeProto = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> protoData = new ArrayList<>();
        for (User user : testData) {
            protoData.add(protobufSerializer.serialize(user));
        }
        long memoryAfterProto = runtime.totalMemory() - runtime.freeMemory();
        long protoMemoryUsed = memoryAfterProto - memoryBeforeProto;
        protoData.clear();

        // FlatBuffer memory
        runtime.gc();
        long memoryBeforeFlatBuffer = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> flatBufferData = new ArrayList<>();
        for (User user : testData) {
            flatBufferData.add(flatBuffersSerializer.serialize(user));
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

    // НОВЫЙ МЕТОД: Сравнение утилизации CPU
    private void compareCpuUsage() throws IOException, InterruptedException {
        System.out.println("\n=== CPU Usage Comparison ===");

        if (!threadBean.isThreadCpuTimeSupported()) {
            System.out.println("CPU time measurement is not supported on this JVM");
            return;
        }

        // Измеряем CPU time для сериализации всех данных
        long jsonCpuTime = measureTotalCpuTime(jsonSerializer);
        long xmlCpuTime = measureTotalCpuTime(xmlSerializer);
        long protoCpuTime = measureTotalCpuTime(protobufSerializer);
        long flatBufferCpuTime = measureTotalCpuTime(flatBuffersSerializer);

        // Рассчитываем total wall-clock time для сравнения
        long jsonWallTime = measureTotalWallTime(jsonSerializer);
        long xmlWallTime = measureTotalWallTime(xmlSerializer);
        long protoWallTime = measureTotalWallTime(protobufSerializer);
        long flatBufferWallTime = measureTotalWallTime(flatBuffersSerializer);

        // Рассчитываем утилизацию CPU
        double jsonCpuUtilization = (jsonCpuTime * 100.0 / jsonWallTime);
        double xmlCpuUtilization = (xmlCpuTime * 100.0 / xmlWallTime);
        double protoCpuUtilization = (protoCpuTime * 100.0 / protoWallTime);
        double flatBufferCpuUtilization = (flatBufferCpuTime * 100.0 / flatBufferWallTime);

        System.out.printf("JSON CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                jsonCpuTime / 1_000_000.0, jsonWallTime / 1_000_000.0, jsonCpuUtilization);
        System.out.printf("XML CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                xmlCpuTime / 1_000_000.0, xmlWallTime / 1_000_000.0, xmlCpuUtilization);
        System.out.printf("Protobuf CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                protoCpuTime / 1_000_000.0, protoWallTime / 1_000_000.0, protoCpuUtilization);
        System.out.printf("FlatBuffer CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                flatBufferCpuTime / 1_000_000.0, flatBufferWallTime / 1_000_000.0, flatBufferCpuUtilization);
    }

    private void compareCpuUsageMultiThreads() throws IOException, InterruptedException {
        System.out.printf("\n=== CPU Usage Comparison Core Count %s ===%n", THREAD_COUNT);

        if (!threadBean.isThreadCpuTimeSupported()) {
            System.out.println("CPU time measurement is not supported on this JVM");
            return;
        }

        PerformanceMeasurer performanceMeasurer = new PerformanceMeasurer(testData);

        // Измеряем CPU time для сериализации всех данных
        long jsonCpuTime = performanceMeasurer.measureTotalCpuTime(jsonSerializer, THREAD_COUNT);
        long xmlCpuTime = performanceMeasurer.measureTotalCpuTime(xmlSerializer, THREAD_COUNT);
        long protoCpuTime = performanceMeasurer.measureTotalCpuTime(protobufSerializer, THREAD_COUNT);
        long flatBufferCpuTime = performanceMeasurer.measureTotalCpuTime(flatBuffersSerializer, THREAD_COUNT);

        // Рассчитываем total wall-clock time для сравнения
        long jsonWallTime = performanceMeasurer.measureTotalWallTime(jsonSerializer, THREAD_COUNT);
        long xmlWallTime = performanceMeasurer.measureTotalWallTime(xmlSerializer, THREAD_COUNT);
        long protoWallTime = performanceMeasurer.measureTotalWallTime(protobufSerializer, THREAD_COUNT);
        long flatBufferWallTime = performanceMeasurer.measureTotalWallTime(flatBuffersSerializer, THREAD_COUNT);

        // Рассчитываем утилизацию CPU
        double jsonCpuUtilization = (jsonCpuTime * 100.0 / jsonWallTime);
        double xmlCpuUtilization = (xmlCpuTime * 100.0 / xmlWallTime);
        double protoCpuUtilization = (protoCpuTime * 100.0 / protoWallTime);
        double flatBufferCpuUtilization = (flatBufferCpuTime * 100.0 / flatBufferWallTime);

        System.out.printf("JSON CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                jsonCpuTime / 1_000_000.0, jsonWallTime / 1_000_000.0, jsonCpuUtilization);
        System.out.printf("XML CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                xmlCpuTime / 1_000_000.0, xmlWallTime / 1_000_000.0, xmlCpuUtilization);
        System.out.printf("Protobuf CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                protoCpuTime / 1_000_000.0, protoWallTime / 1_000_000.0, protoCpuUtilization);
        System.out.printf("FlatBuffer CPU Time: %.3f ms, Wall Time: %.3f ms, Utilization: %.2f%%%n",
                flatBufferCpuTime / 1_000_000.0, flatBufferWallTime / 1_000_000.0, flatBufferCpuUtilization);
    }

    // Вспомогательный метод для измерения CPU time
    private long measureTotalCpuTime(Object serializer) throws IOException {
        return measureTotalCpuTime(serializer, 1);
    }

    private long measureTotalCpuTime(Object serializer, Integer countThreads) throws IOException {
        long startCpu = threadBean.getCurrentThreadCpuTime();
        for (User user : testData) {
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
        return threadBean.getCurrentThreadCpuTime() - startCpu;
    }

    // Вспомогательный метод для измерения wall-clock time
    private long measureTotalWallTime(Object serializer) throws IOException {
        return measureTotalWallTime(serializer, 1);
    }

    private long measureTotalWallTime(Object serializer, Integer countThreads) throws IOException {
        long startWall = System.nanoTime();
        for (User user : testData) {
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
        return System.nanoTime() - startWall;
    }

    private String toMB(long bytes) {
        return String.format("%.4f", (double) bytes / (1024.0 * 1024.0));
    }

    private void measureThroughput() throws IOException {
        System.out.println("\n=== Throughput Test ===");
        int throughputIterations = 10000;

        // JSON throughput with CPU
        long jsonStartWall = System.nanoTime();
        long jsonStartCpu = threadBean.getCurrentThreadCpuTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                jsonSerializer.serialize(user);
            }
        }
        long jsonEndCpu = threadBean.getCurrentThreadCpuTime();
        long jsonEndWall = System.nanoTime();
        double jsonThroughput = (throughputIterations * 100.0) / ((jsonEndWall - jsonStartWall) / 1_000_000_000.0);
        double jsonCpuUtil = ((jsonEndCpu - jsonStartCpu) * 100.0) / (jsonEndWall - jsonStartWall);

        // XML throughput with CPU
        long xmlStartWall = System.nanoTime();
        long xmlStartCpu = threadBean.getCurrentThreadCpuTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                xmlSerializer.serialize(user);
            }
        }
        long xmlEndCpu = threadBean.getCurrentThreadCpuTime();
        long xmlEndWall = System.nanoTime();
        double xmlThroughput = (throughputIterations * 100.0) / ((xmlEndWall - xmlStartWall) / 1_000_000_000.0);
        double xmlCpuUtil = ((xmlEndCpu - xmlStartCpu) * 100.0) / (xmlEndWall - xmlStartWall);

        // Protobuf throughput with CPU
        long protoStartWall = System.nanoTime();
        long protoStartCpu = threadBean.getCurrentThreadCpuTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                protobufSerializer.serialize(user);
            }
        }
        long protoEndCpu = threadBean.getCurrentThreadCpuTime();
        long protoEndWall = System.nanoTime();
        double protoThroughput = (throughputIterations * 100.0) / ((protoEndWall - protoStartWall) / 1_000_000_000.0);
        double protoCpuUtil = ((protoEndCpu - protoStartCpu) * 100.0) / (protoEndWall - protoStartWall);

        // FlatBuffer throughput with CPU
        long flatBufferStartWall = System.nanoTime();
        long flatBufferStartCpu = threadBean.getCurrentThreadCpuTime();
        for (int i = 0; i < throughputIterations; i++) {
            for (User user : testData.subList(0, 100)) {
                flatBuffersSerializer.serialize(user);
            }
        }
        long flatBufferEndCpu = threadBean.getCurrentThreadCpuTime();
        long flatBufferEndWall = System.nanoTime();
        double flatBufferThroughput = (throughputIterations * 100.0) / ((flatBufferEndWall - flatBufferStartWall) / 1_000_000_000.0);
        double flatBufferCpuUtil = ((flatBufferEndCpu - flatBufferStartCpu) * 100.0) / (flatBufferEndWall - flatBufferStartWall);

        // Вывод результатов с CPU utilization
        System.out.printf("JSON Throughput: %.4f ops/sec (CPU utilization: %.2f%%)%n", jsonThroughput, jsonCpuUtil);
        System.out.printf("XML Throughput: %.4f ops/sec (CPU utilization: %.2f%%)%n", xmlThroughput, xmlCpuUtil);
        System.out.printf("Protobuf Throughput: %.4f ops/sec (CPU utilization: %.2f%%)%n", protoThroughput, protoCpuUtil);
        System.out.printf("FlatBuffer Throughput: %.4f ops/sec (CPU utilization: %.2f%%)%n", flatBufferThroughput, flatBufferCpuUtil);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SerializationBenchmark benchmark = new SerializationBenchmark();
        benchmark.runBenchmarks();
    }
}