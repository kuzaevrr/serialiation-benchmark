// XmlSerializer.java
package ram.ka.ru.formats;

import ram.ka.ru.models.User;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class XmlSerializer {
    private final JAXBContext jaxbContext;
    
    public XmlSerializer() {
        try {
            jaxbContext = JAXBContext.newInstance(User.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to create JAXB context", e);
        }
    }
    
    public byte[] serialize(User user) throws IOException {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshaller.marshal(user, outputStream);
            return outputStream.toByteArray();
        } catch (JAXBException e) {
            throw new IOException("XML serialization failed", e);
        }
    }
    
    public User deserialize(byte[] data) throws IOException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return (User) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new IOException("XML deserialization failed", e);
        }
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