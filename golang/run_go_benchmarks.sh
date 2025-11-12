#!/bin/bash
# scripts/run_go_benchmarks.sh

echo "Starting Go benchmarks..."
cd go

# Запуск бенчмарков
echo "Running serialization benchmarks..."
go run main.go serializers/*.go models/*.go

# Опционально: запуск встроенных бенчмарков Go
# go test -bench=. -benchmem ./...

echo "Go benchmarks completed!"