package flickr;

import com.flickr4java.flickr.util.IOUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration extends Properties {
    public Configuration() throws IOException {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/setup.properties");
            load(in);
        } finally {
            IOUtilities.close(in);
        }
    }
}
