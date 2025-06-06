package weather.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LargeDataProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Stream<ObjectNode> streamJsonArrayFile(Path jsonFilePath) throws IOException {
        JsonParser parser = new JsonFactory().createParser(Files.newInputStream(jsonFilePath));

        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("Expected JSON array at the root level");
        }

        Iterator<ObjectNode> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                try {
                    return parser.nextToken() == JsonToken.START_OBJECT;
                } catch (IOException e) {
                    throw new RuntimeException("Error reading JSON", e);
                }
            }

            @Override
            public ObjectNode next() {
                try {
                    return objectMapper.readTree(parser);
                } catch (IOException e) {
                    throw new RuntimeException("Error parsing JSON object", e);
                }
            }
        };
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).onClose(() -> {
            try {
                parser.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing parser", e);
            }
        });
    }
}
