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

    // Target WAF engine flavor: both accept the same SecLang core, but only
    // Coraza implements @validateSchema for JSON bodies (ModSecurity3's is XSD-only).
    private String engineFlavor = FLAVOR_MODSECURITY3;
    private boolean validateBodySchema = true;
    // Path to the schema file as referenced from inside the generated @validateSchema
    // rule. Coraza resolves it relative to the server process working directory, which
    // generally differs from the rules directory (e.g. "rules/schema.json").
    private String schemaRulePath = null;

    // Deny behavior and logging (issue #16). denyAction is the disruptive action
    // SecDefaultAction applies when a generated rule blocks: deny (with denyStatus),
    // drop, redirect (to denyRedirectUrl), or pass (detection-only).
    private String denyAction = "deny";
    private int denyStatus = 403;
    private String denyRedirectUrl = null;
    private boolean enableLogging = true;
    // Policy for declared media types the WAF cannot inspect (e.g.
    // application/octet-stream): pass them through or block them.
    private String unknownMediaTypePolicy = "pass";
    // Deployed base path prefix for path-match regexes; null = auto-extract from
    // the first servers.url, "" = no prefix.
    private String basePathOverride = null;
    // XSD generation + @validateSchema XML rules. Default OFF: libmodsecurity3
    // currently cannot load XSDs at request time (fails open to match-everything)
    // and Coraza has no XML support — see docs/engine-behavior.md.
    private boolean validateXmlSchema = false;
    public String xsdOutputFile = "schema.xsd";
    private String xsdRulePath = null;
    // false = emit no SecRuleEngine/SecRequestBodyAccess/SecDefaultAction, for
    // deployments whose existing ModSecurity config already sets them
    private boolean includeEngineConfig = true;

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

        if (additionalProperties.containsKey(ENGINE_FLAVOR)) {
            engineFlavor = additionalProperties.get(ENGINE_FLAVOR).toString();
            if (!FLAVOR_MODSECURITY3.equals(engineFlavor) && !FLAVOR_CORAZA.equals(engineFlavor)) {
                throw new IllegalArgumentException(
                    "Unknown engineFlavor '" + engineFlavor + "'; expected '" + FLAVOR_MODSECURITY3
                        + "' or '" + FLAVOR_CORAZA + "'");
            }
            LOGGER.info("engineFlavor set to: {}", engineFlavor);
        }
        // Overwrite with real booleans: CLI additional properties arrive as strings and
        // the string "false" is truthy in mustache sections.
        additionalProperties.put("isCoraza", FLAVOR_CORAZA.equals(engineFlavor));
        additionalProperties.put("isModsec3", FLAVOR_MODSECURITY3.equals(engineFlavor));

        if (additionalProperties.containsKey("validateBodySchema")) {
            validateBodySchema = Boolean.parseBoolean(additionalProperties.get("validateBodySchema").toString());
            LOGGER.info("validateBodySchema set to: {}", validateBodySchema);
        }
        additionalProperties.put("validateBodySchema", validateBodySchema);

        if (additionalProperties.containsKey("schemaRulePath")) {
            schemaRulePath = additionalProperties.get("schemaRulePath").toString();
        }
        additionalProperties.put("schemaRulePath", schemaRulePath != null ? schemaRulePath : jsonSchemaOutputFile);

        if (additionalProperties.containsKey("denyAction")) {
            denyAction = additionalProperties.get("denyAction").toString();
            if (!Arrays.asList("deny", "drop", "pass", "redirect").contains(denyAction)) {
                throw new IllegalArgumentException(
                    "Unknown denyAction '" + denyAction + "'; expected 'deny', 'drop', 'pass' or 'redirect'");
            }
            LOGGER.info("denyAction set to: {}", denyAction);
        }

        if (additionalProperties.containsKey("denyStatus")) {
            try {
                denyStatus = Integer.parseInt(additionalProperties.get("denyStatus").toString());
            } catch (NumberFormatException e) {
                denyStatus = -1;
            }
            if (denyStatus < 100 || denyStatus > 599) {
                throw new IllegalArgumentException(
                    "Invalid denyStatus '" + additionalProperties.get("denyStatus") + "'; expected an HTTP status code (100-599)");
            }
            LOGGER.info("denyStatus set to: {}", denyStatus);
        }

        if (additionalProperties.containsKey("denyRedirectUrl")) {
            denyRedirectUrl = additionalProperties.get("denyRedirectUrl").toString();
            if (!denyRedirectUrl.matches("^https?://[^\\s\"']+$")) {
                throw new IllegalArgumentException(
                    "Invalid denyRedirectUrl '" + denyRedirectUrl + "'; expected an absolute http(s) URL");
            }
        }
        if ("redirect".equals(denyAction) && denyRedirectUrl == null) {
            throw new IllegalArgumentException("denyAction=redirect requires denyRedirectUrl");
        }

        if (additionalProperties.containsKey("enableLogging")) {
            enableLogging = Boolean.parseBoolean(additionalProperties.get("enableLogging").toString());
            LOGGER.info("enableLogging set to: {}", enableLogging);
        }

        if (additionalProperties.containsKey("includeEngineConfig")) {
            includeEngineConfig = Boolean.parseBoolean(additionalProperties.get("includeEngineConfig").toString());
            LOGGER.info("includeEngineConfig set to: {}", includeEngineConfig);
        }

        if (additionalProperties.containsKey("unknownMediaTypePolicy")) {
            unknownMediaTypePolicy = additionalProperties.get("unknownMediaTypePolicy").toString();
            if (!Arrays.asList("pass", "block").contains(unknownMediaTypePolicy)) {
                throw new IllegalArgumentException(
                    "Unknown unknownMediaTypePolicy '" + unknownMediaTypePolicy + "'; expected 'pass' or 'block'");
            }
            LOGGER.info("unknownMediaTypePolicy set to: {}", unknownMediaTypePolicy);
        }
        additionalProperties.put("blockOtherMedia", "block".equals(unknownMediaTypePolicy));

        if (additionalProperties.containsKey("basePath")) {
            basePathOverride = additionalProperties.get("basePath").toString();
            LOGGER.info("basePath set to: '{}'", basePathOverride);
        }

        if (additionalProperties.containsKey("validateXmlSchema")) {
            validateXmlSchema = Boolean.parseBoolean(additionalProperties.get("validateXmlSchema").toString());
            LOGGER.info("validateXmlSchema set to: {}", validateXmlSchema);
        }
        additionalProperties.put("validateXmlSchema", validateXmlSchema);
        if (additionalProperties.containsKey("xsdOutputFile")) {
            xsdOutputFile = additionalProperties.get("xsdOutputFile").toString();
        }
        if (additionalProperties.containsKey("xsdRulePath")) {
            xsdRulePath = additionalProperties.get("xsdRulePath").toString();
        }
        additionalProperties.put("xsdRulePath", xsdRulePath != null ? xsdRulePath : xsdOutputFile);

        // Real boolean for the mustache section; derived strings so templates stay flat
        additionalProperties.put("includeEngineConfig", includeEngineConfig);
        additionalProperties.put("logAction", enableLogging ? "log,auditlog" : "nolog");
        additionalProperties.put("denyActionDirective", buildDenyActionDirective());
    }

    /**
     * The disruptive-action fragment of SecDefaultAction: deny/redirect carry a
     * status, drop/pass ignore it.
     */
    private String buildDenyActionDirective() {
        switch (denyAction) {
            case "deny":
                return "deny,status:" + denyStatus;
            case "redirect":
                return "redirect:'" + denyRedirectUrl + "',status:" + denyStatus;
            default:
                return denyAction;
        }
    }

  private static final Logger LOGGER = LoggerFactory.getLogger(Modsecurity3Generator.class);

  private static final String MODSECURITY_INDEX_KEY = "x-codegen-globalIndex";
  private static final int MODSECURITY_INDEX_MAX = 40;
  private static final String MODSECURITY_PATH_REGEX_KEY = "x-codegen-pathRegex";
  private static final String VENDOR_EXTENSIONS_KEY = "vendorExtensions";
  private static final String MODSECURITY_HAS_ARRAY_MIN = "x-codegen-hasArrayMin";
  private static final String MODSECURITY_HAS_ARRAY_MAX = "x-codegen-hasArrayMax";
  private static final String MODSECURITY_HAS_JSON = "x-codegen-isJson";
  private static final String MODSECURITY_HAS_XML = "x-codegen-isXml";
  private static final String MODSECURITY_MODEL_PROPERTIES = "x-codegen-modelProperties";
  private static final String MODSECURITY_ARGS_ALLOWLIST = "x-codegen-argsAllowlist";

  private static final String ENGINE_FLAVOR = "engineFlavor";
  private static final String FLAVOR_MODSECURITY3 = "modsecurity3";
  private static final String FLAVOR_CORAZA = "coraza";

  // Media-type classification keys set on each consume entry (exactly one is "true")
  static final String CONSUME_JSON = "isJson";
  static final String CONSUME_XML = "isXml";
  static final String CONSUME_FORM_LIKE = "isFormLike";
  static final String CONSUME_WILDCARD = "isWildcardAll";
  static final String CONSUME_OTHER = "isOtherMedia";

  // Prefix both engines use when flattening JSON bodies into ARGS
  private static final String JSON_ARGS_PREFIX = "json.";
  // ModSecurity3 keys array elements "json.items.array_0", Coraza "json.items.0";
  // this fragment matches either so generated selectors work on both engines.
  private static final String ARRAY_INDEX_REGEX = "(?:array_)?\\d{1,9}";
  private static final String PROP_INDEX_KEY = "x-codegen-propIndex";
  private static final int PROP_INDEX_MAX = 12;
  private static final int MAX_FLATTEN_DEPTH = 5;


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

    // Deployed base path (servers.url path component or basePath override),
    // prepended to every operation's path-match regex.
    String basePathRegex = buildBasePathRegex();

    // $ref properties carry no vars of their own; resolve them via the model list
    Map<String, CodegenModel> modelLookup = new HashMap<String, CodegenModel>();
    if (allModels != null) {
      for (ModelMap modelMap : allModels) {
        CodegenModel model = modelMap.getModel();
        if (model != null) {
          modelLookup.put(model.classname, model);
          modelLookup.put(model.name, model);
        }
      }
    }

    // iterate over the operation and perhaps modify something
    for (CodegenOperation co : opList) {
      for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
        co.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalIndex++);
      }
      LOGGER.debug("Processing operation: {}", co.operationId);

      boolean includeRequestJSON = false;
      boolean includeRequestXML = false;

      if(co.hasConsumes) {
        LOGGER.debug("Operation: {} Consumes: {}", co.baseName, co.consumes);
        int consumeIndex = 0;
        for (Map<String, String> consume : co.consumes) {
          String mediaType = consume.get("mediaType");
          // Canonical single classification key per consume entry. DefaultCodegen's
          // isJson/isXml string flags are replaced: the string "false" is truthy in
          // mustache sections, and unclassified media types previously fell through
          // to an unconditional block (form/multipart operations could never succeed).
          String classification = classifyMediaType(mediaType);
          consume.remove("isJson");
          consume.remove("isXml");
          consume.put(classification, "true");
          includeRequestJSON |= CONSUME_JSON.equals(classification);
          includeRequestXML |= CONSUME_XML.equals(classification);

          // Unique marker suffix and rule ids per consume entry: two consumes of the
          // same class would otherwise emit duplicate SecMarker names and rule ids.
          consume.put("consumeIndex", String.valueOf(consumeIndex++));
          consume.put("oasGateId", String.valueOf(globalParamIndex++));
          consume.put("oasBodyErrId", String.valueOf(globalParamIndex++));
          consume.put("oasSchemaId", String.valueOf(globalParamIndex++));
          consume.put("oasPassId", String.valueOf(globalParamIndex++));

          // mediaType can contain regex metacharacters (e.g. application/vnd.api+json);
          // '*' wildcards (application/*) match any token in that position
          if (mediaType != null) {
            consume.put("mediaTypeRegex", escapeRegexLiteral(mediaType).replace("\\*", "[^/\\s]+"));
          }
        }

        // OAS3 requestBody.required defaults to FALSE: a bodiless request to an
        // operation with an optional body must skip the body checks instead of
        // being blocked by the content-type fallthrough.
        if (!isRequestBodyRequired(co)) {
          co.vendorExtensions.put("x-codegen-optionalBody", true);
        }
      }

      // Add vendor extension for JSON and XML
      co.vendorExtensions.put(MODSECURITY_HAS_JSON, includeRequestJSON);
      co.vendorExtensions.put(MODSECURITY_HAS_XML, includeRequestXML);
      co.vendorExtensions.put("validateBodySchema", validateBodySchema);

      // Names allowed to appear in ARGS_NAMES for this operation (query + form +
      // flattened JSON body fields); anything else is denied by the generated allowlist rule.
      java.util.Set<String> argsAllowlist = new java.util.LinkedHashSet<String>();

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
          // the model. Both engines flatten JSON bodies into ARGS as "json.<path>".
          // Composed (allOf/oneOf/anyOf) body models carry no vars on the parameter
          // itself; resolve them via the model list.
          CodegenModel bodyModel = modelLookup.get(param.baseType);
          if (bodyModel == null) {
            bodyModel = modelLookup.get(param.dataType);
          }
          List<CodegenProperty> rootVars = param.vars;
          if ((rootVars == null || rootVars.isEmpty()) && bodyModel != null) {
            rootVars = bodyModel.vars;
          }
          List<CodegenProperty> flattenedProperties = new ArrayList<CodegenProperty>();
          if (rootVars != null) {
            for (CodegenProperty prop : rootVars) {
              List<CodegenProperty> properties = flattenModel(prop, JSON_ARGS_PREFIX, 1, modelLookup);
              flattenedProperties.addAll(properties);
            }
          }
          if (bodyModel != null && unionMembers(bodyModel.getComposedSchemas()) != null) {
            // oneOf/anyOf body: vars is the union of all branches, only one of which
            // must be present, so no property can be individually required
            for (CodegenProperty prop : flattenedProperties) {
              prop.required = false;
            }
          }

          // Raw-spec keyword pass: const and patternProperties are not surfaced by
          // the codegen abstractions, so resolve them from the parsed spec schema
          // along each flattened path.
          io.swagger.v3.oas.models.media.Schema<?> rawRoot = rawSchemaByName(param.baseType);
          if (rawRoot == null) {
            rawRoot = rawSchemaByName(param.dataType);
          }
          if (rawRoot != null) {
            for (CodegenProperty prop : flattenedProperties) {
              io.swagger.v3.oas.models.media.Schema<?> rawProp = rawSchemaForPath(rawRoot, prop.baseName);
              if (rawProp == null) {
                continue;
              }
              if (rawProp.getConst() != null) {
                prop.pattern = "^" + escapeRegexLiteral(String.valueOf(rawProp.getConst())) + "$";
                // const admits exactly one value — never null, even when codegen
                // inferred nullability from a type-less schema
                prop.isNullable = false;
              }
              if (rawProp.getPatternProperties() != null && (prop.isMap || prop.isFreeFormObject)) {
                List<Map<String, Object>> ppRules = new ArrayList<Map<String, Object>>();
                for (Map.Entry<String, io.swagger.v3.oas.models.media.Schema> pp
                    : rawProp.getPatternProperties().entrySet()) {
                  Map<String, Object> rule = new HashMap<String, Object>();
                  rule.put("nameRegex", patternPropertiesNameRegex(pp.getKey()));
                  rule.put("valuePattern", rawValuePattern(pp.getValue()));
                  ppRules.add(rule);
                }
                prop.vendorExtensions.put("x-oashield-patternProps", ppRules);
              }
            }

            // dependentRequired at the body root: presence of the trigger property
            // demands the dependent property (chained count rules on both engines)
            io.swagger.v3.oas.models.media.Schema<?> resolvedRoot = resolveRawRef(rawRoot);
            if (resolvedRoot.getDependentRequired() != null) {
              List<Map<String, Object>> depRules = new ArrayList<Map<String, Object>>();
              for (Map.Entry<String, List<String>> dep : resolvedRoot.getDependentRequired().entrySet()) {
                for (String requiredName : dep.getValue()) {
                  Map<String, Object> rule = new HashMap<String, Object>();
                  rule.put("trigger", JSON_ARGS_PREFIX + dep.getKey());
                  rule.put("dependent", JSON_ARGS_PREFIX + requiredName);
                  rule.put("depRuleId", globalParamIndex++);
                  depRules.add(rule);
                }
              }
              param.vendorExtensions.put("x-oashield-dependentRules", depRules);
            }
          }

          for (CodegenProperty prop : flattenedProperties) {
            decorateBodyProperty(prop, argsAllowlist);
          }

          // Add the flattened properties to the parameter
          param.vendorExtensions.put(MODSECURITY_MODEL_PROPERTIES, flattenedProperties);
        } else if (param.isBodyParam && param.isArray) {
          // Root-level JSON array body: flatten as an array at the root. Element
          // index 0 stands in for every element (generalized to a regex later);
          // without this the ARGS_NAMES allowlist is empty and every element key
          // is rejected as an unknown parameter.
          // Coraza also lists the bare "json" container node in ARGS_NAMES for
          // root arrays (it does not for object bodies).
          argsAllowlist.add("json");
          List<CodegenProperty> flattenedProperties = new ArrayList<CodegenProperty>();
          List<CodegenProperty> itemVars = null;
          if (param.items != null) {
            if (param.items.vars != null && !param.items.vars.isEmpty()) {
              itemVars = param.items.vars;
            } else {
              itemVars = lookupModelVars(param.items, modelLookup);
            }
          }
          if (itemVars != null) {
            for (CodegenProperty prop : itemVars) {
              flattenedProperties.addAll(flattenModel(prop, JSON_ARGS_PREFIX + "0.", 2, modelLookup));
            }
          } else if (param.items != null) {
            // root array of primitives: element keys are json.0 / json.array_0
            CodegenProperty leaf = flattenedLeaf(param.items, JSON_ARGS_PREFIX);
            leaf.baseName = JSON_ARGS_PREFIX + "0";
            flattenedProperties.add(leaf);
          }
          for (CodegenProperty prop : flattenedProperties) {
            decorateBodyProperty(prop, argsAllowlist);
          }
          param.vendorExtensions.put(MODSECURITY_MODEL_PROPERTIES, flattenedProperties);
        }

        if (param.isQueryParam || param.isFormParam) {
          argsAllowlist.add(escapeRegexLiteral(param.baseName));
        }

        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MIN, (param.getMinItems() != null));
        param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MAX, (param.getMaxItems() != null));
        for (int i=1; i<=MODSECURITY_INDEX_MAX;i++) {
          param.vendorExtensions.put(MODSECURITY_INDEX_KEY + "_" + i, globalParamIndex++);
        }

        String patternString = sanitizeSpecPattern(param.pattern);

        if(patternString != null && !patternString.isEmpty()) {
          LOGGER.debug("Config pattern string used: {}", patternString);
          if(isInvalidPattern(patternString)) {
            LOGGER.warn("Invalid pattern string: {}", patternString);
            patternString = null;
          }

        }

        if(patternString == null || patternString.isEmpty()) {
          // anyOf/oneOf parameter: a value matching any member schema is valid, so
          // the members' patterns are combined into one alternation.
          List<CodegenProperty> paramUnion = unionMembers(param.getComposedSchemas());
          if (paramUnion != null) {
            patternString = patternGenerationService.getComposedPattern(paramUnion, param.required);
          } else if (param.isArray && param.items != null) {
            // array parameters validate each value against the ITEM schema (the
            // param's own flags describe the container, not the elements)
            String itemPattern = sanitizeSpecPattern(param.items.pattern);
            if (itemPattern == null || itemPattern.isEmpty() || isInvalidPattern(itemPattern)) {
              itemPattern = patternGenerationService.getPropertyPattern(param.items);
            }
            patternString = itemPattern;
          } else {
            patternString = getParamPattern(param);
          }
          LOGGER.debug("Calculated pattern string {}", patternString);
        }
        // content: parameter — the value is an encoded document (e.g. JSON in a
        // query string) that the engines do not parse per-field; cap its length
        // and rely on the allowlist entry for the name.
        if (param.getContent() != null && !param.getContent().isEmpty() && !param.isBodyParam) {
          // RE2 (Coraza) rejects repeat counts above 1000, so the cap is clamped
          int cap = Math.min(param.getMaxLength() != null ? param.getMaxLength() : 1000, 1000);
          patternString = "^[\\s\\S]{0," + cap + "}$";
        }
        // multipleOf: power-of-10 multiples of integers are expressible as a
        // trailing-zeros pattern; anything else is enforced via schema.json only.
        int paramZeros = powerOfTenZeros(param.getMultipleOf());
        if (paramZeros > 0 && !param.isArray && (param.isInteger || param.isLong)) {
          patternString = "^(?:0|[0-9]{1," + (19 - paramZeros) + "}0{" + paramZeros + "})$";
        }
        // explode=false arrays arrive as ONE delimited value (CSV / space / pipe
        // per style), so validate the joined form and suppress the per-value
        // count rules, whose &ARGS count would always be 1.
        if (param.isArray && !param.isExplode && (param.isQueryParam || param.isFormParam)
            && patternString != null && !patternString.isEmpty()) {
          patternString = buildJoinedArrayPattern(param, patternString);
          param.vendorExtensions.put("x-codegen-joinedArray", true);
          param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MIN, false);
          param.vendorExtensions.put(MODSECURITY_HAS_ARRAY_MAX, false);
        }
        // deepObject query params serialize as name[prop]=value: allow those keys
        // (the scalar ARGS_GET:name rules no-op on an empty collection).
        if (param.isDeepObject && param.isQueryParam) {
          argsAllowlist.add(escapeRegexLiteral(param.baseName) + "\\[[^\\]]{1,64}\\]");
        }
        // allowEmptyValue: an empty value is explicitly valid for this parameter
        if (param.isAllowEmptyValue && patternString != null && !patternString.isEmpty()) {
          patternString = "^(?:" + stripAnchors(patternString) + ")?$";
        }
        // Always write back: spec-provided patterns arrive DefaultCodegen-mangled
        // (/.../-delimited, backslashes doubled), and the template and
        // buildPathMatchRegex read param.pattern directly.
        param.setPattern(patternString);
        LOGGER.debug("param: {}, validation: {}, pattern: {}", param.hasValidation, param.pattern);
        LOGGER.debug("Parameter: {}, data type: {}, isString: {}, max length: {}", param.baseName, param.getDataType(),
            param.isString, param.getMaxLength());

      }

      // Security-scheme parameters never appear in allParams; an apiKey in the
      // query string must not be rejected as an unknown parameter. Header/cookie
      // keys need no exemption (undeclared headers and cookies are not blocked).
      if (co.authMethods != null) {
        for (org.openapitools.codegen.CodegenSecurity auth : co.authMethods) {
          if (Boolean.TRUE.equals(auth.isApiKey) && Boolean.TRUE.equals(auth.isKeyInQuery)
              && auth.keyParamName != null) {
            argsAllowlist.add(escapeRegexLiteral(auth.keyParamName));
          }
        }
      }

      // One regex matches the route AND validates path parameter values: each {param}
      // is replaced with that parameter's validation pattern. Works on both engines,
      // unlike the Coraza-only @restpath/ARGS_PATH (issue #42). Must run after the
      // param loop so parameter patterns exist.
      co.vendorExtensions.put(MODSECURITY_PATH_REGEX_KEY, basePathRegex + buildPathMatchRegex(co));
      co.vendorExtensions.put(MODSECURITY_ARGS_ALLOWLIST, String.join("|", argsAllowlist));
    }

    Map<String, Object> vendorExtensions = new HashMap<String, Object>();
    vendorExtensions.put(MODSECURITY_INDEX_KEY, globalIndex++);
    results.put(VENDOR_EXTENSIONS_KEY, vendorExtensions);

    return results;
  }

  public List<CodegenProperty> flattenModel(CodegenProperty currentProperty, String baseNamePrefix) {
    return flattenModel(currentProperty, baseNamePrefix, 1, java.util.Collections.<String, CodegenModel>emptyMap());
  }

  public List<CodegenProperty> flattenModel(CodegenProperty currentProperty, String baseNamePrefix, int depth,
      Map<String, CodegenModel> modelLookup) {
    List<CodegenProperty> properties = new ArrayList<CodegenProperty>();

    // Bounded recursion: cyclic/self-referencing models would otherwise never terminate.
    if (depth > MAX_FLATTEN_DEPTH) {
      LOGGER.warn("Model nesting deeper than {} levels at '{}{}'; deeper properties are not validated per-field",
          MAX_FLATTEN_DEPTH, baseNamePrefix, currentProperty.baseName);
      return properties;
    }

    // Map / free-form object: arbitrary keys are legal beneath this path, so it
    // becomes a wildcard leaf instead of being dropped (free-form objects have no
    // resolvable vars and previously produced nothing, blocking every key).
    if (currentProperty.isMap || currentProperty.isFreeFormObject) {
      properties.add(flattenedLeaf(currentProperty, baseNamePrefix));
      return properties;
    }

    if (currentProperty.isArray) {
      List<CodegenProperty> itemVars = null;
      if (currentProperty.vars != null && !currentProperty.vars.isEmpty()) {
        // inline item schema: vars carries the element properties
        if (currentProperty.vars.get(0).isPrimitiveType) {
          properties.add(flattenedLeaf(currentProperty, baseNamePrefix));
          return properties;
        }
        itemVars = currentProperty.vars;
      } else if (currentProperty.items != null) {
        // $ref item schema: resolve the referenced model's properties
        itemVars = lookupModelVars(currentProperty.items, modelLookup);
        if (itemVars == null) {
          // array of primitives: one rule covers every element via an index selector
          properties.add(flattenedLeaf(currentProperty, baseNamePrefix));
          return properties;
        }
      }
      LOGGER.debug("Flattening array of model property: {}", currentProperty.baseName);
      if (itemVars != null) {
        // Element index is generalized to a regex later, so flattening index 0 stands in
        // for every element.
        for (CodegenProperty prop : itemVars) {
          properties.addAll(flattenModel(prop, baseNamePrefix + currentProperty.baseName + ".0.", depth + 1, modelLookup));
        }
      }
      return properties;
    }

    if (currentProperty.isModel) {
      LOGGER.debug("Flattening model property: {}", currentProperty.baseName);
      CodegenModel refModel = currentProperty.complexType != null ? modelLookup.get(currentProperty.complexType) : null;
      List<CodegenProperty> union = refModel != null ? unionMembers(refModel.getComposedSchemas()) : null;

      boolean unionHasModel = false;
      if (union != null) {
        for (CodegenProperty member : union) {
          unionHasModel |= member.isModel;
        }
        if (!unionHasModel) {
          // anyOf/oneOf of primitives: a single leaf validated against the
          // alternation of the member patterns
          CodegenProperty leaf = flattenedLeaf(currentProperty, baseNamePrefix);
          leaf.pattern = patternGenerationService.getComposedPattern(union, true);
          properties.add(leaf);
          return properties;
        }
      }

      List<CodegenProperty> vars = (currentProperty.vars != null && !currentProperty.vars.isEmpty())
          ? currentProperty.vars
          : lookupModelVars(currentProperty, modelLookup);
      baseNamePrefix += currentProperty.baseName + ".";
      if (vars == null) {
        LOGGER.warn("No properties resolvable for model property '{}'; its fields are not validated per-field",
            currentProperty.baseName);
        return properties;
      }
      for (CodegenProperty prop : vars) {
        properties.addAll(flattenModel(prop, baseNamePrefix, depth + 1, modelLookup));
      }
      if (union != null) {
        // oneOf/anyOf of models: vars holds the union of all branches, only one of
        // which must be present, so no branch property can be individually required
        for (CodegenProperty prop : properties) {
          prop.required = false;
        }
      }
      return properties;
    }

    LOGGER.debug("Adding property: {}", currentProperty.baseName);
    properties.add(flattenedLeaf(currentProperty, baseNamePrefix));
    return properties;
  }

  // Un-normalized spec parse, lazily created: openapi-generator's normalizer
  // rewrites this.openAPI in place (e.g. it drops 3.1 prefixItems), so raw
  // keyword lookups re-read the original document.
  private io.swagger.v3.oas.models.OpenAPI rawOpenAPI;

  private io.swagger.v3.oas.models.OpenAPI rawOpenAPI() {
    if (rawOpenAPI == null) {
      String spec = getInputSpec();
      if (spec != null && !spec.isEmpty()) {
        try {
          io.swagger.v3.parser.core.models.ParseOptions options =
              new io.swagger.v3.parser.core.models.ParseOptions();
          options.setResolve(true);
          io.swagger.v3.parser.core.models.SwaggerParseResult result =
              new io.swagger.v3.parser.OpenAPIV3Parser().readLocation(spec, null, options);
          rawOpenAPI = result != null ? result.getOpenAPI() : null;
        } catch (Exception e) {
          LOGGER.warn("Could not re-parse spec '{}' for raw keyword lookups: {}", spec, e.getMessage());
        }
      }
      if (rawOpenAPI == null) {
        rawOpenAPI = this.openAPI;
      }
    }
    return rawOpenAPI;
  }

  /**
   * Look up a raw parsed spec schema by component name (null-safe).
   */
  io.swagger.v3.oas.models.media.Schema<?> rawSchemaByName(String name) {
    io.swagger.v3.oas.models.OpenAPI raw = name != null ? rawOpenAPI() : null;
    if (raw == null || raw.getComponents() == null || raw.getComponents().getSchemas() == null) {
      return null;
    }
    return raw.getComponents().getSchemas().get(name);
  }

  private io.swagger.v3.oas.models.media.Schema<?> resolveRawRef(io.swagger.v3.oas.models.media.Schema<?> schema) {
    if (schema != null && schema.get$ref() != null) {
      String ref = schema.get$ref();
      io.swagger.v3.oas.models.media.Schema<?> resolved =
          rawSchemaByName(ref.substring(ref.lastIndexOf('/') + 1));
      return resolved != null ? resolved : schema;
    }
    return schema;
  }

  /**
   * Walk a raw spec schema along a flattened body path ("json.tags.0.name") to
   * the schema of that leaf; "0" segments descend into array items. Returns null
   * when the path cannot be resolved.
   */
  io.swagger.v3.oas.models.media.Schema<?> rawSchemaForPath(
      io.swagger.v3.oas.models.media.Schema<?> root, String flatPath) {
    io.swagger.v3.oas.models.media.Schema<?> current = resolveRawRef(root);
    String[] segments = flatPath.split("\\.");
    for (int i = 1; i < segments.length && current != null; i++) { // segment 0 is the "json" prefix
      if ("0".equals(segments[i])) {
        current = resolveRawRef(current.getItems());
      } else {
        Map<String, io.swagger.v3.oas.models.media.Schema> props = current.getProperties();
        current = props != null ? resolveRawRef(props.get(segments[i])) : null;
      }
    }
    return current;
  }

  /**
   * Convert a patternProperties NAME regex into the fragment matching that name
   * inside a flattened ARGS key (unanchored ends admit surrounding characters).
   */
  static String patternPropertiesNameRegex(String namePattern) {
    return (namePattern.startsWith("^") ? "" : "[^.]*")
        + stripAnchors(namePattern)
        + (namePattern.endsWith("$") && !namePattern.endsWith("\\$") ? "" : "[^.]*");
  }

  /**
   * Value pattern for a raw patternProperties value schema (primitive types only;
   * anything structured is admitted by name and validated via schema.json).
   */
  String rawValuePattern(io.swagger.v3.oas.models.media.Schema<?> valueSchema) {
    io.swagger.v3.oas.models.media.Schema<?> schema = resolveRawRef(valueSchema);
    if (schema == null) {
      return "^.*$";
    }
    if (schema.getPattern() != null && !isInvalidPattern(schema.getPattern())) {
      return schema.getPattern();
    }
    String type = schema.getType();
    if (type == null && schema.getTypes() != null && !schema.getTypes().isEmpty()) {
      type = schema.getTypes().iterator().next();
    }
    if ("integer".equals(type)) {
      return "^-?[0-9]{1,19}$";
    }
    if ("number".equals(type)) {
      return "^-?([0-9]{1,15}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})$";
    }
    if ("boolean".equals(type)) {
      return "^(true|false)$";
    }
    return "^.*$";
  }

  /**
   * Classify a request media type into the template section that handles it.
   * JSON and XML get body validation; form-urlencoded/multipart rely on the
   * ARGS_POST parameter rules; "*&#47;*" accepts anything; everything else is an
   * uninspectable declared type governed by unknownMediaTypePolicy.
   */
  public static String classifyMediaType(String mediaType) {
    if (mediaType == null) {
      return CONSUME_OTHER;
    }
    String mt = mediaType.trim().toLowerCase(java.util.Locale.ROOT);
    if (mt.startsWith("*/*")) {
      return CONSUME_WILDCARD;
    }
    if (mt.matches("^application/(?:[a-z0-9.+-]+\\+)?json\\b.*")) {
      return CONSUME_JSON;
    }
    if (mt.matches("^(?:application|text)/(?:[a-z0-9.+-]+\\+)?xml\\b.*")) {
      return CONSUME_XML;
    }
    if (mt.startsWith("application/x-www-form-urlencoded") || mt.startsWith("multipart/")) {
      return CONSUME_FORM_LIKE;
    }
    return CONSUME_OTHER;
  }

  /**
   * Resolve the raw spec requestBody.required for an operation. CodegenOperation
   * does not expose it for form-param operations (their body param is dissolved
   * into formParams), so read it from the parsed OpenAPI document.
   */
  private boolean isRequestBodyRequired(CodegenOperation co) {
    if (this.openAPI == null || this.openAPI.getPaths() == null) {
      return false;
    }
    io.swagger.v3.oas.models.PathItem pathItem = this.openAPI.getPaths().get(co.path);
    if (pathItem == null) {
      return false;
    }
    io.swagger.v3.oas.models.Operation rawOp;
    try {
      rawOp = pathItem.readOperationsMap()
          .get(io.swagger.v3.oas.models.PathItem.HttpMethod
              .valueOf(co.httpMethod.toUpperCase(java.util.Locale.ROOT)));
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (rawOp == null || rawOp.getRequestBody() == null) {
      return false;
    }
    io.swagger.v3.oas.models.parameters.RequestBody body =
        org.openapitools.codegen.utils.ModelUtils.getReferencedRequestBody(this.openAPI, rawOp.getRequestBody());
    return body != null && Boolean.TRUE.equals(body.getRequired());
  }

  /**
   * Collect the oneOf/anyOf members of a composed schema. Both keywords get the
   * same WAF treatment (a value passing any member is allowed), so they are merged.
   * Returns null when the schema is not a oneOf/anyOf composition.
   */
  static List<CodegenProperty> unionMembers(org.openapitools.codegen.CodegenComposedSchemas composedSchemas) {
    if (composedSchemas == null) {
      return null;
    }
    List<CodegenProperty> members = new ArrayList<CodegenProperty>();
    if (composedSchemas.getOneOf() != null) {
      members.addAll(composedSchemas.getOneOf());
    }
    if (composedSchemas.getAnyOf() != null) {
      members.addAll(composedSchemas.getAnyOf());
    }
    return members.isEmpty() ? null : members;
  }

  /**
   * Resolve the referenced model's properties for a $ref property (whose own vars
   * list is empty). Returns null when the property is not a model reference or the
   * model is unknown.
   */
  private static List<CodegenProperty> lookupModelVars(CodegenProperty prop, Map<String, CodegenModel> modelLookup) {
    if (prop.complexType == null) {
      return null;
    }
    CodegenModel model = modelLookup.get(prop.complexType);
    return model != null ? model.vars : null;
  }

  /**
   * Number of trailing zeros for an integer power-of-10 multipleOf (10 -> 1,
   * 100 -> 2, ...); -1 when the value is not a power of ten >= 10.
   */
  public static int powerOfTenZeros(Number multipleOf) {
    if (multipleOf == null) {
      return -1;
    }
    java.math.BigDecimal value;
    try {
      value = new java.math.BigDecimal(multipleOf.toString()).stripTrailingZeros();
    } catch (NumberFormatException e) {
      return -1;
    }
    // a power of ten >= 10 strips to unscaled value 1 with negative scale
    if (java.math.BigDecimal.ONE.compareTo(new java.math.BigDecimal(value.unscaledValue())) != 0
        || value.scale() >= 0) {
      return -1;
    }
    return -value.scale();
  }

  /**
   * Copy a leaf property with the flattened name instead of mutating it: models are
   * shared between operations, so in-place mutation would double-prefix the second
   * operation that references the same model (json.json.id).
   */
  private static CodegenProperty flattenedLeaf(CodegenProperty prop, String baseNamePrefix) {
    CodegenProperty flat = prop.clone();
    flat.baseName = baseNamePrefix + prop.baseName;
    flat.vendorExtensions = new HashMap<String, Object>(
        prop.vendorExtensions != null ? prop.vendorExtensions : java.util.Collections.<String, Object>emptyMap());
    return flat;
  }

  /**
   * Escape a literal string for safe embedding in an RE2/PCRE regex.
   */
  public static String escapeRegexLiteral(String literal) {
    return literal.replaceAll("([.^$+?*()\\[\\]{}|\\\\])", "\\\\$1");
  }

  /**
   * Undo DefaultCodegen.toRegularExpression() mangling of spec-provided patterns:
   * it wraps them in /.../ delimiters and doubles backslashes, neither of which
   * belongs in a SecRule @rx operand.
   */
  public static String sanitizeSpecPattern(String pattern) {
    if (pattern == null) {
      return null;
    }
    String result = pattern;
    if (result.length() >= 2 && result.startsWith("/") && result.endsWith("/")) {
      result = result.substring(1, result.length() - 1);
      result = result.replace("\\\\", "\\");
    }
    return result;
  }

  /**
   * Build the operation's path-match regex: literal segments are regex-escaped and
   * each {param} placeholder is replaced with that path parameter's validation
   * pattern (anchors stripped). Unknown parameters fall back to a single segment.
   * matrix/label style path params include their style prefix in the match.
   */
  String buildPathMatchRegex(CodegenOperation co) {
    Map<String, CodegenParameter> pathParams = new HashMap<String, CodegenParameter>();
    for (CodegenParameter param : co.allParams) {
      if (param.isPathParam) {
        pathParams.put(param.baseName, param);
      }
    }

    StringBuilder regex = new StringBuilder();
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^/{}]+)\\}").matcher(co.path);
    int last = 0;
    while (m.find()) {
      regex.append(escapeRegexLiteral(co.path.substring(last, m.start())));
      CodegenParameter param = pathParams.get(m.group(1));
      String core;
      if (param != null && param.pattern != null && !param.pattern.isEmpty()) {
        core = "(?:" + stripAnchors(param.pattern) + ")";
      } else {
        core = "[^/]+";
      }
      if (param != null && "matrix".equals(param.style)) {
        core = ";" + escapeRegexLiteral(m.group(1)) + "=" + core;
      } else if (param != null && "label".equals(param.style)) {
        core = "\\." + core;
      }
      regex.append(core);
      last = m.end();
    }
    regex.append(escapeRegexLiteral(co.path.substring(last)));
    return regex.toString();
  }

  /**
   * Joined (explode=false) array parameter pattern: the whole delimited list in a
   * single value, item repetitions bounded by minItems/maxItems.
   */
  String buildJoinedArrayPattern(CodegenParameter param, String itemPattern) {
    String sep = ",";
    if ("spaceDelimited".equals(param.style)) {
      sep = " ";
    } else if ("pipeDelimited".equals(param.style)) {
      sep = "\\|";
    }
    String item = "(?:" + stripAnchors(itemPattern) + ")";
    int lo = param.getMinItems() != null && param.getMinItems() > 0 ? param.getMinItems() - 1 : 0;
    // ponytail: 999-item ceiling keeps the quantifier bounded (ReDoS hygiene)
    String hi = param.getMaxItems() != null && param.getMaxItems() > 0
        ? String.valueOf(param.getMaxItems() - 1)
        : "999";
    return "^" + item + "(?:" + sep + item + "){" + lo + "," + hi + "}$";
  }

  /**
   * Regex prefix for the deployed base path: the basePath CLI option when set
   * (empty string disables prefixing), otherwise the path component of the first
   * servers.url. Server-URL template variables match one path segment.
   */
  String buildBasePathRegex() {
    String path = basePathOverride;
    if (path == null && this.openAPI != null && this.openAPI.getServers() != null
        && !this.openAPI.getServers().isEmpty()) {
      String url = this.openAPI.getServers().get(0).getUrl();
      if (url != null) {
        if (url.startsWith("/")) {
          path = url;
        } else {
          try {
            // {variables} are not URI-legal; neutralize for parsing, then restore
            String parsed = java.net.URI.create(url.replace("{", "%7B").replace("}", "%7D")).getPath();
            path = parsed != null ? parsed.replace("%7B", "{").replace("%7D", "}") : null;
          } catch (IllegalArgumentException e) {
            LOGGER.warn("Cannot parse server URL '{}'; no base path prefix applied", url);
          }
        }
      }
    }
    if (path == null) {
      return "";
    }
    path = path.trim();
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (path.isEmpty()) {
      return "";
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    StringBuilder regex = new StringBuilder();
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{[^/{}]+\\}").matcher(path);
    int last = 0;
    while (m.find()) {
      regex.append(escapeRegexLiteral(path.substring(last, m.start()))).append("[^/]+");
      last = m.end();
    }
    regex.append(escapeRegexLiteral(path.substring(last)));
    return regex.toString();
  }

  static String stripAnchors(String pattern) {
    String result = pattern;
    if (result.startsWith("^")) {
      result = result.substring(1);
    }
    if (result.endsWith("$") && !result.endsWith("\\$")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  /**
   * Decorate a flattened JSON body property with the vendor extensions the template
   * needs to emit per-field rules, and add its name pattern to the operation's
   * ARGS_NAMES allowlist.
   *
   * x-oashield-argTarget is either the literal ARGS key ("json.category.name") or,
   * when the path crosses an array, a regex selector ("/^json\.tags\.(?:array_)?\d{1,9}\.name$/")
   * that matches both engines' array key forms.
   */
  private void decorateBodyProperty(CodegenProperty prop, java.util.Collection<String> argsAllowlist) {
    String path = prop.baseName;
    boolean indexedPath = path.matches(".*\\.0(\\..*|$)");
    boolean indexed = indexedPath || prop.isArray;

    StringBuilder body = new StringBuilder();
    String[] segments = path.split("\\.");
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        body.append("\\.");
      }
      body.append(segments[i].equals("0") ? ARRAY_INDEX_REGEX : escapeRegexLiteral(segments[i]));
      // Coraza also lists container nodes (json.photoUrls, json.category, ...) in
      // ARGS_NAMES, not just leaves, so every intermediate prefix must be allowed.
      if (i > 0) {
        argsAllowlist.add(body.toString());
      }
    }
    if (prop.isArray) {
      // array of primitives: actual arg keys carry an index suffix (json.items.0 / json.items.array_0)
      body.append("\\.").append(ARRAY_INDEX_REGEX);
      argsAllowlist.add(body.toString());
      // minItems/maxItems as element-count rules. Only for arrays not nested in
      // another array (per-element counts are ambiguous) — and this leaf form only
      // exists for primitive-item arrays, whose indexed keys are countable on both
      // engines (object arrays flatten to per-field leaves with no index-only key
      // on ModSecurity3).
      if (!indexedPath && (prop.getMinItems() != null || prop.getMaxItems() != null)) {
        prop.vendorExtensions.put("x-oashield-countSelector", "/(?i)^" + body + "$/");
        // minItems only for REQUIRED arrays: an absent optional array also counts
        // 0 and there is no per-field way to distinguish absent from empty on
        // ModSecurity3 (schema.json covers optional arrays on Coraza).
        if (prop.getMinItems() != null && prop.required) {
          prop.vendorExtensions.put("x-oashield-countMin", prop.getMinItems());
        }
        // maxItems is safe unconditionally: absent counts 0, never above the max
        if (prop.getMaxItems() != null) {
          prop.vendorExtensions.put("x-oashield-countMax", prop.getMaxItems());
        }
      }
    }

    // Map / free-form object: any key beneath this path is legal, so the allowlist
    // gets a bounded wildcard subtree. Values are validated only for maps with a
    // primitive value schema; deeper structures are covered by the wildcard alone.
    if (prop.isMap || prop.isFreeFormObject) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> ppRules =
          (List<Map<String, Object>>) prop.vendorExtensions.get("x-oashield-patternProps");
      if (ppRules != null) {
        // patternProperties: only keys matching a declared name pattern are
        // admitted (no broad wildcard) and their values are validated per entry
        for (Map<String, Object> rule : ppRules) {
          String nameRegex = (String) rule.get("nameRegex");
          argsAllowlist.add(body + "\\." + nameRegex);
          rule.put("selector", "/(?i)^" + body + "\\." + nameRegex + "$/");
          rule.put("ruleId", globalParamIndex++);
        }
        for (int i = 1; i <= PROP_INDEX_MAX; i++) {
          prop.vendorExtensions.put(PROP_INDEX_KEY + "_" + i, globalParamIndex++);
        }
        return;
      }
      argsAllowlist.add(body + "\\..{1,256}");
      CodegenProperty valueSchema = prop.items;
      if (prop.isMap && valueSchema != null
          && !valueSchema.isModel && !valueSchema.isMap && !valueSchema.isArray
          && !valueSchema.isFreeFormObject) {
        prop.vendorExtensions.put("x-oashield-argTarget", "/(?i)^" + body + "\\.[^.]{1,64}$/");
        String valuePattern = sanitizeSpecPattern(valueSchema.pattern);
        if (valuePattern == null || valuePattern.isEmpty() || isInvalidPattern(valuePattern)) {
          valuePattern = patternGenerationService.getPropertyPattern(valueSchema);
        }
        prop.vendorExtensions.put("x-oashield-pattern", valuePattern);
      }
      // No required-presence rule: an empty map produces no ARGS keys on
      // ModSecurity3, making {} indistinguishable from an absent property.
      for (int i = 1; i <= PROP_INDEX_MAX; i++) {
        prop.vendorExtensions.put(PROP_INDEX_KEY + "_" + i, globalParamIndex++);
      }
      return;
    }

    prop.vendorExtensions.put("x-oashield-argTarget", indexed ? "/(?i)^" + body + "$/" : path);

    // Type pattern: for arrays validate each element against the item type
    CodegenProperty typeSource = prop;
    if (prop.isArray) {
      if (prop.vars != null && !prop.vars.isEmpty()) {
        typeSource = prop.vars.get(0);
      } else if (prop.items != null) {
        typeSource = prop.items;
      }
    }
    String pattern = sanitizeSpecPattern(typeSource.pattern);
    if (pattern == null || pattern.isEmpty() || isInvalidPattern(pattern)) {
      pattern = patternGenerationService.getPropertyPattern(typeSource);
    }
    // multipleOf: power-of-10 integer multiples become a trailing-zeros pattern
    int propZeros = powerOfTenZeros(typeSource.multipleOf);
    if (propZeros > 0 && (typeSource.isInteger || typeSource.isLong)) {
      pattern = "^(?:0|[0-9]{1," + (19 - propZeros) + "}0{" + propZeros + "})$";
    }
    if (typeSource.isNullable && pattern != null && !pattern.isEmpty()) {
      // JSON null flattens to a present key with an EMPTY value on both engines
      // (docs/engine-behavior.md), so nullable values must accept empty.
      pattern = "^(?:" + stripAnchors(pattern) + ")?$";
    }
    prop.vendorExtensions.put("x-oashield-pattern", pattern);

    // Required-presence rules only for non-array paths: per-element "required" has no
    // meaningful &-count form. Nested required properties are guarded on their parent
    // being present (JSON Schema semantics: required applies only within its object).
    // readOnly properties may legally be omitted from requests even when required.
    if (prop.required && !indexed && !prop.isReadOnly) {
      prop.vendorExtensions.put("x-oashield-requiredRule", true);
      String parent = path.substring(0, path.lastIndexOf('.') + 1);
      if (!parent.equals(JSON_ARGS_PREFIX)) {
        prop.vendorExtensions.put("x-oashield-parentSelector", "/(?i)^" + escapeRegexLiteral(parent) + "/");
      }
    }

    for (int i = 1; i <= PROP_INDEX_MAX; i++) {
      prop.vendorExtensions.put(PROP_INDEX_KEY + "_" + i, globalParamIndex++);
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

  /**
   * Process all models and generate JSON Schema. (The per-ModelsMap
   * postProcessModels hook is deliberately NOT overridden: it used to overwrite
   * schema.json with partial single-model content on every call before
   * postProcessAllModels wrote the combined file.)
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

    if (validateXmlSchema) {
      generateXmlSchema(result);
    }

    return result;
  }

  /**
   * Generate the XSD from models (only when validateXmlSchema is enabled).
   */
  private void generateXmlSchema(Map<String, ModelsMap> models) {
    LOGGER.info("Generating XSD from models...");
    try {
      String xsd = new XsdGenerator().generateXsd(models);
      File outputDir = new File(outputFolder);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
      File xsdFile = new File(outputDir, xsdOutputFile);
      try (FileWriter writer = new FileWriter(xsdFile)) {
        writer.write(xsd);
      }
      LOGGER.info("XSD generated successfully: {}", xsdFile.getAbsolutePath());
    } catch (Exception e) {
      LOGGER.error("Error generating XSD", e);
    }
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
      // No root "type": the document is a definitions container and the request
      // body may legally be an object OR an array (root-array bodies would fail
      // a type:object root under Coraza's @validateSchema).
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

        // Process the model and add it to the definitions, enriched with the raw
        // spec keywords the codegen abstractions do not surface
        io.swagger.v3.oas.models.media.Schema<?> rawSchema = rawSchemaByName(model.name);
        if (rawSchema == null) {
          rawSchema = rawSchemaByName(modelName);
        }
        ObjectNode modelSchema = generator.generateModelSchema(model, rawSchema);
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

    // Engine flavor and body validation options
    additionalProperties.put("isCoraza", false);
    additionalProperties.put("isModsec3", true);
    additionalProperties.put("validateBodySchema", validateBodySchema);
    additionalProperties.put("schemaRulePath", jsonSchemaOutputFile);
    cliOptions.add(new CliOption(ENGINE_FLAVOR,
        "Target WAF engine: 'modsecurity3' (per-field JSON body rules) or 'coraza' (adds @validateSchema)")
        .defaultValue(FLAVOR_MODSECURITY3));
    cliOptions.add(new CliOption("validateBodySchema", "Emit request body validation rules")
        .defaultValue(Boolean.toString(validateBodySchema)));
    cliOptions.add(new CliOption("schemaRulePath",
        "Schema file path as referenced from the generated @validateSchema rule (coraza flavor); "
            + "resolved by Coraza relative to the server working directory")
        .defaultValue(jsonSchemaOutputFile));

    // Deny behavior and logging options (issue #16)
    additionalProperties.put("includeEngineConfig", includeEngineConfig);
    additionalProperties.put("logAction", "log,auditlog");
    additionalProperties.put("denyActionDirective", "deny,status:" + denyStatus);
    cliOptions.add(new CliOption("denyAction",
        "Disruptive action applied when a rule blocks: 'deny', 'drop', 'redirect' or 'pass' (detection-only)")
        .defaultValue(denyAction));
    cliOptions.add(new CliOption("denyStatus",
        "HTTP status returned on deny (use a 3xx with denyAction=redirect)")
        .defaultValue(Integer.toString(denyStatus)));
    cliOptions.add(new CliOption("denyRedirectUrl",
        "Absolute URL to redirect to; required when denyAction=redirect"));
    cliOptions.add(new CliOption("enableLogging",
        "Emit log,auditlog on generated rules; false emits nolog")
        .defaultValue(Boolean.toString(enableLogging)));
    cliOptions.add(new CliOption("includeEngineConfig",
        "Emit SecRuleEngine/SecRequestBodyAccess/SecDefaultAction in mainconfig.conf; "
            + "set false when your existing WAF configuration already defines them")
        .defaultValue(Boolean.toString(includeEngineConfig)));
    additionalProperties.put("blockOtherMedia", false);
    cliOptions.add(new CliOption("unknownMediaTypePolicy",
        "Handling of declared request media types the WAF cannot inspect "
            + "(e.g. application/octet-stream, text/plain): 'pass' or 'block'")
        .defaultValue(unknownMediaTypePolicy));
    cliOptions.add(new CliOption("basePath",
        "Base path prefix for all generated path-match rules; defaults to the path "
            + "component of the first servers.url, use an empty string to disable"));
    additionalProperties.put("validateXmlSchema", false);
    additionalProperties.put("xsdRulePath", xsdOutputFile);
    cliOptions.add(new CliOption("validateXmlSchema",
        "Generate an XSD from the models and emit @validateSchema XML rules "
            + "(modsecurity3 flavor). Default false: current libmodsecurity3 cannot "
            + "load XSDs at request time and Coraza has no XML support")
        .defaultValue(Boolean.toString(validateXmlSchema)));
    cliOptions.add(new CliOption("xsdOutputFile", "XSD output file name")
        .defaultValue(xsdOutputFile));
    cliOptions.add(new CliOption("xsdRulePath",
        "XSD path as referenced from the generated @validateSchema XML rule")
        .defaultValue(xsdOutputFile));

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
