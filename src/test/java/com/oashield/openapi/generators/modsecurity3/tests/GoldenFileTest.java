package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/**
 * Golden-file snapshot tests: the generated rule and schema text for every sample
 * spec and engine flavor is compared against checked-in golden files, so any change
 * to generated output shows up as a reviewable diff.
 *
 * To regenerate after an intentional change: mvn test -Dtest=GoldenFileTest -DupdateGoldenFiles=true
 */
public class GoldenFileTest {

    private static final Path GOLDEN_ROOT = Paths.get("src/test/resources/golden");
    private static final List<String> SAMPLES = List.of(
            "petstore", "composed", "getparam", "urlintparam", "multipart", "paramfeatures", "xmlbody", "oas31");
    private static final List<String> FLAVORS = List.of("modsecurity3", "coraza");

    static Stream<Arguments> cases() {
        return SAMPLES.stream().flatMap(sample -> FLAVORS.stream().map(flavor -> Arguments.of(sample, flavor)));
    }

    @ParameterizedTest(name = "{0}/{1}")
    @MethodSource("cases")
    void generatedOutputMatchesGolden(String sample, String flavor, @TempDir Path outputDir) throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/" + sample + ".yaml")
                        .setOutputDir(outputDir.toString())
                        .addAdditionalProperty("engineFlavor", flavor)
                        .toClientOptInput())
                .generate();

        List<Path> generated;
        try (Stream<Path> files = Files.list(outputDir)) {
            generated = files
                    .filter(p -> p.getFileName().toString().endsWith(".conf")
                            || p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertFalse(generated.isEmpty(), "generator produced no .conf/.json files");

        Path goldenDir = GOLDEN_ROOT.resolve(sample).resolve(flavor);
        if (Boolean.getBoolean("updateGoldenFiles")) {
            updateGoldenFiles(goldenDir, generated);
            return;
        }

        assertTrue(Files.isDirectory(goldenDir),
                "Missing golden directory " + goldenDir + "; create it with -DupdateGoldenFiles=true");
        List<String> goldenNames;
        try (Stream<Path> files = Files.list(goldenDir)) {
            goldenNames = files.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
        List<String> generatedNames = generated.stream()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        assertEquals(goldenNames, generatedNames,
                "generated file set differs from golden for " + sample + "/" + flavor);

        for (Path p : generated) {
            String actual = normalize(Files.readString(p));
            String expected = normalize(Files.readString(goldenDir.resolve(p.getFileName().toString())));
            assertEquals(expected, actual, "golden mismatch: " + sample + "/" + flavor + "/" + p.getFileName());
        }
    }

    private static void updateGoldenFiles(Path goldenDir, List<Path> generated) throws IOException {
        Files.createDirectories(goldenDir);
        try (Stream<Path> old = Files.list(goldenDir)) {
            for (Path p : old.collect(Collectors.toList())) {
                Files.delete(p);
            }
        }
        for (Path p : generated) {
            Files.copy(p, goldenDir.resolve(p.getFileName().toString()));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
