package com.oashield.openapi.generators.modsecurity3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Modsecurity3Generator extends DefaultCodegen implements CodegenConfig {

    // Pattern generation service for secure pattern handling
    private final PatternGenerationService patternGenerationService = new PatternGenerationService();

    @Override
    public void setOutputDir(String dir) {
        super.setOutputDir(dir);
        this.outputFolder = dir;
    }

    // JSON Schema generation configuration
    private boolean generateJsonSchema = true;
    public String jsonSchemaOutputFile = "schema.json";

    /**
     * Process the CLI options passed to the generator.
     *
     * @param opts The options passed to the generator
     */
    @Override
    public void processOpts() {
        super.processOpts();

        // Process JSON Schema generation options
        if (additionalProperties.containsKey("generateJsonSchema")) {
            String generateJsonSchemaStr = additionalProperties.get("generateJsonSchema").toString();
            generateJsonSchema = Boolean.parseBoolean(generateJsonSchemaStr);
            LOGGER.info("generateJsonSchema set to: {}", generateJsonSchema);
        }

        if (additionalProperties.containsKey("jsonSchemaOutputFile")) {
            jsonSchemaOutputFile = additionalProperties.get("jsonSchemaOutputFile").toString();
            LOGGER.info("jsonSchemaOutputFile set to: {}", jsonSchemaOutputFile);
        }
    }

  private static final Logger LOGGER = LoggerFactory.getLogger(Modsecurity3Generator.class);

  private static final String MODSECURITY_INDEX_KEY = "x-codegen-globalIndex";
  private static final int MODSECURITY_INDEX_MAX = 30;
  private static final String MODSECURITY_PATH_REGEX_KEY = "x-codegen-pathRegex";
  private static final String VENDOR_EXTENSIONS_KEY = "vendorExtensions";
  private static final String MODSECURITY_HAS_ARRAY_MIN = "x-codegen-hasArrayMin";
  private static final String MODSECURITY_HAS_ARRAY_MAX = "x-codegen-hasArrayMax";
  private static final String MODSECURITY_HAS_JSON = "x-codegen-isJson";
  private static final String MODSECURITY_HAS_XML = "x-codegen-isXml";
  private static final String MODSECURITY_MODEL_PROPERTIES = "x-codegen-modelProperties";


  // source folder where to write the files
  public String apiVersion = "0.0.2";

  public Long globalIndex = 4200001L; // Default start
  public Long globalParamIndex = 4210001L; // Default start

  /**
   * Configures the type of generator.
   *
   * @return the CodegenType for this generator
   * @see org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() {
    LOGGER.debug("Getting the generator tag");
    return CodegenType.OTHER;
  }

  /**
   * Configures a friendly name for the generator. This will be used by the
   * generator
   * to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() {
    LOGGER.debug("Getting the generator name");
    return "modsecurity3";
  }

  /*
   * Generates a regex string to validate the input of a parameter.
   * SECURITY FIX: Delegates to PatternGenerationService for secure pattern generation
   */
  public String getParamPattern(CodegenParameter param) {
    return patternGenerationService.getParamPattern(param);
  }

  /*
   * Based on the type of parameter, returns the allowed input pattern.
   * SECURITY FIX: Delegates to PatternGenerationService for secure pattern generation
   */
  public String getAllowedInputPattern(CodegenParameter param) {
    return patternGenerationService.getAllowedInputPattern(param);
  }

  /*
   * Returns true if the parameter is a decimal type.
   * Delegates to PatternGenerationService for consistency
   */
  public boolean isDecimal(CodegenParameter param) {
    return patternGenerationService.isDecimal(param);
  }

  /*
   * Returns true if the parameter allows multiple character or digits, or
   * if it uses a single complex pattern (such as true/false for booleans,
   * or decimals since they have a set pattern).
   * Delegates to PatternGenerationService for consistency
   */
  public boolean allowMultiple(CodegenParameter param) {
    return patternGenerationService.allowMultiple(param);
  }

  /*
   * Returns the minimum length pattern string for the parameter.
   * If the parameter is required, the minimum length is set to 1 if it is not already set.
   */
  public String getMinLengthPatternString(CodegenParameter param, boolean isRequired) {
    Integer minLength = param.getMinLength();

    // If the parameter is required, and no minimum length is set, we set it to 1.
    if (isRequired) {
      if (minLength == null || minLength == 0) {
        minLength = 1;
      }
    } else {
      // For optional numeric types, we still want a minimum of 1 digit if a value is provided
      if ((param.isInteger || param.isLong || param.isNumber || param.isFloat || param.isDouble || param.isDecimal)
          && (minLength == null || minLength == 0)) {
        minLength = 1;
      }
    }

    return minLength == null ? "" : minLength.toString();
  }

  /*
   * Returns the maximum length pattern string for the parameter.
   * If the parameter is required, the maximum length is set to 1 if it is not already set.
   */
  public String getMaxLengthPatternString(CodegenParameter param, boolean isRequired) {
    Integer maxLength = param.getMaxLength();

    // Sets the maximum length depending on the type of the parameter.
    Integer maxLengthForType = null;
    if (param.isInteger) {
      maxLengthForType = 10;
    } else if (param.isLong) {
      maxLengthForType = 19;
    }

    // If we have a maximum length for the type, and either no maximum length is set on the param
    // or if it's greater than the maximum length for the type, we use the maximum length for the
    // type.
    if (maxLengthForType != null && (maxLength == null || maxLength == 0 || maxLength > maxLengthForType)) {
      return maxLengthForType.toString();
    }

    return maxLength == null ? "" : maxLength.toString();
  }

  // Returns a pattern for decimal types.
  public String getDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean isRequired) {
    if (!isRequired) {
      return allowedInputPattern + "*.?" + allowedInputPattern + "*";
    } else {
      return allowedInputPattern + "{" + minLengthPatternString + "," + maxLengthPatternString + "}";
    }
  }

  public String getNonDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean allowMultiple, boolean isRequired) {
    if (allowMultiple) {
      if(minLengthPatternString.isEmpty() && maxLengthPatternString.isEmpty()) {
        if(isRequired) {
          return allowedInputPattern + "+";
        }
        return allowedInputPattern + "*";
      }
      else if(minLengthPatternString.equals("1") && maxLengthPatternString.isEmpty()) {
        // Make it look prettier
        return allowedInputPattern + "+";
      }
      else if(minLengthPatternString.equals(maxLengthPatternString)) {
        // no comma needed
        return allowedInputPattern + "{" + minLengthPatternString + "}";
      }
      return allowedInputPattern + "{" + minLengthPatternString + "," + maxLengthPatternString + "}";
    } else {
      String pattern = allowedInputPattern;
      if (!isRequired) {
        // For patterns with quantifiers or complex patterns (containing | or groups), wrap in parentheses
        // But if the pattern is already fully parenthesized, just add ?
        if (pattern.startsWith("(") && pattern.endsWith(")")) {
          pattern += "?";
        } else if (pattern.contains("{") || pattern.contains("|")) {
          pattern = "(" + pattern + ")?";
        } else {
          pattern += "?";
        }
      }
      return pattern;
    }
  }

  public boolean isInvalidPattern(String patternString) {
    // TODO: This is a very basic check, and should be improved.
    return patternString.contains("(?!") ||
    patternString.contains("(?=") ||
    patternString.contains("(?<=") ||
    patternString.contains("(?<!");
  }

  /**
   * Provides an opportunity to inspect and modify operation data before the code
   * is generated.
   */
  @Override
  public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    LOGGER.debug("Post-processing operations with models");
    OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

    OperationMap ops = results.getOperations();
    List<CodegenOperation> opList = ops.getOperation();

    // iterate over the operation and perhaps modify something
    for (CodegenOperation co : opList) {
      String path = co.path;
      String matchPath = path.replaceAll("\\{.*?\\}", "[^/]+");
      co.vendorExtensions.put(MODSECURITY_PATH_REGEX_KEY, matchPath);
      for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
        co.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalIndex++);
      }
      LOGGER.debug("Processing operation: {}", co.operationId);
      // example:
      // co.httpMethod = co.httpMethod.toLowerCase();

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

      ArrayList<CodegenProperty> allParams = new ArrayList<CodegenProperty>();

      // Loop through parameters and print information about them
      for (CodegenParameter param : co.allParams) {

        // We don't want to use a different method for required arrays
        if (param.required && param.isArray && (param.getMinItems() == null || param.getMinItems() == 0)) {
          LOGGER.debug("Required array parameter: {}", param.baseName);
          param.setMinItems(1);
        }

        if (param.isModel) {
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

        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MIN, (param.getMinItems() != null));
        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MAX, (param.getMaxItems() != null));
        for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
          param.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalParamIndex++);
        }

        String patternString = param.pattern;

        if(patternString != null && !patternString.isEmpty()) {
          LOGGER.debug("Config pattern string used: {}", patternString);
          if(isInvalidPattern(patternString)) {
            LOGGER.warn("Invalid pattern string: {}", patternString);
            patternString = null;
          }

        }

        if(patternString == null || patternString.isEmpty()) {
          patternString = getParamPattern(param);
          LOGGER.debug("Calculated pattern string {}", patternString);
          param.setPattern(patternString);
        }
        LOGGER.debug("param: {}, validation: {}, pattern: {}", param.hasValidation, param.pattern);
        LOGGER.debug("Parameter: {}, data type: {}, isString: {}, max length: {}", param.baseName, param.getDataType(),
            param.isString, param.getMaxLength());

      }
    }

    Map<String, Object> vendorExtensions = new HashMap<String, Object>();
    vendorExtensions.put(MODSECURITY_INDEX_KEY, globalIndex++);
    results.put(VENDOR_EXTENSIONS_KEY, vendorExtensions);

    return results;
  }

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
   * Returns human-friendly help for the generator. Provide the consumer with help
   * tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    LOGGER.debug("Getting help message");
    return "Generates a modsecurity3 client library.";
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

    try {
      LOGGER.info("Generating JSON Schema from models...");
      JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator();
      String jsonSchema = jsonSchemaGenerator.generateJsonSchema(result);

      // Save the JSON Schema to a file
      String outputPath = outputFolder + File.separator + "schema.json";
      try {
        Files.write(Paths.get(outputPath), jsonSchema.getBytes());
        LOGGER.info("JSON Schema generated successfully: {}", outputPath);
      } catch (IOException e) {
        LOGGER.error("Error writing JSON Schema to file: {}", e.getMessage());
      }
    } catch (Exception e) {
      LOGGER.error("Error generating JSON Schema: {}", e.getMessage());
    }

    return result;
  }

  /**
   * Process all models and generate JSON Schema.
   */
  @Override
  public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    Map<String, ModelsMap> result = super.postProcessAllModels(objs);

    // Examine ModelMap structure to understand its content
    if (LOGGER.isDebugEnabled()) {
      for (Map.Entry<String, ModelsMap> entry : result.entrySet()) {
        String modelName = entry.getKey();
        ModelsMap modelsMap = entry.getValue();
        LOGGER.debug("Model: {}", modelName);

        for (ModelMap modelMap : modelsMap.getModels()) {
          CodegenModel model = modelMap.getModel();
          LOGGER.debug("  Model name: {}", model.name);
          LOGGER.debug("  Model description: {}", model.description);
          LOGGER.debug("  Model vars count: {}", model.vars.size());

          // Log specific model properties to understand structure
          if (model.vars != null && !model.vars.isEmpty()) {
            LOGGER.debug("  Model has vars");
            CodegenProperty firstVar = model.vars.get(0);
            LOGGER.debug("  First var name: {}, dataType: {}", firstVar.name, firstVar.dataType);
          }
          if (model.requiredVars != null && !model.requiredVars.isEmpty()) {
            LOGGER.debug("  Model has requiredVars");
          }
        }
      }
    }

    // Process models for JSON Schema generation
    if (generateJsonSchema) {
      generateJsonSchema(result);
    }

    return result;
  }

  /**
   * Generate JSON Schema from models.
   *
   * @param models The models to convert to JSON Schema
   */
  private void generateJsonSchema(Map<String, ModelsMap> models) {
    LOGGER.info("Generating JSON Schema from models...");

    try {
      // Create a combined schema with all models
      ObjectMapper objectMapper = new ObjectMapper();
      ObjectNode rootSchema = objectMapper.createObjectNode();
      rootSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
      rootSchema.put("title", "OpenAPI Schema Definitions");
      rootSchema.put("description", "JSON Schema definitions generated from OpenAPI specification");
      rootSchema.put("type", "object");
      ObjectNode definitions = rootSchema.putObject("definitions");

      JsonSchemaGenerator generator = new JsonSchemaGenerator();

      // Process each model and add to the combined schema
      for (Map.Entry<String, ModelsMap> entry : models.entrySet()) {
        String modelName = entry.getKey();
        ModelsMap modelsMap = entry.getValue();

        // Skip models without any model maps
        if (modelsMap.getModels() == null || modelsMap.getModels().isEmpty()) {
          continue;
        }

        // Get the first model from the models map
        ModelMap modelMap = modelsMap.getModels().get(0);
        CodegenModel model = modelMap.getModel();

        // Process the model and add it to the definitions
        ObjectNode modelSchema = generator.generateModelSchema(model);
        if (modelSchema != null) {
          definitions.set(modelName, modelSchema);
        }
      }

      // Convert the schema to a JSON string
      String jsonSchema = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootSchema);

      // Write JSON Schema to file
      File outputDir = new File(outputFolder);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }

      File schemaFile = new File(outputDir, jsonSchemaOutputFile);
      try (FileWriter writer = new FileWriter(schemaFile)) {
        writer.write(jsonSchema);
      }

      LOGGER.info("JSON Schema generated successfully: {}", schemaFile.getAbsolutePath());
    } catch (Exception e) {
      LOGGER.error("Error generating JSON Schema", e);
    }
  }

  public Modsecurity3Generator() {
    super();
    LOGGER.debug("Initializing Modsecurity3Generator");

    // super.outputFolder = "output/modsecurity3";
    /**
     * Api classes. You can write classes for each Api file with the
     * apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple
     * files per
     * class
     */
    apiTemplateFiles.put(
        "config.mustache", // the template to use
        ".conf"); // the extension for each file to write

    /**
     * Template Location. This is the location which templates will be read from.
     * The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "modsecurity3";

    /**
     * Additional Properties. These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);

    // JSON Schema generation configuration
    additionalProperties.put("generateJsonSchema", generateJsonSchema);
    additionalProperties.put("jsonSchemaOutputFile", jsonSchemaOutputFile);

    // CLI options for JSON Schema generation
    cliOptions.add(new CliOption("generateJsonSchema", "Generate JSON Schema from models")
        .defaultValue(Boolean.toString(generateJsonSchema)));
    cliOptions.add(new CliOption("jsonSchemaOutputFile", "JSON Schema output file name")
        .defaultValue(jsonSchemaOutputFile));

    /**
     * Supporting Files. You can write single files for the generator with the
     * entire object tree available. If the input file has a suffix of `.mustache
     * it will be processed by the template engine. Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("mainconfig.mustache", // the input template or file
        "", // the destination folder, relative `outputFolder`
        "mainconfig.conf") // the output file
    );

    /**
     * Language Specific Primitives. These types will not trigger imports by
     * the client generator
     */
    languageSpecificPrimitives = new HashSet<String>(
        Arrays.asList(
            "Type1", // replace these with your types
            "Type2"));

    LOGGER.debug("Modsecurity3Generator initialized with output folder: {}", outputFolder);
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle
   * escaping
   * those terms here. This logic is only called if a variable matches the
   * reserved words
   *
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    LOGGER.debug("Escaping reserved word: {}", name);
    return "_" + name; // add an underscore to the name
  }

  /**
   * Location to write model files. You can use the modelPackage() as defined when
   * the class is
   * instantiated
   */
  public String modelFileFolder() {
    String folder = outputFolder;
    LOGGER.debug("Model file folder: {}", folder);
    return folder;
  }

  /**
   * Location to write api files. You can use the apiPackage() as defined when the
   * class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    String folder = outputFolder;
    LOGGER.debug("API file folder: {}", folder);
    return folder;
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
    LOGGER.debug("Escaping unsafe characters in input");
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
    LOGGER.debug("Escaping quotation marks in input");
    if (input == null) {
      return null;
    }
    // TODO: check that this logic is safe to escape quotation mark to avoid code
    // injection
    return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'");
  }
}
