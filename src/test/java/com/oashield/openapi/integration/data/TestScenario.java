package com.oashield.openapi.integration.data;

import java.util.Objects;

/**
 * Immutable data container for test scenario information.
 */
public final class TestScenario {

    /**
     * Enumeration of test scenario types.
     */
    public enum ScenarioType {
        VALID,
        INVALID
    }

    private final String name;
    private final ScenarioType type;
    private final String description;

    /**
     * Constructs a new TestScenario.
     *
     * @param name        the name of the scenario, must not be null
     * @param type        the type of the scenario, must not be null
     * @param description the description of the scenario, must not be null
     */
    public TestScenario(String name, ScenarioType type, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    /**
     * Returns the scenario name.
     *
     * @return scenario name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the scenario type.
     *
     * @return scenario type
     */
    public ScenarioType getType() {
        return type;
    }

    /**
     * Returns the scenario description.
     *
     * @return scenario description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestScenario that = (TestScenario) o;
        return Objects.equals(name, that.name)
                && type == that.type
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, description);
    }

    @Override
    public String toString() {
        return "TestScenario{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }
}
