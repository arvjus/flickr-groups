package flickr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;

public class JsonUtils {
    public static void writeToJson(String pathname, Object object) throws IOException {
        ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(pathname), object);
    }

    public static Object readFromJson(String pathname, Class clazz) throws IOException, IllegalAccessException, InstantiationException {
        if (!fileExists(pathname))
            return clazz.newInstance();
        return new ObjectMapper().readValue(new File(pathname), clazz);
    }

    public static boolean fileExists(String pathname) {
        return new File(pathname).exists();
    }
}
