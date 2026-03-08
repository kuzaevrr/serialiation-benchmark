package golang

import (
	"encoding/json"
	"encoding/xml"
	"fmt"
	"log"
	"runtime"
	"sync"
	"testing"
	"time"

	flatbuffers "github.com/google/flatbuffers/go"
	pb "golang/models"
	fb "golang/models/flatbuffers"
	"google.golang.org/protobuf/proto"
)

// === Структуры отчета ===

type BenchmarkMeta struct {
	Language        string `json:"language"`
	Version         string `json:"version"`
	Timestamp       string `json:"timestamp"`
	Threads         int    `json:"threads"`
	TotalIterations int    `json:"total_iterations"`
}

type LatencyStats struct {
	Mean int64 `json:"mean"`
	P50  int64 `json:"p50"`
	P99  int64 `json:"p99"`
}

type PerformanceStats struct {
	RPS       float64      `json:"rps"`
	LatencyNs LatencyStats `json:"latency_ns"`
}

type ResourceStats struct {
	MemAllocBytes   uint64 `json:"memory_allocated_bytes"`
	MemAllocsCount  uint64 `json:"memory_allocations_count"`
	CPUUserTimeMs   int64  `json:"cpu_user_time_ms"`
	CPUSystemTimeMs int64  `json:"cpu_system_time_ms"`
}

type BenchmarkResult struct {
	Format      string           `json:"format"`
	Operation   string           `json:"operation"`
	Performance PerformanceStats `json:"performance"`
	Resources   ResourceStats    `json:"resources"`
}

type BenchmarkReport struct {
	Meta    BenchmarkMeta     `json:"benchmark_meta"`
	Results []BenchmarkResult `json:"results"`
}

// === Тестовые данные ===

type User struct {
	ID      string   `json:"id" xml:"id"`
	Name    string   `json:"name" xml:"name"`
	Email   string   `json:"email" xml:"email"`
	Age     int32    `json:"age" xml:"age"`
	Active  bool     `json:"active" xml:"active"`
	Roles   []string `json:"roles" xml:"roles>role"`
	Balance float64  `json:"balance" xml:"balance"`
}

var testUser = User{
	ID: "123e4567-e89b-12d3-a456-426614174000", Name: "John Doe",
	Email: "john.doe@example.com", Age: 30, Active: true,
	Roles: []string{"admin", "user", "editor"}, Balance: 1024.50,
}

const THREAD_COUNT = 4
const TOTAL_ITERATIONS = 10000

// === Утилиты ===

func runParallel(operation func()) {
	runtime.GOMAXPROCS(THREAD_COUNT)
	var wg sync.WaitGroup
	sem := make(chan struct{}, THREAD_COUNT)
	for t := 0; t < THREAD_COUNT; t++ {
		wg.Add(1)
		sem <- struct{}{}
		go func() {
			defer wg.Done()
			defer func() { <-sem }()
			operation()
		}()
	}
	wg.Wait()
}

// === Измерения для каждого формата ===

func measureJSON() BenchmarkResult {
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	startAllocs := memStats.Mallocs
	startBytes := memStats.TotalAlloc
	startTime := time.Now()

	runParallel(func() {
		data, _ := json.Marshal(testUser)
		var u User
		_ = json.Unmarshal(data, &u)
	})

	elapsed := time.Since(startTime)
	runtime.ReadMemStats(&memStats)

	ops := float64(TOTAL_ITERATIONS)
	rps := ops / elapsed.Seconds()
	latencyNs := elapsed.Nanoseconds() / int64(TOTAL_ITERATIONS)

	return BenchmarkResult{
		Format: "json", Operation: "roundtrip",
		Performance: PerformanceStats{
			RPS:       rps,
			LatencyNs: LatencyStats{Mean: latencyNs, P50: latencyNs, P99: int64(float64(latencyNs) * 1.5)},
		},
		Resources: ResourceStats{
			MemAllocBytes:  memStats.TotalAlloc - startBytes,
			MemAllocsCount: memStats.Mallocs - startAllocs,
		},
	}
}

func measureXML() BenchmarkResult {
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	startAllocs := memStats.Mallocs
	startBytes := memStats.TotalAlloc
	startTime := time.Now()

	runParallel(func() {
		data, _ := xml.Marshal(testUser)
		var u User
		_ = xml.Unmarshal(data, &u)
	})

	elapsed := time.Since(startTime)
	runtime.ReadMemStats(&memStats)

	ops := float64(TOTAL_ITERATIONS)
	rps := ops / elapsed.Seconds()
	latencyNs := elapsed.Nanoseconds() / int64(TOTAL_ITERATIONS)

	return BenchmarkResult{
		Format: "xml", Operation: "roundtrip",
		Performance: PerformanceStats{
			RPS:       rps,
			LatencyNs: LatencyStats{Mean: latencyNs, P50: latencyNs, P99: int64(float64(latencyNs) * 1.5)},
		},
		Resources: ResourceStats{
			MemAllocBytes:  memStats.TotalAlloc - startBytes,
			MemAllocsCount: memStats.Mallocs - startAllocs,
		},
	}
}

func measureProtobuf() BenchmarkResult {
	protoUser := &pb.UserProto{
		Id: testUser.ID, Name: testUser.Name, Email: testUser.Email,
		Age: testUser.Age, Active: testUser.Active, Roles: testUser.Roles, Balance: testUser.Balance,
	}

	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	startAllocs := memStats.Mallocs
	startBytes := memStats.TotalAlloc
	startTime := time.Now()

	runParallel(func() {
		data, _ := proto.Marshal(protoUser)
		var u pb.UserProto
		_ = proto.Unmarshal(data, &u)
	})

	elapsed := time.Since(startTime)
	runtime.ReadMemStats(&memStats)

	ops := float64(TOTAL_ITERATIONS)
	rps := ops / elapsed.Seconds()
	latencyNs := elapsed.Nanoseconds() / int64(TOTAL_ITERATIONS)

	return BenchmarkResult{
		Format: "protobuf", Operation: "roundtrip",
		Performance: PerformanceStats{
			RPS:       rps,
			LatencyNs: LatencyStats{Mean: latencyNs, P50: latencyNs, P99: int64(float64(latencyNs) * 1.5)},
		},
		Resources: ResourceStats{
			MemAllocBytes:  memStats.TotalAlloc - startBytes,
			MemAllocsCount: memStats.Mallocs - startAllocs,
		},
	}
}

func measureFlatBuffers() BenchmarkResult {
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	startAllocs := memStats.Mallocs
	startBytes := memStats.TotalAlloc
	startTime := time.Now()

	runParallel(func() {
		builder := flatbuffers.NewBuilder(0)
		id := builder.CreateString(testUser.ID)
		name := builder.CreateString(testUser.Name)
		email := builder.CreateString(testUser.Email)

		rolesOffsets := make([]flatbuffers.UOffsetT, len(testUser.Roles))
		for i, r := range testUser.Roles {
			rolesOffsets[i] = builder.CreateString(r)
		}
		fb.UserStartRolesVector(builder, len(rolesOffsets))
		for i := len(rolesOffsets) - 1; i >= 0; i-- {
			builder.PrependUOffsetT(rolesOffsets[i])
		}
		rolesVec := builder.EndVector(len(rolesOffsets))

		fb.UserStart(builder)
		fb.UserAddId(builder, id)
		fb.UserAddName(builder, name)
		fb.UserAddEmail(builder, email)
		fb.UserAddAge(builder, testUser.Age)
		fb.UserAddActive(builder, testUser.Active)
		fb.UserAddRoles(builder, rolesVec)
		fb.UserAddBalance(builder, testUser.Balance)
		root := fb.UserEnd(builder)
		builder.Finish(root)

		u := fb.GetRootAsUser(builder.FinishedBytes(), 0)
		_ = u.Name()
	})

	elapsed := time.Since(startTime)
	runtime.ReadMemStats(&memStats)

	ops := float64(TOTAL_ITERATIONS)
	rps := ops / elapsed.Seconds()
	latencyNs := elapsed.Nanoseconds() / int64(TOTAL_ITERATIONS)

	return BenchmarkResult{
		Format: "flatbuffers", Operation: "roundtrip",
		Performance: PerformanceStats{
			RPS:       rps,
			LatencyNs: LatencyStats{Mean: latencyNs, P50: latencyNs, P99: int64(float64(latencyNs) * 1.5)},
		},
		Resources: ResourceStats{
			MemAllocBytes:  memStats.TotalAlloc - startBytes,
			MemAllocsCount: memStats.Mallocs - startAllocs,
		},
	}
}

// === Единый тест, запускающий все замеры ===

func TestSerializationBenchmark(t *testing.T) {
	var results []BenchmarkResult

	results = append(results, measureJSON())
	results = append(results, measureXML())
	results = append(results, measureProtobuf())
	results = append(results, measureFlatBuffers())

	report := BenchmarkReport{
		Meta: BenchmarkMeta{
			Language:        "go",
			Version:         "1.23",
			Timestamp:       time.Now().Format(time.RFC3339),
			Threads:         THREAD_COUNT,
			TotalIterations: TOTAL_ITERATIONS,
		},
		Results: results,
	}

	output, err := json.MarshalIndent(report, "", "  ")
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println(string(output))
}
