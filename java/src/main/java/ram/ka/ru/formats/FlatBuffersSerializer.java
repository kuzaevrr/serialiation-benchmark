package ram.ka.ru.formats;

import ram.ka.ru.models.User;
import ram.ka.ru.models.UserFlatBuffers;

import com.google.flatbuffers.FlatBufferBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;


public class FlatBuffersSerializer {
    
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
        int rolesVector = UserFlatBuffers.User.createRolesVector(builder, rolesOffsets);
        
        // Создаем объект User
        UserFlatBuffers.User.startUser(builder);
        UserFlatBuffers.User.addId(builder, idOffset);
        UserFlatBuffers.User.addName(builder, nameOffset);
        UserFlatBuffers.User.addEmail(builder, emailOffset);
        UserFlatBuffers.User.addAge(builder, user.getAge());
        UserFlatBuffers.User.addActive(builder, user.isActive());
        UserFlatBuffers.User.addRoles(builder, rolesVector);
        UserFlatBuffers.User.addBalance(builder, user.getBalance());
        
        int userOffset = UserFlatBuffers.User.endUser(builder);
        builder.finish(userOffset);
        
        return builder.sizedByteArray();
    }
    
    public User deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        UserFlatBuffers.User userFlat = UserFlatBuffers.User.getRootAsUser(buffer);
        
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