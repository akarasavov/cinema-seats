package dev.avyguzov.service;

import dev.avyguzov.db.Database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

public class MessageService {
    public final Properties properties = new Properties();

    public MessageService(String resourceName) throws IOException {
        String pathToProps = getPathToTheFile(resourceName);

        try (final FileInputStream fis = new FileInputStream(pathToProps)) {
            properties.load(fis);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static String getPathToTheFile(String fileName) throws FileNotFoundException {
        return Optional.ofNullable(Database.class.getClassLoader().getResource(fileName))
                .map(URL::getPath)
                .orElseThrow(FileNotFoundException::new);
    }

    public String getProperty(String propKey) {
        return properties.getProperty(propKey);
    }
}
