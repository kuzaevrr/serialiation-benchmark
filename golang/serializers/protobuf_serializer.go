// serializers/protobuf_serializer.go
package serializers

import (
	"golang/models"
	"time"

	"google.golang.org/protobuf/proto"
)

type ProtobufSerializer struct{}

func (s *ProtobufSerializer) Serialize(user *models.User) ([]byte, error) {
	userProto := &models.UserProto{
		Id:      user.ID,
		Name:    user.Name,
		Email:   user.Email,
		Age:     int32(user.Age),
		Active:  user.Active,
		Roles:   user.Roles,
		Balance: user.Balance,
	}

	return proto.Marshal(userProto)
}

func (s *ProtobufSerializer) Deserialize(data []byte) (*models.User, error) {
	var userProto models.UserProto
	if err := proto.Unmarshal(data, &userProto); err != nil {
		return nil, err
	}

	user := &models.User{
		ID:      userProto.Id,
		Name:    userProto.Name,
		Email:   userProto.Email,
		Age:     int(userProto.Age),
		Active:  userProto.Active,
		Roles:   userProto.Roles,
		Balance: userProto.Balance,
	}

	return user, nil
}

func (s *ProtobufSerializer) MeasureSerializationTime(user *models.User, iterations int) time.Duration {
	var totalTime time.Duration

	userProto := &models.UserProto{
		Id:      user.ID,
		Name:    user.Name,
		Email:   user.Email,
		Age:     int32(user.Age),
		Active:  user.Active,
		Roles:   user.Roles,
		Balance: user.Balance,
	}

	for i := 0; i < iterations; i++ {
		start := time.Now()
		proto.Marshal(userProto)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}
