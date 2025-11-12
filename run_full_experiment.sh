#!/bin/bash
# scripts/run_full_experiment.sh

echo "Starting full serialization experiment..."

# Генерация кода FlatBuffers
./scripts/generate_flatbuffers.sh

# Запуск Java бенчмарков
./scripts/run_java_benchmarks.sh

# Запуск Go бенчмарков
./scripts/run_go_benchmarks.sh

# Анализ результатов
echo "Analyzing results..."
go run analysis/analyzer.go

echo "Experiment completed! Check results/ directory for outputs."