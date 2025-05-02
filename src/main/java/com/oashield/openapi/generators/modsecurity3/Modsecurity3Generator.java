package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;

import java.util.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Generator for ModSecurity 3 configuration files.
 * This class has been refactored to use the OperationProcessor and ModelProcessor
 * classes for better separation of concerns.
 */
@Slf4j
public class Modsecurity3Generator extends DefaultCodegen {

    // Service components and managers
    private final PatternGenerationService patternGenerationService;
    private final ConfigurationManager configManager;
    private final TemplateManager templateManager;
    private final OperationProcessor operationProcessor;
    private final ModelProcessor modelProcessor;

    /**
     * Constructor for Modsecurity3Generator.
     * Initializes the generator with default settings.
     */
    public Modsecurity3Generator() {
        super();
        log.debug("Initializing Modsecurity3Generator");

        // Initialize type mappings for JSON Schema base types
        // This aligns with DefaultCodegen expectations, though JsonSchemaGenerator uses its own mapper.
        typeMapping = new HashMap<>();
        typeMapping.put("integer", "integer");
        typeMapping.put("int", "integer");
        typeMapping.put("long", "integer");
        typeMapping.put("float", "number");
        typeMapping.put("double", "number");
        typeMapping.put("number", "number");
        typeMapping.put("decimal", "number");
        typeMapping.put("boolean", "boolean");
        typeMapping.put("string", "string");
        typeMapping.put("date", "string"); // Base type is string
        typeMapping.put("date-time", "string"); // Base type is string
        typeMapping.put("DateTime", "string"); // Base type is string
        typeMapping.put("byte", "string"); // Base type is string
        typeMapping.put("binary", "string"); // Base type is string
        typeMapping.put("password", "string"); // Base type is string
        typeMapping.put("email", "string"); // Base type is string
        typeMapping.put("uuid", "string"); // Base type is string
        typeMapping.put("object", "object");
        typeMapping.put("array", "array");
        typeMapping.put("map", "object");

        // Initialize the pattern generation service
        this.patternGenerationService = new PatternGenerationService();

        // Initialize managers
        this.configManager = new ConfigurationManager(this);
        this.templateManager = new TemplateManager(this, configManager);

        // Initialize processors
        this.operationProcessor = new OperationProcessor(patternGenerationService, configManager);
        this.modelProcessor = new ModelProcessor(configManager);

        // Initialize configuration
        configManager.initialize();

        // Set template directory (must be done directly since it's protected in DefaultCodegen)
        this.templateDir = templateManager.getTemplateDir();

        // Initialize templates
        templateManager.initialize();

        // Initialize fields for backward compatibility with tests
        this.globalIndex = operationProcessor.getGlobalIndex();
        this.jsonSchemaOutputFile = configManager.getJsonSchemaOutputFile();
        this.outputFolder = configManager.getOutputFolder();

        log.debug("Modsecurity3Generator initialized with output folder: {}", configManager.getOutputFolder());
    }

    /**
     * Process the CLI options passed to the generator.
     */
    @Override
    public void processOpts() {
        super.processOpts();

        // Delegate to configuration manager
        configManager.processOpts();
    }

    //
    // Setup and Configuration Methods
    //

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see org.openapitools.codegen.CodegenType
     */
    public CodegenType getTag() {
        log.debug("Getting the generator tag");
        return CodegenType.OTHER;
    }

    /**
     * Configures a friendly name for the generator. This will be used by the
     * generator to select the library with the -g flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        log.debug("Getting the generator name");
        return "modsecurity3";
    }

    /**
     * Returns human-friendly help for the generator. Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        log.debug("Getting help message");
        return "Generates a modsecurity3 client library.";
    }

    /**
     * Location to write model files. You can use the modelPackage() as defined when
     * the class is instantiated
     */
    public String modelFileFolder() {
        return templateManager.modelFileFolder();
    }

    /**
     * Location to write api files. You can use the apiPackage() as defined when the
     * class is instantiated
     */
    @Override
    public String apiFileFolder() {
        return templateManager.apiFileFolder();
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle
     * escaping those terms here. This logic is only called if a variable matches the
     * reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        log.debug("Escaping reserved word: {}", name);
        return "_" + name; // add an underscore to the name
    }

    /**
     * override with any special text escaping logic to handle unsafe
     * characters so as to avoid code injection
     *
     * @param input String to be cleaned up
     * @return string with unsafe characters removed or escaped
     */
    @Override
    public String escapeUnsafeCharacters(String input) {
        log.debug("Escaping unsafe characters in input");
        // TODO: check that this logic is safe to escape unsafe characters to avoid code
        // injection
        return input;
    }

    /**
     * Escape single and/or double quote to avoid code injection
     *
     * @param input String to be cleaned up
     * @return string with quotation mark removed or escaped
     */
    public String escapeQuotationMark(String input) {
        log.debug("Escaping quotation marks in input");
        // TODO: check that this logic is safe to escape quotation mark to avoid code
        // injection
        return input.replace("\"", "\\\"");
    }

    /**
     * Checks if a pattern string contains invalid regex constructs.
     * Delegates to OperationProcessor.
     *
     * @param patternString The pattern string to check
     * @return true if the pattern is invalid, false otherwise
     */
    public boolean isInvalidPattern(String patternString) {
        return operationProcessor.isInvalidPattern(patternString);
    }

    //
    // Core Processing Methods
    //

    /**
     * Provides an opportunity to inspect and modify operation data before the code
     * is generated.
     */
    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        log.debug("Post-processing operations with models");
        OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

        // Delegate to the operation processor
        return operationProcessor.processOperationsWithModels(results, allModels);
    }

    /**
     * Process models and generate JSON Schema.
     *
     * @param objs The models to process
     * @return The processed models
     */
    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        ModelsMap result = super.postProcessModels(objs);

        // Delegate to the model processor
        return modelProcessor.processModels(result);
    }

    /**
     * Process all models and generate JSON Schema.
     */
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> result = super.postProcessAllModels(objs);

        // Delegate to the model processor
        return modelProcessor.processAllModels(result);
    }

    /**
     * Helper method to flatten a model property into a list of properties.
     * Delegates to ModelProcessor.
     *
     * @param currentProperty The property to flatten
     * @param baseNamePrefix The prefix to add to the property name
     * @return A list of flattened properties
     */
    public List<CodegenProperty> flattenModel(CodegenProperty currentProperty, String baseNamePrefix) {
        return modelProcessor.flattenModel(currentProperty, baseNamePrefix);
    }

    /**
     * These methods are now handled by Lombok @Getter and @Setter annotations
     * on the fields below. The implementation delegates to the appropriate
     * processor or manager.
     */

    // For backward compatibility with tests
    @Getter @Setter
    public Long globalIndex;

    @Getter @Setter
    public String jsonSchemaOutputFile;

    @Getter @Setter
    public String outputFolder;

    /**
     * Get the global parameter index.
     * For backward compatibility with tests.
     *
     * @return The global parameter index from the operation processor
     */
    public Long getGlobalParamIndex() {
        return operationProcessor.getGlobalParamIndex();
    }

    /**
     * Set the global parameter index.
     * For backward compatibility with tests.
     *
     * @param globalParamIndex The global parameter index to set
     */
    public void setGlobalParamIndex(Long globalParamIndex) {
        operationProcessor.setGlobalParamIndex(globalParamIndex);
    }

    //
    // Methods delegated to PatternGenerationService for backward compatibility
    //

    /**
     * Based on the type of parameter, returns the allowed input pattern.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to get the allowed input pattern for
     * @return A regex pattern string for the allowed input
     */
    public String getAllowedInputPattern(CodegenParameter param) {
        return patternGenerationService.getAllowedInputPattern(param);
    }

    /**
     * Returns true if the parameter is a decimal type.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to check
     * @return true if the parameter is a decimal type, false otherwise
     */
    public boolean isDecimal(CodegenParameter param) {
        return patternGenerationService.isDecimal(param);
    }

    /**
     * Returns true if the parameter allows multiple character or digits.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to check
     * @return true if the parameter allows multiple values, false otherwise
     */
    public boolean allowMultiple(CodegenParameter param) {
        return patternGenerationService.allowMultiple(param);
    }

    /**
     * Returns the minimum length pattern string for the parameter.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to get the minimum length pattern for
     * @param isRequired Whether the parameter is required
     * @return The minimum length pattern string
     */
    public String getMinLengthPatternString(CodegenParameter param, boolean isRequired) {
        return patternGenerationService.getMinLengthPatternString(param, isRequired);
    }

    /**
     * Returns the maximum length pattern string for the parameter.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to get the maximum length pattern for
     * @param isRequired Whether the parameter is required
     * @return The maximum length pattern string
     */
    public String getMaxLengthPatternString(CodegenParameter param, boolean isRequired) {
        return patternGenerationService.getMaxLengthPatternString(param, isRequired);
    }

    /**
     * Returns a pattern for decimal types.
     * Delegates to PatternGenerationService.
     *
     * @param allowedInputPattern The allowed input pattern
     * @param minLengthPatternString The minimum length pattern string
     * @param maxLengthPatternString The maximum length pattern string
     * @param isRequired Whether the parameter is required
     * @return A regex pattern string for decimal types
     */
    public String getDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean isRequired) {
        return patternGenerationService.getDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, isRequired);
    }

    /**
     * Returns a pattern for non-decimal types.
     * Delegates to PatternGenerationService.
     *
     * @param allowedInputPattern The allowed input pattern
     * @param minLengthPatternString The minimum length pattern string
     * @param maxLengthPatternString The maximum length pattern string
     * @param allowMultiple Whether multiple values are allowed
     * @param isRequired Whether the parameter is required
     * @return A regex pattern string for non-decimal types
     */
    public String getNonDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean allowMultiple, boolean isRequired) {
        return patternGenerationService.getNonDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, allowMultiple, isRequired);
    }

    /**
     * Generates a regex string to validate the input of a parameter.
     * Delegates to PatternGenerationService.
     *
     * @param param The parameter to generate a pattern for
     * @return A regex pattern string for validating the parameter
     */
    public String getParamPattern(CodegenParameter param) {
        return patternGenerationService.getParamPattern(param);
    }
}
