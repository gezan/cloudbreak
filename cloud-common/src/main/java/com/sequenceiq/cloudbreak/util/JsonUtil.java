package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        MAPPER.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);
    }

    private JsonUtil() {
    }

    public static <T> T readValue(Reader reader, Class<T> valueType) throws IOException {
        return MAPPER.readValue(reader, valueType);
    }

    public static <T> T readValue(String content, Class<T> valueType) throws IOException {
        return MAPPER.readValue(content, valueType);
    }

    public static String writeValueAsString(Object object) throws JsonProcessingException {
        return MAPPER.writeValueAsString(object);
    }

    public static JsonNode readTree(String content) throws IOException {
        return MAPPER.readTree(content);
    }

    public static JsonNode createJsonTree(Map<String, Object> map) {
        ObjectNode rootNode = MAPPER.createObjectNode();
        map.forEach((key, value) -> rootNode.set(key, MAPPER.valueToTree(value)));
        return rootNode;
    }

    public static String minify(String content) {
        return minify(content, Collections.emptySet());
    }

    public static String minify(String content, Collection<String> toCleanup) {
        try {
            JsonNode node = Optional.ofNullable(readTree(content)).orElse(new ObjectNode(JsonNodeFactory.instance));
            if (!toCleanup.isEmpty() && node instanceof ObjectNode) {
                ((ObjectNode) node).remove(toCleanup);
            }
            return node.toString();
        } catch (IOException ignored) {
            return "INVALID_JSON_CONTENT";
        }
    }

    public static <T> T treeToValue(TreeNode n, Class<T> valueType) throws JsonProcessingException {
        return MAPPER.treeToValue(n, valueType);
    }

}
