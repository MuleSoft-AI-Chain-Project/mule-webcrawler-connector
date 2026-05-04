package org.mule.extension.webcrawler.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;

class JSONUtilsTest {

    @Test
    void convertToJSONSerializesMap() throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "webcrawler");
        map.put("count", 42);

        String json = JSONUtils.convertToJSON(map);
        assertTrue(json.contains("\"name\":\"webcrawler\""));
        assertTrue(json.contains("\"count\":42"));
    }

    @Test
    void convertToJSONSerializesSimpleString() throws JsonProcessingException {
        assertEquals("\"hello\"", JSONUtils.convertToJSON("hello"));
    }

    @Test
    void convertToJSONSerializesNumber() throws JsonProcessingException {
        assertEquals("123", JSONUtils.convertToJSON(123));
    }

    @Test
    void convertToJSONExcludeEmptyTrueOmitsEmptyCollectionsAndNulls() throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "webcrawler");
        map.put("emptyList", new java.util.ArrayList<>());
        map.put("emptyMap", new HashMap<>());
        map.put("nullVal", null);

        String json = JSONUtils.convertToJSON(map, true);
        assertTrue(json.contains("\"name\":\"webcrawler\""));
        // Empty / null fields dropped under NON_EMPTY inclusion.
        assertFalse(json.contains("emptyList"), "Expected emptyList to be dropped; got: " + json);
        assertFalse(json.contains("emptyMap"), "Expected emptyMap to be dropped; got: " + json);
        assertFalse(json.contains("nullVal"), "Expected nullVal to be dropped; got: " + json);
    }

    @Test
    void convertToJSONExcludeEmptyFalseIncludesAll() throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "webcrawler");
        map.put("nullVal", null);

        String json = JSONUtils.convertToJSON(map, false);
        assertTrue(json.contains("\"name\":\"webcrawler\""));
        assertTrue(json.contains("nullVal"));
    }
}
