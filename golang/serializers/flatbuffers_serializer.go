package serializers

import (
	"golang/models"
	"golang/models/flatbuffers"
	"time"

	flatbufferslib "github.com/google/flatbuffers/go"
)

type FlatBuffersSerializer struct{}

func (s *FlatBuffersSerializer) Serialize(user *models.User) ([]byte, error) {
	builder := flatbufferslib.NewBuilder(1024)

	// Создаем строки
	idOffset := builder.CreateString(user.ID)
	nameOffset := builder.CreateString(user.Name)
	emailOffset := builder.CreateString(user.Email)

	// Создаем вектор для ролей
	rolesOffsets := make([]flatbufferslib.UOffsetT, len(user.Roles))
	for i, role := range user.Roles {
		rolesOffsets[i] = builder.CreateString(role)
	}
	flatbuffers.UserStartRolesVector(builder, len(rolesOffsets))
	for i := len(rolesOffsets) - 1; i >= 0; i-- {
		builder.PrependUOffsetT(rolesOffsets[i])
	}
	rolesVector := builder.EndVector(len(rolesOffsets))

	// Создаем объект User
	flatbuffers.UserStart(builder)
	flatbuffers.UserAddId(builder, idOffset)
	flatbuffers.UserAddName(builder, nameOffset)
	flatbuffers.UserAddEmail(builder, emailOffset)
	flatbuffers.UserAddAge(builder, user.Age)
	flatbuffers.UserAddActive(builder, user.Active)
	flatbuffers.UserAddRoles(builder, rolesVector)
	flatbuffers.UserAddBalance(builder, user.Balance)

	userOffset := flatbuffers.UserEnd(builder)
	builder.Finish(userOffset)

	return builder.FinishedBytes(), nil
}

func (s *FlatBuffersSerializer) Deserialize(data []byte) (*models.User, error) {
	userFlat := flatbuffers.GetRootAsUser(data, 0)

	// Восстанавливаем роли
	roles := make([]string, userFlat.RolesLength())
	for i := 0; i < userFlat.RolesLength(); i++ {
		roles[i] = string(userFlat.Roles(i))
	}

	user := &models.User{
		ID:      string(userFlat.Id()),
		Name:    string(userFlat.Name()),
		Email:   string(userFlat.Email()),
		Age:     userFlat.Age(),
		Active:  userFlat.Active(),
		Roles:   roles,
		Balance: userFlat.Balance(),
	}

	return user, nil
}

func (s *FlatBuffersSerializer) MeasureSerializationTime(user *models.User, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		s.Serialize(user)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}

func (s *FlatBuffersSerializer) MeasureDeserializationTime(data []byte, iterations int) time.Duration {
	var totalTime time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		s.Deserialize(data)
		totalTime += time.Since(start)
	}

	return totalTime / time.Duration(iterations)
}
