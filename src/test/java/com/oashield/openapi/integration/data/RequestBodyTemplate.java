package com.oashield.openapi.integration.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for JSON request body template with variable placeholders.
 * Holds immutable template content and thread-safe parameters.
 * Delegates processing to TemplateProcessor for variable substitution.
 */
public class RequestBodyTemplate {

    private static final Logger logger = LoggerFactory.getLogger(RequestBodyTemplate.class);

    private final String templateContent;
    private final Map<String, String> parameters;

    /**
     * Constructs a new RequestBodyTemplate with specified template content.
     *
     * @param templateContent the template content, must not be null
     */
    public RequestBodyTemplate(String templateContent) {
        this.templateContent = Objects.requireNonNull(templateContent, "templateContent must not be null");
        this.parameters = new ConcurrentHashMap<>();
    }

    /**
     * Constructs a new RequestBodyTemplate with specified template content and initial parameters.
     *
     * @param templateContent the template content, must not be null
     * @param parameters      the initial parameters map, must not be null
     */
    public RequestBodyTemplate(String templateContent, Map<String, String> parameters) {
        this.templateContent = Objects.requireNonNull(templateContent, "templateContent must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        this.parameters = new ConcurrentHashMap<>(parameters);
    }

    /**
     * Sets or updates a parameter for substitution.
     *
     * @param key   parameter key, must not be null
     * @param value parameter value, must not be null
     */
    public void setParameter(String key, String value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        parameters.put(key, value);
    }

    /**
     * Retrieves the parameter value for the specified key.
     *
     * @param key parameter key, must not be null
     * @return parameter value, or null if not present
     */
    public String getParameter(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return parameters.get(key);
    }

    /**
     * Returns an unmodifiable view of the parameters map.
     *
     * @return unmodifiable map of parameters
     */
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Processes the template content by substituting variables with current parameters.
     *
     * @return processed template string
     * @throws TemplateProcessingException if processing fails
     */
    public String process() {
        logger.debug("Processing template with parameters: {}", parameters);
        String result = TemplateProcessor.processTemplate(templateContent, parameters);
        logger.debug("Processed template result: {}", result);
        return result;
    }

    /**
     * Returns the original template content.
     *
     * @return template content
     */
    public String getTemplateContent() {
        return templateContent;
    }

    /**
     * Checks if the template contains any variable placeholders.
     *
     * @return true if placeholders are present, false otherwise
     */
    public boolean hasVariables() {
        return TemplateProcessor.hasVariables(templateContent);
    }

    /**
     * Returns the set of variable names required by the template.
     *
     * @return set of variable names
     */
    public Set<String> getRequiredVariables() {
        return TemplateProcessor.extractVariables(templateContent);
    }
}
