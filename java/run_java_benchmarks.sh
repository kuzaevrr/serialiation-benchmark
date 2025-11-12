#!/bin/bash
# scripts/run_java_benchmarks.sh

echo "Starting Java benchmarks..."
cd java

# Компиляция проекта
mvn clean compile

# Запуск бенчмарков
echo "Running serialization benchmarks..."
mvn exec:java -Dexec.mainClass="benchmark.SerializationBenchmark"

# Опционально: запуск JMH бенчмарков (если настроены)
# mvn package
# java -jar target/benchmarks.jar

echo "Java benchmarks completed!"