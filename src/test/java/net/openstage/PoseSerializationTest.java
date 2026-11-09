package net.openstage;

import net.openstage.network.OpenStageNetwork;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PoseSerialization Adversarial QA and Parsing Tests")
class PoseSerializationTest {
    @Test
    @DisplayName("Empty and null pose strings parse to empty map")
    void testParseEmptyAndNullString() {
        Map<String, float[]> nullResult = OpenStageNetwork.parsePoseData(null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());
        Map<String, float[]> emptyResult = OpenStageNetwork.parsePoseData("");
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    @DisplayName("Malformed pose strings safely return empty map")
    void testParseMalformedStrings() {
        assertTrue(OpenStageNetwork.parsePoseData("===").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData(";;;").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("bone=").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("bone=1,2").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("bone=1,2,3,4").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("bone=abc,def,ghi").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("bone=NaN,Infinity,-Infinity").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData("=1.0,2.0,3.0").isEmpty());
        assertTrue(OpenStageNetwork.parsePoseData(" ; ; = ").isEmpty());
    }

    @Test
    @DisplayName("Partially malformed strings extract only valid entries")
    void testPartiallyMalformedString() {
        String data = "head=10.0,20.0,30.0;invalid=abc,1,2;broken=1,2;corrupt=NaN,0,0;body=5.0,-10.0,15.0";
        Map<String, float[]> result = OpenStageNetwork.parsePoseData(data);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("head"));
        assertTrue(result.containsKey("body"));
        assertArrayEquals(new float[]{10.0f, 20.0f, 30.0f}, result.get("head"), 1e-4f);
        assertArrayEquals(new float[]{5.0f, -10.0f, 15.0f}, result.get("body"), 1e-4f);
    }

    @Test
    @DisplayName("Valid multi-bone pose string parses correctly and roundtrips")
    void testValidMultiBonePoseRoundtrip() {
        Map<String, float[]> original = new LinkedHashMap<>();
        original.put("head", new float[]{15.5f, -20.25f, 45.0f});
        original.put("body", new float[]{0.0f, 10.0f, -5.5f});
        original.put("right_arm", new float[]{-90.0f, 0.0f, 15.0f});
        original.put("left_leg", new float[]{30.0f, -12.0f, 0.5f});
        String serialized = OpenStageNetwork.serializePoseData(original);
        assertNotNull(serialized);
        assertFalse(serialized.isEmpty());
        Map<String, float[]> parsed = OpenStageNetwork.parsePoseData(serialized);
        assertEquals(original.size(), parsed.size());

        for (Map.Entry<String, float[]> entry : original.entrySet()) {
            assertTrue(parsed.containsKey(entry.getKey()));
            float[] expected = entry.getValue();
            float[] actual = parsed.get(entry.getKey());
            assertEquals(expected[0], actual[0], 1e-4f);
            assertEquals(expected[1], actual[1], 1e-4f);
            assertEquals(expected[2], actual[2], 1e-4f);
        }

        String roundtripSerialized = OpenStageNetwork.serializePoseData(parsed);
        Map<String, float[]> roundtripParsed = OpenStageNetwork.parsePoseData(roundtripSerialized);
        assertEquals(parsed.size(), roundtripParsed.size());
    }

    @Test
    @DisplayName("serializePoseData handles empty and invalid maps gracefully")
    void testSerializeEmptyAndInvalidMaps() {
        assertEquals("", OpenStageNetwork.serializePoseData(null));
        assertEquals("", OpenStageNetwork.serializePoseData(Map.of()));
        Map<String, float[]> mapWithInvalid = new LinkedHashMap<>();
        mapWithInvalid.put("bad1", new float[]{Float.NaN, 0.0f, 0.0f});
        mapWithInvalid.put("bad2", new float[]{1.0f, Float.POSITIVE_INFINITY, 0.0f});
        mapWithInvalid.put("bad3", new float[]{1.0f});
        mapWithInvalid.put("", new float[]{1.0f, 2.0f, 3.0f});
        mapWithInvalid.put("valid", new float[]{1.0f, 2.0f, 3.0f});
        String serialized = OpenStageNetwork.serializePoseData(mapWithInvalid);
        assertEquals("valid=1.0,2.0,3.0", serialized);
    }
}

