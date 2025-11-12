// serializers/xml_serializer.go
package serializers

import (
	"encoding/xml"
	"golang/models"
	"time"
)

type XMLSerializer struct{}

func (s *XMLSerializer) Serialize(user *models.User) ([]byte, error) {
	start := time.Now()
	data, err := xml.Marshal(user)
	_ = time.Since(start) // Можно сохранить для метрик
	return data, err
}

func (s *XMLSerializer) Deserialize(data []byte) (*models.User, error) {
	var user models.User
	err := xml.Unmarshal(data, &user)
	return &user, err
}

func (s *XMLSerializer) MeasureSerializationTime(user *models.User, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		xml.Marshal(user)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}

func (s *XMLSerializer) MeasureDeserializationTime(data []byte, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		var user models.User
		xml.Unmarshal(data, &user)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}
