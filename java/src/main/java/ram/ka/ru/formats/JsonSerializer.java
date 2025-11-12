package ram.ka.ru.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import ram.ka.ru.models.User;

import java.io.IOException;

public class JsonSerializer {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public byte[] serialize(User user) throws IOException {
        long startTime = System.nanoTime();
        byte[] result = mapper.writeValueAsBytes(user);
        long endTime = System.nanoTime();
        return result;
    }
    
    public User deserialize(byte[] data) throws IOException {
        return mapper.readValue(data, User.class);
    }
    
    public long measureSerializationTime(User user, int iterations) throws IOException {
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            mapper.writeValueAsBytes(user);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        return totalTime / iterations;
    }
}