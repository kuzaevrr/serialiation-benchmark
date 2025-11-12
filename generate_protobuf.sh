# Установка зависимостей для Java
cd java && mvn dependency:resolve && cd ../

# Установка зависимостей для Go
cd golang && go mod tidy && cd ../

# Генерация Protobuf кода
protoc --java_out=java/src/main/java java/src/main/java/ram/ka/ru/user.proto
protoc --go_out=golang golang/user.proto