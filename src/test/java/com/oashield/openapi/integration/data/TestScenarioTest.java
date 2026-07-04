package com.oashield.openapi.integration.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.oashield.openapi.integration.data.TestScenario.ScenarioType;

/**
 * Unit tests for TestScenario data container.
 * <p>
 * Tests include:
 * - Constructor and getters
 * - Null argument validations
 * - Enum value coverage
 * - equals(), hashCode(), and toString consistency
 * - Collection behavior in sets and maps
 * </p>
 */
class TestScenarioTest {

    @Test
    void testConstructorAndGetters() {
        TestScenario ts = new TestScenario("name", ScenarioType.VALID, "desc");
        assertEquals("name", ts.getName());
        assertEquals(ScenarioType.VALID, ts.getType());
        assertEquals("desc", ts.getDescription());
    }

    @Test
    void testConstructorNullName() {
        assertThrows(NullPointerException.class,
                () -> new TestScenario(null, ScenarioType.VALID, "desc"));
    }

    @Test
    void testConstructorNullType() {
        assertThrows(NullPointerException.class,
                () -> new TestScenario("name", null, "desc"));
    }

    @Test
    void testConstructorNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new TestScenario("name", ScenarioType.VALID, null));
    }

    @Test
    void testEnumValues() {
        ScenarioType[] types = ScenarioType.values();
        List<ScenarioType> list = Arrays.asList(types);
        assertTrue(list.containsAll(Arrays.asList(ScenarioType.VALID, ScenarioType.INVALID)));
    }

    @Test
    void testEqualsAndHashCode() {
        TestScenario ts1 = new TestScenario("n", ScenarioType.INVALID, "d");
        TestScenario ts2 = new TestScenario("n", ScenarioType.INVALID, "d");
        assertEquals(ts1, ts2);
        assertEquals(ts1.hashCode(), ts2.hashCode());
        TestScenario ts3 = new TestScenario("n2", ScenarioType.INVALID, "d");
        assertNotEquals(ts1, ts3);
    }

    @Test
    void testToString() {
        TestScenario ts = new TestScenario("n", ScenarioType.VALID, "d");
        String str = ts.toString();
        assertTrue(str.contains("name='n'"));
        assertTrue(str.contains("type=VALID"));
        assertTrue(str.contains("description='d'"));
    }

    @Test
    void testCollectionsBehavior() {
        TestScenario ts1 = new TestScenario("n", ScenarioType.VALID, "d");
        TestScenario ts2 = new TestScenario("n", ScenarioType.VALID, "d");
        Set<TestScenario> set = new HashSet<>();
        set.add(ts1);
        set.add(ts2);
        assertEquals(1, set.size());
        Map<TestScenario, String> map = new HashMap<>();
        map.put(ts1, "first");
        map.put(ts2, "second");
        assertEquals(1, map.size());
        assertEquals("second", map.get(ts1));
    }
}
