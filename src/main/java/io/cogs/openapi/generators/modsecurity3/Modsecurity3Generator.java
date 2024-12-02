package io.cogs.openapi.generators.modsecurity3;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;
import io.swagger.models.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.File;

public class Modsecurity3Generator extends DefaultCodegen implements CodegenConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(Modsecurity3Generator.class);

  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "0.0.1";

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
   */
  private String getParamPattern(CodegenParameter param) {
    String patternString = "^";
    String allowedInputPattern = getAllowedInputPattern(param);
    boolean handleDecimal = isDecimal(param);
    boolean isRequired = param.required;
    boolean allowMultiple = allowMultiple(param);

    String minLengthPatternString = getMinLengthPatternString(param, isRequired);
    String maxLengthPatternString = getMaxLengthPatternString(param, isRequired);

    if (handleDecimal) {
      patternString += getDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, isRequired);
    } else {
      patternString += getNonDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, allowMultiple, isRequired);
    }

    patternString += "$";

    return patternString;
  }

  /*
   * Based on the type of parameter, returns the allowed input pattern.
   */
  private String getAllowedInputPattern(CodegenParameter param) {
    if (param.isInteger || param.isLong || param.isNumber || param.isFloat || param.isDouble || param.isDecimal) {
      return "[0-9]";
    } else if (param.getIsBoolean()) {
      return "(true|false)";
    } else if (param.isUuid) {
      return "[0-9a-fA-F]";
    } else if (param.isByteArray || param.isDate || param.isDateTime || param.isEmail) {
      return ".";
    } else {
      return ".";
    }
  }

  /*
   * Returns true if the parameter is a decimal type.
   */
  private boolean isDecimal(CodegenParameter param) {
    return param.isFloat || param.isDouble || param.isDecimal;
  }

  /*
   * Returns true if the parameter allows multiple character or digits, or
   * if it uses a single complex pattern (such as true/false for booleans, 
   * or decimals since they have a set pattern).
   */
  private boolean allowMultiple(CodegenParameter param) {
    return !param.getIsBoolean() && !isDecimal(param);
  }

  /*
   * Returns the minimum length pattern string for the parameter.
   * If the parameter is required, the minimum length is set to 1 if it is not already set.
   */
  private String getMinLengthPatternString(CodegenParameter param, boolean isRequired) {
    Integer minLength = param.getMinLength();

    // If the parameter is required, and no minimum length is set, we set it to 1.
    if (isRequired) {
      if (minLength == null || minLength == 0) {
        minLength = 1;
      }
    }

    return minLength == null ? "" : minLength.toString();
  }

  /*
   * Returns the maximum length pattern string for the parameter.
   * If the parameter is required, the maximum length is set to 1 if it is not already set.
   */
  private String getMaxLengthPatternString(CodegenParameter param, boolean isRequired) {
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
  private String getDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean isRequired) {
    if (!isRequired) {
      return allowedInputPattern + "*.?" + allowedInputPattern + "*";
    } else {
      return allowedInputPattern + "{" + minLengthPatternString + "," + maxLengthPatternString + "}";
    }
  }

  private String getNonDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean allowMultiple, boolean isRequired) {
    if (allowMultiple) {
      return allowedInputPattern + "{" + minLengthPatternString + "," + maxLengthPatternString + "}";
    } else {
      String pattern = allowedInputPattern;
      if (!isRequired) {
        pattern += "?";
      }
      return pattern;
    }
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
      LOGGER.debug("Processing operation: {}", co.operationId);
      // example:
      // co.httpMethod = co.httpMethod.toLowerCase();

      // Loop through parameters and print information about them
      for (CodegenParameter param : co.allParams) {
        String patternString = param.pattern;
        if (patternString == null || patternString.isEmpty()) {
          patternString = getParamPattern(param);
          LOGGER.debug("Calculated pattern string {}", patternString);
          param.setPattern(patternString);
        } else {
          LOGGER.debug("Config pattern string used: {}", patternString);
        }
        LOGGER.debug("param: {}, validation: {}, pattern: {}", param.hasValidation, param.pattern);
        LOGGER.debug("Parameter: {}, data type: {}, isString: {}, max length: {}", param.baseName, param.getDataType(),
            param.isString, param.getMaxLength());
      }
    }

    return results;
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

  public Modsecurity3Generator() {
    super();
    LOGGER.debug("Initializing Modsecurity3Generator");

    // set the output folder here
    outputFolder = "generated-code/modsecurity3";

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
     * Api Package. Optional, if needed, this can be used in templates
     */
    apiPackage = "org.openapitools.api";

    /**
     * Reserved words. Override this with reserved words specific to your language
     */
    reservedWords = new HashSet<String>(
        Arrays.asList(
            "sample1", // replace with static values
            "sample2"));

    /**
     * Additional Properties. These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);

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
    String folder = outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
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
    String folder = outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
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
    // TODO: check that this logic is safe to escape quotation mark to avoid code
    // injection
    return input.replace("\"", "\\\"");
  }
}
