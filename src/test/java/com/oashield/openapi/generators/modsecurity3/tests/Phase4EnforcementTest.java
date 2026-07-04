package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;

/**
 * New enforcement (Phase 4): header/cookie parameter validation, required
 * parameter presence, power-of-10 multipleOf, and body array element counts.
 */
public class Phase4EnforcementTest {

    @TempDir
    static Path outputDir;
    private static String conf;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/paramfeatures.yaml")
                        .setOutputDir(outputDir.toString())
                        .toClientOptInput())
                .generate();
        try (Stream<Path> files = Files.list(outputDir)) {
            conf = files.filter(p -> p.getFileName().toString().endsWith("Api.conf"))
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
    }

    @Test
    void requiredQueryParamGetsPresenceRule() {
        assertTrue(conf.contains("SecRule &ARGS_GET:q \"@eq 0\""),
                "required query parameter needs a presence rule:\n" + conf);
    }

    @Test
    void headerParamsAreValidated() {
        assertTrue(conf.contains("SecRule REQUEST_HEADERS:X-Request-Id \"!@rx"),
                "header parameter value rule missing:\n" + conf);
        assertTrue(conf.contains("SecRule &REQUEST_HEADERS:X-Request-Id \"@eq 0\""),
                "required header presence rule missing:\n" + conf);
        assertTrue(conf.contains("SecRule REQUEST_HEADERS:X-Trace \"!@rx ^[a-f0-9]{8}$\""),
                "optional header spec pattern rule missing:\n" + conf);
        assertFalse(conf.contains("&REQUEST_HEADERS:X-Trace"),
                "optional header must not get a presence rule:\n" + conf);
    }

    @Test
    void cookieParamsAreValidated() {
        assertTrue(conf.contains("SecRule REQUEST_COOKIES:session \"!@rx ^[A-Za-z0-9]{10,64}$\""),
                "cookie parameter value rule missing:\n" + conf);
        assertTrue(conf.contains("SecRule &REQUEST_COOKIES:session \"@eq 0\""),
                "required cookie presence rule missing:\n" + conf);
    }

    @Test
    void headerAndCookieNamesAreNotInArgsAllowlist() {
        // undeclared headers/cookies must pass; only query/form/body names are allowlisted
        assertTrue(conf.contains("ARGS_NAMES \"!@rx ^(?:q)$\""),
                "GET allowlist should contain only the query param:\n" + conf);
    }

    @Test
    void powerOfTenMultipleOfBecomesTrailingZerosPattern() {
        assertTrue(conf.contains("ARGS:json.price \"!@rx ^(?:0|[0-9]{1,17}0{2})$\""),
                "multipleOf: 100 should produce a trailing-zeros pattern:\n" + conf);
    }

    @Test
    void bodyArrayCountsAreEnforced() {
        assertTrue(conf.contains("SecRule &ARGS:/^json\\.labels\\.(?:array_)?\\d{1,9}$/ \"@lt 1\""),
                "minItems count rule missing:\n" + conf);
        assertTrue(conf.contains("SecRule &ARGS:/^json\\.labels\\.(?:array_)?\\d{1,9}$/ \"@gt 3\""),
                "maxItems count rule missing:\n" + conf);
    }

    @Test
    void powerOfTenZerosHelper() {
        assertEquals(1, Modsecurity3Generator.powerOfTenZeros(10));
        assertEquals(2, Modsecurity3Generator.powerOfTenZeros(100));
        assertEquals(3, Modsecurity3Generator.powerOfTenZeros(1000L));
        assertEquals(-1, Modsecurity3Generator.powerOfTenZeros(null));
        assertEquals(-1, Modsecurity3Generator.powerOfTenZeros(5));
        assertEquals(-1, Modsecurity3Generator.powerOfTenZeros(250));
        assertEquals(-1, Modsecurity3Generator.powerOfTenZeros(0.1));
        assertEquals(-1, Modsecurity3Generator.powerOfTenZeros(1));
    }
}
