package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service responsible for generating ModSecurity parameter patterns.
 * This class encapsulates pattern generation logic extracted from Modsecurity3Generator.
 */
public class PatternGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternGenerationService.class);

    /**
     * Generates a regex string to validate the input of a parameter.
     *
     * @param param The parameter to generate a pattern for
     * @return A regex pattern string for validating the parameter
     */
    public String getParamPattern(CodegenParameter param) {
        boolean isRequired = param.required;
        String patternString = "";

        // Integer/Long
        if (param.isInteger || param.isLong) {
            return isRequired ? "^[0-9]{1,19}$" : "^([0-9]{1,19})?$";
        }
        // UUID - SECURITY FIX: RFC 4122 compliant pattern with bounded quantifiers
        else if (param.isUuid) {
            return isRequired ? "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" : "^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})?$";
        }
        // NEW OpenAPI 3.1 Format Types - Direct pattern handling
        else if (isTimeFormat(param)) {
            return isRequired ? "^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?$" : "^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?$";
        }
        else if (isUriFormat(param)) {
            return isRequired ? "^[a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^\\s]{1,2000}$" : "^[a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^\\s]{1,2000}$";
        }
        else if (isUriReferenceFormat(param)) {
            return isRequired ? "^([a-zA-Z][a-zA-Z0-9+.-]{0,31}://)?[^\\s]{1,2000}$" : "^([a-zA-Z][a-zA-Z0-9+.-]{0,31}://)?[^\\s]{1,2000}$";
        }
        else if (isUriTemplateFormat(param)) {
            return isRequired ? "^[^\\s{]{0,1000}(\\{[^}]{1,100}\\}[^\\s{]{0,1000}){0,50}$" : "^[^\\s{]{0,1000}(\\{[^}]{1,100}\\}[^\\s{]{0,1000}){0,50}$";
        }
        else if (isHostnameFormat(param)) {
            return isRequired ? "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.){0,126}[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$" : "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.){0,126}[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$";
        }
        else if (isIpv4Format(param)) {
            return isRequired ? "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$" : "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        }
        else if (isIpv6Format(param)) {
            return isRequired ? "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$" : "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        }
        else if (isJsonPointerFormat(param)) {
            return isRequired ? "^(/(([^/~]|~[01]){0,100})){0,50}$" : "^(/(([^/~]|~[01]){0,100})){0,50}$";
        }
        else if (isRelativeJsonPointerFormat(param)) {
            return isRequired ? "^[0-9]{1,10}(#|(/(([^/~]|~[01]){0,100})){0,50})$" : "^[0-9]{1,10}(#|(/(([^/~]|~[01]){0,100})){0,50})$";
        }
        else if (isByteFormat(param)) {
            return isRequired ? "^[A-Za-z0-9+/]{0,1000}={0,2}$" : "^[A-Za-z0-9+/]{0,1000}={0,2}$";
        }
        else if (isBinaryFormat(param)) {
            return isRequired ? "^[0-9a-fA-F]{0,10000}$" : "^[0-9a-fA-F]{0,10000}$";
        }

        String allowedInputPattern = getAllowedInputPattern(param);
        boolean handleDecimal = isDecimal(param);
        String minLengthPatternString = getMinLengthPatternString(param, isRequired);
        String maxLengthPatternString = getMaxLengthPatternString(param, isRequired);

        // Decimal (float/double/decimal)
        if (handleDecimal) {
            // Always wrap decimal pattern with ^ and $ (test expects this)
            patternString = "^" + getDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, isRequired) + "$";
        }
        // Boolean
        else if (param.getIsBoolean()) {
            patternString = "^" + getNonDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, false, isRequired) + "$";
        }
        // Enum, Email, Date, DateTime
        else if (param.isEnum || param.isEmail || param.isDate || param.isDateTime) {
            patternString = "^" + getNonDecimalPattern(allowedInputPattern, minLengthPatternString, maxLengthPatternString, false, isRequired) + "$";
        }
        // Fallback
        else {
            // Path parameters must not match across segment boundaries, otherwise
            // /user/a/b would route into /user/{username}.
            String anyChar = param.isPathParam ? "[^/]" : ".";
            // Use minLength/maxLength if set for string types
            if (!minLengthPatternString.isEmpty() || !maxLengthPatternString.isEmpty()) {
                String min = minLengthPatternString.isEmpty() ? "0" : minLengthPatternString;
                String pattern;
                if (!min.isEmpty() && !maxLengthPatternString.isEmpty()) {
                    pattern = "^" + anyChar + "{" + min + "," + maxLengthPatternString + "}$";
                } else if (!min.isEmpty() && maxLengthPatternString.isEmpty()) {
                    if (min.equals("1")) {
                        pattern = "^" + anyChar + "+$";
                    } else if (min.equals("0")) {
                        pattern = "^" + anyChar + "*$";
                    } else {
                        pattern = "^" + anyChar + "{" + min + ",}$";
                    }
                } else if (min.isEmpty() && !maxLengthPatternString.isEmpty()) {
                    // If required, min should be 1; if not required, min is 0
                    String minVal = isRequired ? "1" : "0";
                    pattern = "^" + anyChar + "{" + minVal + "," + maxLengthPatternString + "}$";
                } else {
                    pattern = isRequired ? "^" + anyChar + "+$" : "^" + anyChar + "*$";
                }
                patternString = pattern;
            } else {
                patternString = isRequired ? "^" + anyChar + "+$" : "^" + anyChar + "*$";
            }
        }
        return patternString;
    }

    /**
     * Generates a validation pattern for a model property (flattened JSON body field)
     * by adapting it to a CodegenParameter and delegating to getParamPattern.
     * JSON body values are always present when the arg exists, so required=true.
     *
     * @param prop The model property to generate a pattern for
     * @return A regex pattern string for validating the property value
     */
    public String getPropertyPattern(CodegenProperty prop) {
        CodegenParameter param = new CodegenParameter();
        param.baseName = prop.baseName;
        param.required = true;
        param.isInteger = prop.isInteger;
        param.isLong = prop.isLong;
        param.isNumber = prop.isNumber;
        param.isFloat = prop.isFloat;
        param.isDouble = prop.isDouble;
        param.isDecimal = prop.isDecimal;
        param.isBoolean = prop.isBoolean;
        param.isUuid = prop.isUuid;
        param.isDate = prop.isDate;
        param.isDateTime = prop.isDateTime;
        param.isEmail = prop.isEmail;
        param.isEnum = prop.isEnum;
        param.allowableValues = prop.allowableValues;
        param.setMinLength(prop.getMinLength());
        param.setMaxLength(prop.getMaxLength());
        // format detection is via vendorExtensions x-format (CodegenParameter.setFormat is unreliable)
        if (prop.dataFormat != null) {
            param.vendorExtensions.put("x-format", prop.dataFormat);
        }
        return getParamPattern(param);
    }

    /**
     * Builds a validation pattern for an anyOf/oneOf composition: a value is valid
     * when it matches any member schema, so the member patterns are joined into one
     * alternation. Members with a spec-provided pattern use it; others get their
     * type-derived pattern.
     *
     * @param members the composed schema members (oneOf and/or anyOf entries)
     * @param isRequired whether an empty value should be rejected
     * @return an anchored alternation regex covering all members
     */
    public String getComposedPattern(List<CodegenProperty> members, boolean isRequired) {
        List<String> alternatives = new java.util.ArrayList<>();
        for (CodegenProperty member : members) {
            if (member.isNull) {
                continue;
            }
            String pattern = Modsecurity3Generator.sanitizeSpecPattern(member.pattern);
            if (pattern == null || pattern.isEmpty()
                    || pattern.contains("(?!") || pattern.contains("(?=") || pattern.contains("(?<")) {
                pattern = getPropertyPattern(member);
            }
            alternatives.add(Modsecurity3Generator.stripAnchors(pattern));
        }
        if (alternatives.isEmpty()) {
            return isRequired ? "^.+$" : "^.*$";
        }
        return "^(?:" + String.join("|", alternatives) + ")" + (isRequired ? "" : "?") + "$";
    }

    /**
     * Based on the type of parameter, returns the allowed input pattern.
     *
     * @param param The parameter to get the allowed input pattern for
     * @return A regex pattern string for the allowed input
     */
    public String getAllowedInputPattern(CodegenParameter param) {
        if (param.isInteger || param.isLong || param.isNumber || param.isFloat || param.isDouble || param.isDecimal) {
            // Integer/number/float/double/decimal: only digits, min/max handled elsewhere
            return "[0-9]";
        } else if (param.getIsBoolean()) {
            return "(true|false)";
        } else if (param.isUuid) {
            // UUID: RFC 4122 compliant format - SECURITY FIX: bounded quantifiers
            return "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        } else if (param.isDate) {
            // Date: use complex regex from test, anchored
            return "(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d)))";
        } else if (param.isDateTime) {
            // DateTime: use complex regex from test, anchored
            return "(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d))T([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)(\\.\\d+)?([Zz]|[+\\-](0\\d|1[0-4])(:[0-5]\\d)?))";
        } else if (param.isEmail) {
            // Email: use anchored pattern from test
            return "([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})";
        }
        // NEW OpenAPI 3.1 Data Types - SECURITY: All patterns use bounded quantifiers
        else if (isTimeFormat(param)) {
            // Time format: HH:MM:SS with optional fractional seconds and timezone
            return "([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?";
        } else if (isUriFormat(param)) {
            // URI format: bounded pattern for URI validation
            return "[a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^\\s]{1,2000}";
        } else if (isUriReferenceFormat(param)) {
            // URI-reference: can be relative or absolute URI
            return "([a-zA-Z][a-zA-Z0-9+.-]{0,31}://)?[^\\s]{1,2000}";
        } else if (isUriTemplateFormat(param)) {
            // URI template with RFC 6570 variables
            return "[^\\s{]{0,1000}(\\{[^}]{1,100}\\}[^\\s{]{0,1000}){0,50}";
        } else if (isHostnameFormat(param)) {
            // Hostname: RFC 1123 compliant with bounded quantifiers
            return "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.){0,126}[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?";
        } else if (isIpv4Format(param)) {
            // IPv4: precise pattern with octet validation
            return "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        } else if (isIpv6Format(param)) {
            // IPv6: simplified pattern with bounded quantifiers
            return "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}";
        } else if (isJsonPointerFormat(param)) {
            // JSON Pointer: RFC 6901 compliant
            return "(/(([^/~]|~[01]){0,100})){0,50}";
        } else if (isRelativeJsonPointerFormat(param)) {
            // Relative JSON Pointer: non-negative integer + JSON Pointer
            return "[0-9]{1,10}(#|(/(([^/~]|~[01]){0,100})){0,50})";
        } else if (isByteFormat(param)) {
            // Byte: base64 encoded with padding
            return "[A-Za-z0-9+/]{0,1000}={0,2}";
        } else if (isBinaryFormat(param)) {
            // Binary: hex encoded with bounded length
            return "[0-9a-fA-F]{0,10000}";
        } else if (param.isEnum) {
            // Enum values can be Strings or numbers/booleans (integer enums), so
            // treat them as Objects and stringify each one.
            List<?> enumValues = null;
            Object values = param.allowableValues != null ? param.allowableValues.get("values") : null;
            if (values instanceof List) {
                enumValues = (List<?>) values;
            }
            // For empty/null/invalid enum, return "." as per test
            if (enumValues == null || enumValues.isEmpty()) {
                return ".";
            }
            // Only escape if value contains regex metacharacters
            List<String> escapedValues = new java.util.ArrayList<>();
            for (Object o : enumValues) {
                String v = String.valueOf(o);
                if (v.matches("^[a-zA-Z0-9_]+$")) {
                    escapedValues.add(v);
                } else {
                    escapedValues.add(java.util.regex.Pattern.quote(v));
                }
            }
            return "(" + String.join("|", escapedValues) + ")";
        } else {
            // Default: match any character (for string types, per test)
            return ".";
        }
    }

    /**
     * Returns true if the parameter is a decimal type.
     *
     * @param param The parameter to check
     * @return true if the parameter is a decimal type, false otherwise
     */
    public boolean isDecimal(CodegenParameter param) {
        return param.isFloat || param.isDouble || param.isDecimal;
    }

    /**
     * Returns true if the parameter allows multiple character or digits, or
     * if it uses a single complex pattern (such as true/false for booleans,
     * or decimals since they have a set pattern).
     *
     * @param param The parameter to check
     * @return true if the parameter allows multiple values, false otherwise
     */
    public boolean allowMultiple(CodegenParameter param) {
        return !(param.getIsBoolean() ||
                isDecimal(param) ||
                param.isDate ||
                param.isDateTime ||
                param.isEnum ||
                param.isEmail ||
                isTimeFormat(param) ||
                isUriFormat(param) ||
                isUriReferenceFormat(param) ||
                isUriTemplateFormat(param) ||
                isHostnameFormat(param) ||
                isIpv4Format(param) ||
                isIpv6Format(param) ||
                isJsonPointerFormat(param) ||
                isRelativeJsonPointerFormat(param) ||
                isByteFormat(param) ||
                isBinaryFormat(param));
    }

    /**
     * Returns the minimum length pattern string for the parameter.
     * If the parameter is required, the minimum length is set to 1 if it is not already set.
     *
     * @param param The parameter to get the minimum length pattern for
     * @param isRequired Whether the parameter is required
     * @return The minimum length pattern string
     */
    public String getMinLengthPatternString(CodegenParameter param, boolean isRequired) {
        Integer minLength = param.getMinLength();

        // If the parameter is required, and no minimum length is set, we set it to 1.
        if (isRequired) {
            if (minLength == null || minLength == 0) {
                minLength = 1;
            }
        }

        return minLength == null ? "" : minLength.toString();
    }

    /**
     * Returns the maximum length pattern string for the parameter.
     * If the parameter is required, the maximum length is set to 1 if it is not already set.
     *
     * @param param The parameter to get the maximum length pattern for
     * @param isRequired Whether the parameter is required
     * @return The maximum length pattern string
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

    /**
     * Returns a pattern for decimal types.
     *
     * @param allowedInputPattern The allowed input pattern
     * @param minLengthPatternString The minimum length pattern string
     * @param maxLengthPatternString The maximum length pattern string
     * @param isRequired Whether the parameter is required
     * @return A regex pattern string for decimal types
     */
    public String getDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean isRequired) {
        // Always return unanchored pattern; anchoring is handled in getParamPattern
        // SECURITY FIX: Use bounded quantifiers to prevent ReDoS attacks
        if (isRequired) {
            if (!maxLengthPatternString.isEmpty()) {
                return "-?([0-9]{" + minLengthPatternString + "," + maxLengthPatternString + "}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})";
            } else {
                // Apply reasonable upper bound of 15 digits to prevent ReDoS
                String maxBound = "15";
                return "-?([0-9]{" + minLengthPatternString + "," + maxBound + "}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})";
            }
        } else {
            // Optional decimal pattern with bounded quantifiers
            return "-?([0-9]{0,15}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})?";
        }
    }

    /**
     * Returns a pattern for non-decimal types.
     *
     * @param allowedInputPattern The allowed input pattern
     * @param minLengthPatternString The minimum length pattern string
     * @param maxLengthPatternString The maximum length pattern string
     * @param allowMultiple Whether multiple values are allowed
     * @param isRequired Whether the parameter is required
     * @return A regex pattern string for non-decimal types
     */
    public String getNonDecimalPattern(String allowedInputPattern, String minLengthPatternString, String maxLengthPatternString, boolean allowMultiple, boolean isRequired) {
        // Match test expectations exactly
        if (allowMultiple) {
            String min = (minLengthPatternString.isEmpty() ? (isRequired ? "1" : "0") : minLengthPatternString);
            String max = maxLengthPatternString;
            if (!min.isEmpty() && !max.isEmpty() && min.equals(max)) {
                return ".{" + min + "}";
            } else if (!min.isEmpty() && !max.isEmpty()) {
                return ".{" + min + "," + max + "}";
            } else if (!min.isEmpty() && max.isEmpty()) {
                if (min.equals("1")) {
                    return ".+";
                } else if (min.equals("0")) {
                    return ".*";
                } else {
                    return ".{" + min + ",}";
                }
            } else if (min.equals("0") && max.isEmpty()) {
                return ".*";
            } else {
                return isRequired ? ".+" : ".*";
            }
        } else {
            // For enums, booleans, etc., just return the pattern, with optionality
            if (isRequired) {
                return allowedInputPattern;
            } else {
                return allowedInputPattern + "?";
            }
        }
    }

    // Helper methods for OpenAPI 3.1 data type format detection

    /**
     * Check if parameter has time format.
     * @param param The parameter to check
     * @return true if parameter format is time
     */
    private boolean isTimeFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("time")) ||
               (param.vendorExtensions != null && "time".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has URI format.
     * @param param The parameter to check
     * @return true if parameter format is uri
     */
    private boolean isUriFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("uri")) ||
               (param.vendorExtensions != null && "uri".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has URI reference format.
     * @param param The parameter to check
     * @return true if parameter format is uri-reference
     */
    private boolean isUriReferenceFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("uri-reference")) ||
               (param.vendorExtensions != null && "uri-reference".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has URI template format.
     * @param param The parameter to check
     * @return true if parameter format is uri-template
     */
    private boolean isUriTemplateFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("uri-template")) ||
               (param.vendorExtensions != null && "uri-template".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has hostname format.
     * @param param The parameter to check
     * @return true if parameter format is hostname
     */
    private boolean isHostnameFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("hostname")) ||
               (param.vendorExtensions != null && "hostname".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has IPv4 format.
     * @param param The parameter to check
     * @return true if parameter format is ipv4
     */
    private boolean isIpv4Format(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("ipv4")) ||
               (param.vendorExtensions != null && "ipv4".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has IPv6 format.
     * @param param The parameter to check
     * @return true if parameter format is ipv6
     */
    private boolean isIpv6Format(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("ipv6")) ||
               (param.vendorExtensions != null && "ipv6".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has JSON pointer format.
     * @param param The parameter to check
     * @return true if parameter format is json-pointer
     */
    private boolean isJsonPointerFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("json-pointer")) ||
               (param.vendorExtensions != null && "json-pointer".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has relative JSON pointer format.
     * @param param The parameter to check
     * @return true if parameter format is relative-json-pointer
     */
    private boolean isRelativeJsonPointerFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("relative-json-pointer")) ||
               (param.vendorExtensions != null && "relative-json-pointer".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has byte format.
     * @param param The parameter to check
     * @return true if parameter format is byte
     */
    private boolean isByteFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("byte")) ||
               (param.vendorExtensions != null && "byte".equals(param.vendorExtensions.get("x-format")));
    }

    /**
     * Check if parameter has binary format.
     * @param param The parameter to check
     * @return true if parameter format is binary
     */
    private boolean isBinaryFormat(CodegenParameter param) {
        return (param.getFormat() != null && param.getFormat().equals("binary")) ||
               (param.vendorExtensions != null && "binary".equals(param.vendorExtensions.get("x-format")));
    }
}
