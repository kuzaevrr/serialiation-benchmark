#!/bin/bash
# scripts/run_go_benchmarks.sh

echo "Starting Go benchmarks..."

# Запуск бенчмарков
echo "Running serialization benchmarks..."
go run main.go serializers/*.go models/*.go

# Опционально: запуск встроенных бенчмарков Go
# go test -bench=. -benchmem ./... todo

echo "Go benchmarks completed!"