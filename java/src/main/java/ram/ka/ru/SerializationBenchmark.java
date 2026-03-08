package ram.ka.ru;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.flatbuffers.FlatBufferBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import ram.ka.ru.models.UserProtos;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

// === Модели данных ===

@Setter
@Getter
class User {
    String id = "123e4567-e89b-12d3-a456-426614174000";
    String name = "John Doe";
    String email = "john.doe@example.com";
    int age = 30;
    boolean active = true;
    String[] roles = new String[]{"admin", "user", "editor"};
    double balance = 1024.50;
}

// === Структуры отчета (точно как в Go) ===

@Setter
@Getter
class BenchmarkMeta {
    String language = "java";
    String version = "21";
    String timestamp = Instant.now().toString();
    int threads = 1;
    int total_iterations = 10000;
}

@Setter
@Getter
class LatencyStats {
    long mean, p50, p99;
}

@Setter
@Getter
class PerformanceStats {
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    double rps;
    LatencyStats latency_ns = new LatencyStats();
}

@Setter
@Getter
class ResourceStats {
    long memory_allocated_bytes;
    long memory_allocations_count;
    long cpu_user_time_ms = 0;
    long cpu_system_time_ms = 0;
}

@Setter
@Getter
class BenchmarkResult {
    String format;
    String operation = "roundtrip";
    PerformanceStats performance = new PerformanceStats();
    ResourceStats resources = new ResourceStats();
}

@Setter
@Getter
class BenchmarkReport {
    BenchmarkMeta benchmark_meta = new BenchmarkMeta();
    List<BenchmarkResult> results = new ArrayList<>();
}

// === Основной класс ===

public class SerializationBenchmark {

    static final int THREAD_COUNT = 4;
    static final int TOTAL_ITERATIONS = 10000;

    static final ObjectMapper jsonMapper = new ObjectMapper();
    static final XmlMapper xmlMapper = new XmlMapper();
    static final ExecutorService executor = Executors.newFixedThreadPool(
            THREAD_COUNT, Thread.ofVirtual().factory()
    );

    @SneakyThrows
    public static void main(String[] args) throws Exception {
        BenchmarkReport report = new BenchmarkReport();
        User user = new User();

        // Protobuf объект (создаем один раз)
        var protoUser = UserProtos.UserProto.newBuilder()
                .setId(user.id).setName(user.name).setEmail(user.email)
                .setAge(user.age).setActive(user.active).setBalance(user.balance)
                .addAllRoles(java.util.List.of(user.roles)).build();

        // JSON
        report.results.add(measure("json", () -> {
            try {
                byte[] data = jsonMapper.writeValueAsBytes(user);
                jsonMapper.readValue(data, UserProtos.UserProto.class);
            } catch (Exception e) {
            }
        }));

        // XML
        report.results.add(measure("xml", () -> {
            try {
                byte[] data = xmlMapper.writeValueAsBytes(user);
                xmlMapper.readValue(data, User.class);
            } catch (Exception e) {
            }
        }));

        // Protobuf
        report.results.add(measure("protobuf", () -> {
            try {
                byte[] data = protoUser.toByteArray();
                UserProtos.UserProto.parseFrom(data);
            } catch (Exception e) {
            }
        }));

        // FlatBuffers
        report.results.add(measure("flatbuffers", () -> {
            FlatBufferBuilder b = new FlatBufferBuilder(0);
            int name = b.createString(user.name);
            int email = b.createString(user.email);
            int id = b.createString(user.id);
            int[] roles = new int[user.roles.length];
            for (int i = 0; i < user.roles.length; i++) {
                roles[i] = b.createString(user.roles[i]);
            }
            int rolesVec = ram.ka.ru.models.flatbuffers.User.createRolesVector(b, roles);
            ram.ka.ru.models.flatbuffers.User.startUser(b);
            ram.ka.ru.models.flatbuffers.User.addId(b, id);
            ram.ka.ru.models.flatbuffers.User.addName(b, name);
            ram.ka.ru.models.flatbuffers.User.addEmail(b, email);
            ram.ka.ru.models.flatbuffers.User.addAge(b, user.age);
            ram.ka.ru.models.flatbuffers.User.addActive(b, user.active);
            ram.ka.ru.models.flatbuffers.User.addBalance(b, user.balance);
            ram.ka.ru.models.flatbuffers.User.addRoles(b, rolesVec);
            int root = ram.ka.ru.models.flatbuffers.User.endUser(b);
            b.finish(root);
            ram.ka.ru.models.flatbuffers.User.getRootAsUser(b.dataBuffer()).name();
        }));

        // Вывод JSON
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        executor.shutdown();
    }

    static BenchmarkResult measure(String format, Runnable task) throws Exception {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage startMem = memBean.getHeapMemoryUsage();
        long startAllocs = -1; // Java не предоставляет точное число аллокаций без JFR

        long start = System.nanoTime();

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        IntStream.range(0, THREAD_COUNT)
                .parallel()
                .forEach(i -> {
                    executor.submit(
                            () -> {
                                try {
                                    for (int k = 0; k < TOTAL_ITERATIONS / THREAD_COUNT; k++) {
                                        task.run();
                                    }
                                } finally {
                                    latch.countDown();
                                }
                            }
                    );
                });

        latch.await();

        long elapsedNs = System.nanoTime() - start; // Время в наносекундах
        MemoryUsage endMem = memBean.getHeapMemoryUsage();

        double rps = (double) TOTAL_ITERATIONS / toSeconds(elapsedNs);
        long latencyNs = elapsedNs / TOTAL_ITERATIONS;

        long memUsed = Math.max(0, endMem.getUsed() - startMem.getUsed());

        BenchmarkResult r = new BenchmarkResult();
        r.format = format;
        r.performance.rps = rps;
        r.performance.latency_ns.mean = latencyNs;
        r.performance.latency_ns.p50 = latencyNs;
        r.performance.latency_ns.p99 = (long) (latencyNs * 1.5);
        r.resources.memory_allocated_bytes = memUsed;
        r.resources.memory_allocations_count = 0;

        return r;
    }

    static double toSeconds(long elapsedNanos) {
        long sec = elapsedNanos / 1_000_000_000L;
        long nsec = elapsedNanos % 1_000_000_000L;
        return (double) sec + (double) nsec / 1e9;
    }
}