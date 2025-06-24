package com.oashield.openapi.integration.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for processing templates with ${variable} placeholders.
 * Provides methods to extract variables, validate templates, and process
 * templates by substituting variables with provided parameters.
 */
public final class TemplateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TemplateProcessor.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private TemplateProcessor() {
        // Prevent instantiation
    }

    /**
     * Processes the given template by replacing all ${variable} placeholders
     * with corresponding values from the parameters map.
     *
     * @param template   the template string containing placeholders
     * @param parameters map of variable names to their replacement values
     * @return the processed template with all placeholders replaced
     * @throws TemplateProcessingException if template or parameters is null,
     *                                     or if any placeholders are missing in the parameters map
     */
    public static String processTemplate(String template, Map<String, String> parameters) {
        if (template == null) {
            logger.error("Template is null");
            throw new TemplateProcessingException("Template cannot be null");
        }
        if (parameters == null) {
            logger.error("Parameters map is null");
            throw new TemplateProcessingException("Parameters map cannot be null");
        }

        validateTemplate(template, parameters);

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = parameters.get(varName);
            if (replacement == null) {
                replacement = "";
                logger.debug("Null replacement for variable '{}', using empty string", varName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        logger.debug("Processed template result: {}", result);
        return result;
    }

    /**
     * Extracts all variable names from the template string.
     *
     * @param template the template string containing placeholders
     * @return a set of variable names found in the template;
     *         empty set if none or if template is null
     */
    public static Set<String> extractVariables(String template) {
        if (template == null) {
            return Collections.emptySet();
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        Set<String> variables = new HashSet<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(variables);
    }

    /**
     * Checks if the template contains any ${variable} placeholders.
     *
     * @param template the template string
     * @return true if at least one placeholder is found; false otherwise or if template is null
     */
    public static boolean hasVariables(String template) {
        if (template == null) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(template).find();
    }

    /**
     * Validates that all placeholders in the template have corresponding entries
     * in the parameters map.
     *
     * @param template   the template string containing placeholders
     * @param parameters map of variable names to their replacement values
     * @throws TemplateProcessingException if template or parameters is null,
     *                                     or if any placeholders are missing in the parameters map
     */
    public static void validateTemplate(String template, Map<String, String> parameters) {
        if (template == null) {
            logger.error("Template is null");
            throw new TemplateProcessingException("Template cannot be null");
        }
        if (parameters == null) {
            logger.error("Parameters map is null");
            throw new TemplateProcessingException("Parameters map cannot be null");
        }
        Set<String> vars = extractVariables(template);
        Set<String> missing = new HashSet<>();
        for (String varName : vars) {
            if (!parameters.containsKey(varName)) {
                missing.add(varName);
            }
        }
        if (!missing.isEmpty()) {
            String message = "Missing template parameters: " + missing;
            logger.error(message);
            throw new TemplateProcessingException(message);
        }
    }
}
