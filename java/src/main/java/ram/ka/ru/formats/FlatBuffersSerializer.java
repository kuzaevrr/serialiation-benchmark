package ram.ka.ru.formats;

import ram.ka.ru.models.User;

import com.google.flatbuffers.FlatBufferBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;


public class FlatBuffersSerializer implements UserSerializer {
    
    public byte[] serialize(User user) throws IOException {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        
        // Создаем строки
        int idOffset = builder.createString(user.getId());
        int nameOffset = builder.createString(user.getName());
        int emailOffset = builder.createString(user.getEmail());
        
        // Создаем массив ролей
        int[] rolesOffsets = new int[user.getRoles().size()];
        for (int i = 0; i < user.getRoles().size(); i++) {
            rolesOffsets[i] = builder.createString(user.getRoles().get(i));
        }
        int rolesVector = ram.ka.ru.models.flatbuffers.User.createRolesVector(builder, rolesOffsets);

        // Создаем объект User
        ram.ka.ru.models.flatbuffers.User.startUser(builder);
        ram.ka.ru.models.flatbuffers.User.addId(builder, idOffset);
        ram.ka.ru.models.flatbuffers.User.addName(builder, nameOffset);
        ram.ka.ru.models.flatbuffers.User.addEmail(builder, emailOffset);
        ram.ka.ru.models.flatbuffers.User.addAge(builder, user.getAge());
        ram.ka.ru.models.flatbuffers.User.addActive(builder, user.isActive());
        ram.ka.ru.models.flatbuffers.User.addRoles(builder, rolesVector);
        ram.ka.ru.models.flatbuffers.User.addBalance(builder, user.getBalance());
        
        int userOffset = ram.ka.ru.models.flatbuffers.User.endUser(builder);
        builder.finish(userOffset);
        
        return builder.sizedByteArray();
    }
    
    public User deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        ram.ka.ru.models.flatbuffers.User userFlat = ram.ka.ru.models.flatbuffers.User.getRootAsUser(buffer);
        
        User user = new User();
        user.setId(userFlat.id());
        user.setName(userFlat.name());
        user.setEmail(userFlat.email());
        user.setAge(userFlat.age());
        user.setActive(userFlat.active());
        user.setBalance(userFlat.balance());
        
        // Восстанавливаем роли
        for (int i = 0; i < userFlat.rolesLength(); i++) {
            user.getRoles().add(userFlat.roles(i));
        }
        
        return user;
    }
    
    public long measureSerializationTime(User user, int iterations) throws IOException {
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            serialize(user);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        return totalTime / iterations;
    }
}