package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for processing operations in the Modsecurity3Generator.
 * This class encapsulates operation processing logic extracted from Modsecurity3Generator.
 */
@RequiredArgsConstructor
public class OperationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationProcessor.class);

    // Constants
    private static final String MODSECURITY_INDEX_KEY = "x-codegen-globalIndex";
    private static final int MODSECURITY_INDEX_MAX = 20;
    private static final String MODSECURITY_PATH_REGEX_KEY = "x-codegen-pathRegex";
    private static final String VENDOR_EXTENSIONS_KEY = "vendorExtensions";
    private static final String MODSECURITY_HAS_ARRAY_MIN = "x-codegen-hasArrayMin";
    private static final String MODSECURITY_HAS_ARRAY_MAX = "x-codegen-hasArrayMax";
    private static final String MODSECURITY_HAS_JSON = "x-codegen-isJson";
    private static final String MODSECURITY_HAS_XML = "x-codegen-isXml";
    private static final String MODSECURITY_MODEL_PROPERTIES = "x-codegen-modelProperties";

    // Service components
    private final PatternGenerationService patternGenerationService;
    private final ConfigurationManager configManager;

    // Operational fields
    @Getter @Setter
    protected Long globalIndex = 4200001L; // Default start
    @Getter @Setter
    protected Long globalParamIndex = 4210001L; // Default start

    /**
     * Constructor for OperationProcessor.
     * Created by Lombok @RequiredArgsConstructor which creates a constructor
     * with all final fields as parameters.
     */

    /**
     * Provides an opportunity to inspect and modify operation data before the code
     * is generated.
     *
     * @param objs The operations map to process
     * @param allModels The list of all models
     * @return The processed operations map
     */
    public OperationsMap processOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        LOGGER.debug("Processing operations with models");

        OperationMap ops = objs.getOperations();
        List<CodegenOperation> opList = ops.getOperation();

        // Process each operation
        for (CodegenOperation co : opList) {
            processOperation(co);
            processOperationParameters(co);
        }

        // Add global vendor extensions
        Map<String, Object> vendorExtensions = new HashMap<String, Object>();
        vendorExtensions.put(MODSECURITY_INDEX_KEY, globalIndex++);
        objs.put(VENDOR_EXTENSIONS_KEY, vendorExtensions);

        return objs;
    }

    /**
     * Process a single operation, adding necessary vendor extensions.
     *
     * @param co The CodegenOperation to process
     */
    private void processOperation(CodegenOperation co) {
        // Process path and add path regex
        String path = co.path;
        String matchPath = path.replaceAll("\\{.*?\\}", "[^/]+");
        co.vendorExtensions.put(MODSECURITY_PATH_REGEX_KEY, matchPath);

        // Add global indices
        for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
            co.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalIndex++);
        }

        LOGGER.debug("Processing operation: {}", co.operationId);

        // Add validateBodySchema as a vendor extension to the operation
        co.vendorExtensions.put("validateBodySchema", configManager.isValidateBodySchema());

        // Process content types
        processContentTypes(co);
    }

    /**
     * Process content types for an operation.
     *
     * @param co The CodegenOperation to process
     */
    private void processContentTypes(CodegenOperation co) {
        Boolean includeRequestJSON = false;
        Boolean includeRequestXML = false;

        if(co.hasConsumes) {
            LOGGER.debug("Operation: {} Consumes: {}", co.baseName, co.consumes);
            // Check if the operation consumes JSON or XML
            for (Map<String, String> consume : co.consumes) {
                if (consume.containsKey("isJson")) {
                    String isJsonString = consume.get("isJson");
                    includeRequestJSON = isJsonString != null && isJsonString.equals("true");
                }
                if (consume.containsKey("isXml")) {
                    String isXmlString = consume.get("isXml");
                    includeRequestXML = isXmlString != null && isXmlString.equals("true");
                }
            }
        }

        // Add vendor extension for JSON and XML
        co.vendorExtensions.put(MODSECURITY_HAS_JSON, includeRequestJSON);
        co.vendorExtensions.put(MODSECURITY_HAS_XML, includeRequestXML);
    }

    /**
     * Process parameters for an operation.
     *
     * @param co The CodegenOperation containing parameters to process
     */
    private void processOperationParameters(CodegenOperation co) {
        // Loop through parameters and process each one
        for (CodegenParameter param : co.allParams) {
            processParameter(param);
        }
    }

    /**
     * Process a single parameter, adding necessary vendor extensions and patterns.
     *
     * @param param The CodegenParameter to process
     */
    private void processParameter(CodegenParameter param) {
        // Handle required arrays
        if (param.required && param.isArray && (param.getMinItems() == null || param.getMinItems() == 0)) {
            LOGGER.debug("Required array parameter: {}", param.baseName);
            param.setMinItems(1);
        }

        // Process model parameters
        if (param.isModel) {
            processModelParameter(param);
        }

        // Add vendor extensions for array constraints
        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MIN, (param.getMinItems() != null));
        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MAX, (param.getMaxItems() != null));

        // Add global indices
        for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
            param.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalParamIndex++);
        }

        // Process pattern
        processParameterPattern(param);
    }

    /**
     * Process a model parameter by flattening its properties.
     *
     * @param param The model parameter to process
     */
    private void processModelParameter(CodegenParameter param) {
        LOGGER.debug("Model parameter: {}", param.baseName);
        // We need to flatten the model into something that can be used in the template
        // This will be a new vendor extension with an array of properties that represent
        // the model
        List<CodegenProperty> flattenedProperties = new ArrayList<CodegenProperty>();
        for (CodegenProperty prop : param.vars) {
            // We need to create a new CodegenParameter for each property
            // Unless the property is a model, then we need to flatten that model
            // into properties
            List<CodegenProperty> properties = flattenModel(prop, param.baseName + ".");
            flattenedProperties.addAll(properties);
        }

        // Add the flattened properties to the parameter
        param.vendorExtensions.put(MODSECURITY_MODEL_PROPERTIES, flattenedProperties);
    }

    /**
     * Process the pattern for a parameter.
     *
     * @param param The parameter to process
     */
    private void processParameterPattern(CodegenParameter param) {
        String patternString = param.pattern;

        if(patternString != null && !patternString.isEmpty()) {
            LOGGER.debug("Config pattern string used: {}", patternString);
            if(isInvalidPattern(patternString)) {
                LOGGER.warn("Invalid pattern string: {}", patternString);
                patternString = null;
            }
        }

        if(patternString == null || patternString.isEmpty()) {
            patternString = patternGenerationService.getParamPattern(param);
            LOGGER.debug("Calculated pattern string {}", patternString);
            param.setPattern(patternString);
        }

        LOGGER.debug("param: {}, validation: {}, pattern: {}", param.hasValidation, param.pattern);
        LOGGER.debug("Parameter: {}, data type: {}, isString: {}, max length: {}", param.baseName, param.getDataType(),
            param.isString, param.getMaxLength());
    }

    /**
     * Helper method to flatten a model property into a list of properties.
     *
     * @param currentProperty The property to flatten
     * @param baseNamePrefix The prefix to add to the property name
     * @return A list of flattened properties
     */
    public List<CodegenProperty> flattenModel(CodegenProperty currentProperty, String baseNamePrefix) {
        List<CodegenProperty> properties = new ArrayList<CodegenProperty>();

        // handle array of primitives as single property
        if (currentProperty.isArray && currentProperty.vars != null && !currentProperty.vars.isEmpty() && currentProperty.vars.get(0).isPrimitiveType) {
            currentProperty.baseName = baseNamePrefix + currentProperty.baseName;
            properties.add(currentProperty);
            return properties;
        }
        // 1. The property is a model
        if(currentProperty.isModel) {
            // Recursively flatten the model
            LOGGER.debug("Flattening model property: {}", currentProperty.baseName);
            baseNamePrefix += currentProperty.baseName + ".";
            for(CodegenProperty prop : currentProperty.vars) {
                List<CodegenProperty> flattenedProperties = flattenModel(prop, baseNamePrefix);
                properties.addAll(flattenedProperties);
            }
        }

        // 2. The property is an array of models
        else if(currentProperty.isArray) {
            LOGGER.debug("Flattening array of model property: {}", currentProperty.baseName);
            int i = 0;
            for(CodegenProperty prop : currentProperty.vars) {
                List<CodegenProperty> flattenedProperties = flattenModel(prop, baseNamePrefix + currentProperty.baseName + "." + i + ".");
                i++;
                properties.addAll(flattenedProperties);
            }
        }

        // 3. The property is a primitive type
        else {
            LOGGER.debug("Adding property: {}", currentProperty.baseName);
            // Add the baseNamePrefix to the property
            currentProperty.baseName = baseNamePrefix + currentProperty.baseName;
            properties.add(currentProperty);
        }

        return properties;
    }

    /**
     * Checks if a pattern string contains invalid regex constructs.
     *
     * @param patternString The pattern string to check
     * @return true if the pattern is invalid, false otherwise
     */
    public boolean isInvalidPattern(String patternString) {
        // This is a very basic check, and should be improved.
        return patternString.contains("(?!") ||
        patternString.contains("(?=") ||
        patternString.contains("(?<=") ||
        patternString.contains("(?<!");
    }
}
