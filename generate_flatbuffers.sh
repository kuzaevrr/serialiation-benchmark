#!/bin/bash
# scripts/generate_flatbuffers.sh

echo "Generating FlatBuffers code..."

# Генерация для Java
echo "Generating Java code..."
flatc --java -o java/src/main/java src/main/flatbuffers/user.fbs

# Генерация для Go
echo "Generating Go code..."
flatc --go -o go/models go/models/user.fbs

echo "FlatBuffers code generation completed!"