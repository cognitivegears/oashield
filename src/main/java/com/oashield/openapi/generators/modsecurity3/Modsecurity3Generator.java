package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;
import io.swagger.models.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.File;

public class Modsecurity3Generator extends DefaultCodegen implements CodegenConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(Modsecurity3Generator.class);

  private static final String MODSECURITY_INDEX_KEY = "x-codegen-globalIndex";
  private static final int MODSECURITY_INDEX_MAX = 20;
  private static final String MODSECURITY_PATH_REGEX_KEY = "x-codegen-pathRegex";
  private static final String VENDOR_EXTENSIONS_KEY = "vendorExtensions";
  private static final String MODSECURITY_HAS_ARRAY_MIN = "x-codegen-hasArrayMin";
  private static final String MODSECURITY_HAS_ARRAY_MAX = "x-codegen-hasArrayMax";
  private static final String MODSECURITY_AUTH_PARAM = "x-codegen-authParam";

  // source folder where to write the files
  protected String apiVersion = "0.0.2";

  protected Long globalIndex = 4200001L; // Default start
  protected Long globalParamIndex = 4210001L; // Default start

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
    } else if (param.isDate) {
      return "(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d)))$";
    } else if (param.isDateTime) {
      return "^(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d))T([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)(\\.\\d+)?([Zz]|[+\\-](0\\d|1[0-4])(:[0-5]\\d)?)?$";
    } else if (param.isEmail) {
      return "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$";
    } else if (param.isEnum) {
      List<String> enumValues = null;
      try {
        enumValues = (List<String>)param.allowableValues.get("values");
      }
      catch (ClassCastException e) {
        LOGGER.warn("Could not cast allowable values to list of strings for parameter: {}", param.baseName);
      }
      if (enumValues == null || enumValues.isEmpty()) {
        LOGGER.warn("No enum values found for parameter: {}", param.baseName);
        return ".";
      }
      return "(" + String.join("|", enumValues) + ")";
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
    return !(param.getIsBoolean() ||
     isDecimal(param) ||
     param.isDate ||
     param.isDateTime ||
     param.isEnum ||
     param.isEmail);
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
        pattern += "?";
      }
      return pattern;
    }
  }

  private boolean isInvalidPattern(String patternString) {
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

      // Loop through parameters and print information about them
      for (CodegenParameter param : co.allParams) {

        // We don't want to use a different method for required arrays
        if (param.required && param.isArray && (param.getMinItems() == null || param.getMinItems() == 0)) {
          LOGGER.debug("Required array parameter: {}", param.baseName);
          param.setMinItems(1);
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

      addAuthParameters(co);
    }

    

    Map<String, Object> vendorExtensions = new HashMap<String, Object>();
    vendorExtensions.put(MODSECURITY_INDEX_KEY, globalIndex++);
    results.put(VENDOR_EXTENSIONS_KEY, vendorExtensions);

    return results;
  }

  private void addAuthParameters(CodegenOperation co) {
    if(co.hasAuthMethods) {
      LOGGER.debug("Operation: {}, has auth methods: {}", co.operationId, co.hasAuthMethods);
      co.authMethods.forEach((authMethod) -> {
        LOGGER.debug("Auth method: {}", authMethod);
        if(authMethod.isKeyInQuery) {
          LOGGER.debug("Key in query: {}", authMethod.keyParamName);
          String paramName = authMethod.keyParamName;
          if(paramName != null && !paramName.isEmpty()) {
            // Spec supports multiple auth params
            if(co.vendorExtensions.containsKey(MODSECURITY_AUTH_PARAM)) {
              paramName = co.vendorExtensions.get(MODSECURITY_AUTH_PARAM) + "|" + paramName;
            }
            else {
              co.vendorExtensions.put(MODSECURITY_AUTH_PARAM, paramName);
            }
          }
        }
      });
    }
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
    // TODO: check that this logic is safe to escape quotation mark to avoid code
    // injection
    return input.replace("\"", "\\\"");
  }
}
