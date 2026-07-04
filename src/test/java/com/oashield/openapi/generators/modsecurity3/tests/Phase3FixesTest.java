package com.oashield.openapi.generators.modsecurity3.tests;

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

/**
 * False-positive fixes: additionalProperties/maps, nullable, serialization
 * styles, security-scheme parameters, readOnly required properties, and server
 * base paths.
 */
public class Phase3FixesTest {

    @TempDir
    static Path outputDir;
    private static String conf;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("src/test/resources/specs/phase3-features.yaml")
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
    void serverBasePathIsPrefixedWithVariableAsSegment() {
        assertTrue(conf.contains("\"!@rx ^/base/[^/]+/things"),
                "servers.url path (with {version} variable) should prefix path regexes:\n" + conf);
    }

    @Test
    void apiKeyQueryParameterIsAllowlisted() {
        assertTrue(conf.matches("(?s).*ARGS_NAMES \"!@rx \\^\\(\\?:[^\"]*api_key[^\"]*\\)\\$\".*"),
                "api_key security parameter should be in the ARGS_NAMES allowlist:\n" + conf);
    }

    @Test
    void csvArrayValidatesJoinedForm() {
        assertTrue(conf.contains("ARGS_GET:tags \"!@rx ^(?:[a-z]{1,10})(?:,(?:[a-z]{1,10})){1,4}$\""),
                "explode=false array should validate the joined CSV form:\n" + conf);
        assertFalse(conf.contains("&ARGS_GET:tags \"@lt"),
                "joined arrays must not emit per-value count rules:\n" + conf);
    }

    @Test
    void pipeDelimitedArrayUsesPipeSeparator() {
        assertTrue(conf.contains("ARGS_GET:ids \"!@rx ^(?:[0-9]{1,19})(?:\\|(?:[0-9]{1,19})){0,999}$\""),
                "pipeDelimited array should join with | :\n" + conf);
    }

    @Test
    void deepObjectKeysAreAllowlisted() {
        assertTrue(conf.contains("filter\\[[^\\]]{1,64}\\]"),
                "deepObject bracket keys should be allowlisted:\n" + conf);
    }

    @Test
    void readOnlyRequiredPropertyHasNoPresenceRule() {
        assertFalse(conf.contains("Missing required property json.id"),
                "readOnly required properties may be omitted from requests:\n" + conf);
        assertTrue(conf.contains("Missing required property json.name"),
                "regular required properties keep their presence rule:\n" + conf);
    }

    @Test
    void nullablePropertyAcceptsEmptyValue() {
        assertTrue(conf.contains("ARGS:json.nickname \"!@rx ^(?:.+)?$\""),
                "nullable property pattern should accept the empty (null) form:\n" + conf);
    }

    @Test
    void mapPropertyAllowsArbitraryKeysAndValidatesValues() {
        assertTrue(conf.contains("json\\.attrs\\..{1,256}"),
                "map property should allowlist arbitrary sub-keys:\n" + conf);
        assertTrue(conf.contains("ARGS:/(?i)^json\\.attrs\\.[^.]{1,64}$/ \"!@rx ^[a-z]{1,20}$\""),
                "map values should be validated against the additionalProperties schema:\n" + conf);
    }

    @Test
    void freeFormObjectAllowsArbitraryKeysWithoutValueRule() {
        assertTrue(conf.contains("json\\.misc\\..{1,256}"),
                "free-form object should allowlist arbitrary sub-keys:\n" + conf);
        assertFalse(conf.contains("ARGS:json.misc \"!@rx"),
                "free-form object must not get a scalar value rule:\n" + conf);
    }
}
