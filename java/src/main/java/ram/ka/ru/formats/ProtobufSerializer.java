
package ram.ka.ru.formats;

import ram.ka.ru.models.User;
import ram.ka.ru.models.UserProtos;

import java.io.IOException;

public class ProtobufSerializer implements UserSerializer {
    
    public byte[] serialize(User user) throws IOException {
        UserProtos.UserProto.Builder builder = UserProtos.UserProto.newBuilder()
            .setId(user.getId())
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setAge(user.getAge())
            .setActive(user.isActive())
            .setBalance(user.getBalance());
        
        for (String role : user.getRoles()) {
            builder.addRoles(role);
        }
        
        return builder.build().toByteArray();
    }
    
    public User deserialize(byte[] data) throws IOException {
        UserProtos.UserProto userProto = UserProtos.UserProto.parseFrom(data);
        
        User user = new User();
        user.setId(userProto.getId());
        user.setName(userProto.getName());
        user.setEmail(userProto.getEmail());
        user.setAge(userProto.getAge());
        user.setActive(userProto.getActive());
        user.setBalance(userProto.getBalance());
        user.setRoles(userProto.getRolesList());
        
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