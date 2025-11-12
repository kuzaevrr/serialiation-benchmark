#!/bin/bash
# scripts/run_full_experiment.sh

echo "Starting full serialization experiment..."

# Генерация кода
./generate_flatbuffers.sh
./generate_protobuf.sh


# Запуск Java бенчмарков
./java/run_java_benchmarks.sh

# Запуск Go бенчмарков
./golang/run_go_benchmarks.sh

# Анализ результатов
echo "Analyzing results..."
go run analysis/analyzer.go

echo "Experiment completed! Check results/ directory for outputs."