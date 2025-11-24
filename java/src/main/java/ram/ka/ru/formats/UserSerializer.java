package ram.ka.ru.formats;

import ram.ka.ru.models.User;

import java.io.IOException;

/**
 * Универсальный интерфейс для всех сериализаторов
 * Устраняет необходимость instanceof и приводит типы
 */
public interface UserSerializer {
    byte[] serialize(User user) throws IOException;
    User deserialize(byte[] data) throws IOException;
}

