#!/bin/bash
# scripts/generate_flatbuffers.sh

echo "Generating FlatBuffers code..."

# Генерация для Java
echo "Generating Java code..."
flatc --java -o java/src/main/java java/src/main/java/ram/ka/ru/user.fbs

# Генерация для Go
echo "Generating Go code..."
flatc --go -o golang golang/user.fbs

echo "FlatBuffers code generation completed!"