package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;

/**
 * Tests for the deny/logging configuration options (issue #16):
 * denyAction, denyStatus, denyRedirectUrl, enableLogging, includeEngineConfig.
 */
public class DenyConfigTest {

    @TempDir
    Path tempDir;

    private void generate(Map<String, Object> additionalProperties) {
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir(tempDir.toString());
        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
            configurator.addAdditionalProperty(entry.getKey(), entry.getValue());
        }
        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
    }

    private String mainConfig() throws IOException {
        return Files.readString(tempDir.resolve("mainconfig.conf"));
    }

    private String apiConfig() throws IOException {
        return Files.readString(tempDir.resolve("PetApi.conf"));
    }

    @Test
    public void defaultsEmitDeny403WithLogging() throws IOException {
        generate(new HashMap<>());

        String main = mainConfig();
        assertTrue(main.contains("SecRuleEngine On"), "engine config emitted by default");
        assertTrue(main.contains("SecDefaultAction \"phase:2,log,auditlog,deny,status:403\""),
                "default deny action is deny with status 403");
        assertTrue(apiConfig().contains("log,auditlog"), "rules log by default");
    }

    @Test
    public void denyStatusAndActionAreConfigurable() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("denyStatus", "429");
        generate(props);
        assertTrue(mainConfig().contains("SecDefaultAction \"phase:2,log,auditlog,deny,status:429\""));

        props.clear();
        props.put("denyAction", "drop");
        generate(props);
        assertTrue(mainConfig().contains("SecDefaultAction \"phase:2,log,auditlog,drop\""),
                "drop carries no status");

        props.clear();
        props.put("denyAction", "pass");
        generate(props);
        assertTrue(mainConfig().contains("SecDefaultAction \"phase:2,log,auditlog,pass\""),
                "pass = detection-only");
    }

    @Test
    public void redirectEmitsUrlAndStatus() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("denyAction", "redirect");
        props.put("denyRedirectUrl", "https://example.com/blocked");
        props.put("denyStatus", "302");
        generate(props);
        assertTrue(mainConfig().contains(
                "SecDefaultAction \"phase:2,log,auditlog,redirect:'https://example.com/blocked',status:302\""));
    }

    @Test
    public void enableLoggingFalseEmitsNolog() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("enableLogging", "false");
        generate(props);

        assertTrue(mainConfig().contains("SecDefaultAction \"phase:2,nolog,deny,status:403\""));
        assertFalse(apiConfig().contains("log,auditlog"), "no rule logs when logging is disabled");
    }

    @Test
    public void includeEngineConfigFalseOmitsEngineDirectives() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("includeEngineConfig", "false");
        generate(props);

        String main = mainConfig();
        assertFalse(main.contains("SecRuleEngine"), "no SecRuleEngine when includeEngineConfig=false");
        assertFalse(main.contains("SecDefaultAction"), "no SecDefaultAction when includeEngineConfig=false");
        assertTrue(main.contains("Include "), "operation includes still emitted");
        assertTrue(main.contains("SecMarker FAILED_API_CHECKS"), "catch-all rules still emitted");
    }

    @Test
    public void invalidValuesFail() {
        assertThrows(IllegalArgumentException.class, () -> processOptsWith("denyAction", "teapot"));
        assertThrows(IllegalArgumentException.class, () -> processOptsWith("denyStatus", "999"));
        assertThrows(IllegalArgumentException.class, () -> processOptsWith("denyStatus", "abc"));
        assertThrows(IllegalArgumentException.class, () -> processOptsWith("denyRedirectUrl", "javascript:alert(1)"));
        // redirect without a URL
        assertThrows(IllegalArgumentException.class, () -> processOptsWith("denyAction", "redirect"));
    }

    private void processOptsWith(String key, String value) {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        generator.additionalProperties().put(key, value);
        generator.processOpts();
    }
}
