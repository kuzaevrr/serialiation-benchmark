// serializers/json_serializer.go
package serializers

import (
	"encoding/json"
	"golang/models"
	"time"
)

type JSONSerializer struct{}

func (s *JSONSerializer) Serialize(user *models.User) ([]byte, error) {
	start := time.Now()
	data, err := json.Marshal(user)
	_ = time.Since(start) // Можно сохранить для метрик
	return data, err
}

func (s *JSONSerializer) Deserialize(data []byte) (*models.User, error) {
	var user models.User
	err := json.Unmarshal(data, &user)
	return &user, err
}

func (s *JSONSerializer) MeasureSerializationTime(user *models.User, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		json.Marshal(user)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}

func (s *JSONSerializer) MeasureDeserializationTime(data []byte, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		var user models.User
		json.Unmarshal(data, &user)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}
