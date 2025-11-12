// main.go
package main

import (
	"encoding/json"
	"fmt"
	"golang/models"
	"golang/serializers"
	"log"
	"math/rand"
	"runtime"
	"time"

	"runtime/debug"
)

const (
	warmupIterations      = 1000
	measurementIterations = 10000
	dataSize              = 1000
)

type BenchmarkResult struct {
	Format             string
	AvgSerializeTime   time.Duration
	AvgDeserializeTime time.Duration
	AvgDataSize        float64
	MemoryUsage        uint64
	Throughput         float64
}

type Serializer interface {
	Serialize(user *models.User) ([]byte, error)
	Deserialize(data []byte) (*models.User, error)
	MeasureSerializationTime(user *models.User, iterations int) time.Duration
}

func generateTestData(size int) []*models.User {
	data := make([]*models.User, size)
	r := rand.New(rand.NewSource(42))

	for i := 0; i < size; i++ {
		roles := []string{"user", "admin", "moderator"}
		user := &models.User{
			ID:      fmt.Sprintf("user_%d", i),
			Name:    fmt.Sprintf("User Name %d", i),
			Email:   fmt.Sprintf("user%d@example.com", i),
			Age:     20 + r.Intn(50),
			Active:  r.Float32() < 0.5,
			Roles:   roles,
			Balance: 1000.0 + r.Float64()*9000.0,
		}
		data[i] = user
	}

	return data
}

func performWarmup(serializers map[string]Serializer, testData []*models.User) {
	fmt.Println("Performing warmup...")
	for i := 0; i < warmupIterations/100; i++ {
		for _, user := range testData[:10] {
			for _, serializer := range serializers {
				data, err := serializer.Serialize(user)
				if err != nil {
					continue
				}
				serializer.Deserialize(data)
			}
		}
	}
}

func benchmarkFormat(formatName string, serializer Serializer, testData []*models.User) BenchmarkResult {
	fmt.Printf("\n=== %s Benchmark ===\n", formatName)

	var totalSerializeTime time.Duration
	var totalDeserializeTime time.Duration
	totalBytes := 0

	for _, user := range testData {
		// Сериализация
		startSerialize := time.Now()
		data, err := serializer.Serialize(user)
		serializeTime := time.Since(startSerialize)

		if err != nil {
			log.Printf("Serialization error for %s: %v", formatName, err)
			continue
		}

		// Десериализация
		startDeserialize := time.Now()
		_, err = serializer.Deserialize(data)
		deserializeTime := time.Since(startDeserialize)

		if err != nil {
			log.Printf("Deserialization error for %s: %v", formatName, err)
			continue
		}

		totalSerializeTime += serializeTime
		totalDeserializeTime += deserializeTime
		totalBytes += len(data)
	}

	avgSerializeTime := totalSerializeTime / time.Duration(len(testData))
	avgDeserializeTime := totalDeserializeTime / time.Duration(len(testData))
	avgDataSize := float64(totalBytes) / float64(len(testData))

	fmt.Printf("Avg Serialization Time: %v\n", avgSerializeTime)
	fmt.Printf("Avg Deserialization Time: %v\n", avgDeserializeTime)
	fmt.Printf("Avg Data Size: %.2f bytes\n", avgDataSize)

	return BenchmarkResult{
		Format:             formatName,
		AvgSerializeTime:   avgSerializeTime,
		AvgDeserializeTime: avgDeserializeTime,
		AvgDataSize:        avgDataSize,
	}
}

func measureMemoryUsage(serializers map[string]Serializer, testData []*models.User) {
	fmt.Println("\n=== Memory Usage Comparison ===")

	var memStats runtime.MemStats

	for name, serializer := range serializers {
		// Сбор мусора перед измерением
		runtime.GC()
		debug.FreeOSMemory()

		runtime.ReadMemStats(&memStats)
		memoryBefore := memStats.Alloc

		// Сериализация данных
		serializedData := make([][]byte, len(testData))
		for i, user := range testData {
			data, err := serializer.Serialize(user)
			if err != nil {
				continue
			}
			serializedData[i] = data
		}

		runtime.GC()
		runtime.ReadMemStats(&memStats)
		memoryAfter := memStats.Alloc

		memoryUsed := memoryAfter - memoryBefore
		fmt.Printf("%s Memory Used: %d bytes\n", name, memoryUsed)

		// Очистка
		serializedData = nil
		runtime.GC()
	}
}

func measureThroughput(serializers map[string]Serializer, testData []*models.User) {
	fmt.Println("\n=== Throughput Test ===")
	throughputIterations := 10000
	testSubset := testData[:100]

	for name, serializer := range serializers {
		start := time.Now()

		for i := 0; i < throughputIterations; i++ {
			for _, user := range testSubset {
				serializer.Serialize(user)
			}
		}

		elapsed := time.Since(start)
		throughput := float64(throughputIterations*len(testSubset)) / elapsed.Seconds()

		fmt.Printf("%s Throughput: %.2f ops/sec\n", name, throughput)
	}
}

func runCPUBenchmark(serializers map[string]Serializer, testData []*models.User) {
	fmt.Println("\n=== CPU Utilization Test ===")

	for name, serializer := range serializers {
		start := time.Now()

		// Интенсивная нагрузка для измерения использования CPU
		for i := 0; i < 100000; i++ {
			for _, user := range testData[:10] {
				serializer.Serialize(user)
			}
		}

		elapsed := time.Since(start)
		fmt.Printf("%s CPU Time: %v\n", name, elapsed)
	}
}

func main() {
	fmt.Println("Starting Go Serialization Benchmark...")
	fmt.Printf("Warmup iterations: %d\n", warmupIterations)
	fmt.Printf("Measurement iterations: %d\n", measurementIterations)
	fmt.Printf("Data size: %d objects\n\n", dataSize)

	// Генерация тестовых данных
	testData := generateTestData(dataSize)

	// Инициализация сериализаторов
	serializers := map[string]Serializer{
		"JSON":        &serializers.JSONSerializer{},
		"XML":         &serializers.XMLSerializer{},
		"Protobuf":    &serializers.ProtobufSerializer{},
		"FlatBuffers": &serializers.FlatBuffersSerializer{},
	}

	// Прогрев
	performWarmup(serializers, testData)

	// Запуск бенчмарков
	results := make([]BenchmarkResult, 0, len(serializers))

	for name, serializer := range serializers {
		result := benchmarkFormat(name, serializer, testData)
		results = append(results, result)
	}

	// Дополнительные измерения
	measureMemoryUsage(serializers, testData)
	measureThroughput(serializers, testData)
	runCPUBenchmark(serializers, testData)

	// Сохранение результатов
	saveResults(results)
}

func saveResults(results []BenchmarkResult) {
	jsonData, err := json.MarshalIndent(results, "", "  ")
	if err != nil {
		log.Printf("Error saving results: %v", err)
		return
	}

	fmt.Printf("\n=== Results Saved ===\n%s\n", string(jsonData))
}
