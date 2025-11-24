package ram.ka.ru.concurrent;

import ram.ka.ru.formats.UserSerializer;
import ram.ka.ru.models.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMeasurer {
    
    private final List<User> testData;

    public PerformanceMeasurer(List<User> testData) {
        this.testData = testData != null ? testData : Collections.emptyList();
    }

    /**
     * Измеряет общее wall time с виртуальными потоками
     * @param serializer Сериализатор (должен быть потокобезопасным)
     * @param threadCount Количество частей для разделения данных
     */
    public long measureTotalWallTime(UserSerializer serializer, int threadCount)
            throws IOException, InterruptedException {
        validateInputs(serializer, threadCount);
        
        if (testData.isEmpty()) {
            return 0;
        }

        long startWall = System.nanoTime();
        
        // Разделяем данные и запускаем в виртуальных потоках
        List<List<User>> partitions = partitionList(testData, threadCount);
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> tasks = createTasks(serializer, partitions);
            executor.invokeAll(tasks); // ждем завершения всех задач
        }

        return System.nanoTime() - startWall;
    }

    /**
     * Измеряет суммарное CPU время всех виртуальных потоков
     */
    public long measureTotalCpuTime(UserSerializer serializer, int threadCount) 
            throws IOException, InterruptedException {
        validateInputs(serializer, threadCount);
        
        if (testData.isEmpty()) {
            return 0;
        }

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        // Включаем поддержку измерения CPU time
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }

        List<List<User>> partitions = partitionList(testData, threadCount);
        
        try (var executor = Executors.newFixedThreadPool(threadCount)) {
            // Каждая задача возвращает свое CPU время
            List<Callable<Long>> tasks = partitions.stream()
                .map(partition -> (Callable<Long>) () -> measureTaskCpuTime(threadBean, serializer, partition))
                .toList();

            List<Future<Long>> futures = executor.invokeAll(tasks);
            
            // Суммируем CPU время всех потоков
            AtomicLong totalCpuTime = new AtomicLong();
            for (Future<Long> future : futures) {
                try {
                    totalCpuTime.getAndAdd(future.get());
                } catch (ExecutionException e) {
                    throw unwrapExecutionException(e);
                }
            }
            return totalCpuTime.get();
        }
    }

    // === Вспомогательные методы ===

    private void validateInputs(UserSerializer serializer, int threadCount) {
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer cannot be null");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be positive");
        }
    }

    /**
     * Создает задачи сериализации для каждой части данных
     */
    private List<Callable<Void>> createTasks(UserSerializer serializer, List<List<User>> partitions) {
        return partitions.stream()
            .map(partition -> (Callable<Void>) () -> {
                try {
                    for (User user : partition) {
                        serializer.serialize(user);
                    }
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .toList();
    }

    /**
     * Выполняет сериализацию и возвращает CPU время текущего потока
     */
    private long measureTaskCpuTime(ThreadMXBean threadBean, UserSerializer serializer, List<User> partition) throws IOException {
        long startCpu = threadBean.getCurrentThreadCpuTime();
        try {
            for (User user : partition) {
                serializer.serialize(user);
            }
        } finally {
            return threadBean.getCurrentThreadCpuTime() - startCpu;
        }
    }

    /**
     * Разделяет список на примерно равные части
     */
    private <T> List<List<T>> partitionList(List<T> list, int partitions) {
        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        if (size == 0) return result;

        int chunkSize = (size + partitions - 1) / partitions; // Округление вверх
        
        for (int i = 0; i < size; i += chunkSize) {
            result.add(list.subList(i, Math.min(size, i + chunkSize)));
        }
        return result;
    }

    private IOException unwrapExecutionException(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            return (IOException) cause;
        } else if (cause instanceof UncheckedIOException) {
            return ((UncheckedIOException) cause).getCause();
        }
        return new IOException("Unexpected error during serialization", cause);
    }
}
