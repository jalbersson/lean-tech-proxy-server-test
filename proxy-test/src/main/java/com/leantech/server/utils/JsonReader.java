package com.leantech.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class JsonReader {
    private static ObjectMapper objectMapper = defaultObjectMapper();

    private static ObjectMapper defaultObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();

        // prevent that the deserialization process if one property is not found
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }

    public static JsonNode parse(String json) throws JsonProcessingException {
        return objectMapper.readTree(json);
    }

    public static <T> T fromJson(JsonNode jsonNode, Class<T> tClass) throws JsonProcessingException {
        return objectMapper.treeToValue(jsonNode, tClass);
    }

    public static JsonNode toJson(Object object){
        return objectMapper.valueToTree(object);
    }

    public static String stringify(JsonNode jsonNode) throws JsonProcessingException {
        return generateJson(jsonNode, false);
    }

    public static String stringifyIndented(JsonNode jsonNode) throws JsonProcessingException {
        return generateJson(jsonNode, true);
    }

    private static String generateJson(Object object, boolean indent) throws JsonProcessingException {
        ObjectWriter objectWriter = objectMapper.writer();
        if(indent)
            objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);
        return objectWriter.writeValueAsString(object);
    }
}
