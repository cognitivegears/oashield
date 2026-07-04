package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/**
 * OpenAPI 3.1-specific behavior: numeric exclusiveMinimum/exclusiveMaximum must
 * produce @le/@ge comparisons (3.0 uses booleans alongside minimum/maximum; 3.1
 * puts the bound value directly in the exclusive keyword).
 */
public class Oas31FeaturesTest {

    private static String generate(String spec, Path outputDir) throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec(spec)
                        .setOutputDir(outputDir.toString())
                        .toClientOptInput())
                .generate();
        try (java.util.stream.Stream<Path> files = Files.list(outputDir)) {
            StringBuilder sb = new StringBuilder();
            for (Path p : files.filter(f -> f.getFileName().toString().endsWith("Api.conf"))
                    .collect(java.util.stream.Collectors.toList())) {
                sb.append(Files.readString(p));
            }
            return sb.toString();
        }
    }

    @Test
    void numericExclusiveBoundsProduceExclusiveComparisons(@TempDir Path outputDir) throws IOException {
        String conf = generate("src/test/resources/specs/oas31-exclusives.yaml", outputDir);
        assertTrue(conf.contains("ARGS_GET:count \"@le 0\""),
                "exclusiveMinimum: 0 should emit @le 0; conf was:\n" + conf);
        assertTrue(conf.contains("ARGS_GET:count \"@ge 100\""),
                "exclusiveMaximum: 100 should emit @ge 100; conf was:\n" + conf);
        assertTrue(conf.contains("ARGS_GET:size \"@lt 1\""),
                "inclusive minimum: 1 should emit @lt 1; conf was:\n" + conf);
        assertTrue(conf.contains("ARGS_GET:size \"@gt 50\""),
                "inclusive maximum: 50 should emit @gt 50; conf was:\n" + conf);
    }
}
