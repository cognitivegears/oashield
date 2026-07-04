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
 * Media-type handling (Phase 2): form/multipart/other/wildcard consumes must not
 * be unconditionally blocked, and optional request bodies must allow bodiless
 * requests.
 */
public class MediaTypeHandlingTest {

    @TempDir
    static Path outputDir;
    private static String conf;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/multipart.yaml")
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

    private static String section(String operationId) {
        int start = conf.indexOf("# " + operationId + ":");
        int end = conf.indexOf("SecMarker END_" + operationId);
        assertTrue(start >= 0 && end > start, "operation section not found: " + operationId + "\n" + conf);
        return conf.substring(start, end);
    }

    @Test
    void classifyMediaTypeCoversAllClasses() {
        assertEquals("isJson", Modsecurity3Generator.classifyMediaType("application/json"));
        assertEquals("isJson", Modsecurity3Generator.classifyMediaType("application/vnd.api+json"));
        assertEquals("isXml", Modsecurity3Generator.classifyMediaType("text/xml"));
        assertEquals("isXml", Modsecurity3Generator.classifyMediaType("application/soap+xml"));
        assertEquals("isFormLike", Modsecurity3Generator.classifyMediaType("application/x-www-form-urlencoded"));
        assertEquals("isFormLike", Modsecurity3Generator.classifyMediaType("multipart/form-data"));
        assertEquals("isWildcardAll", Modsecurity3Generator.classifyMediaType("*/*"));
        assertEquals("isOtherMedia", Modsecurity3Generator.classifyMediaType("application/octet-stream"));
        assertEquals("isOtherMedia", Modsecurity3Generator.classifyMediaType("text/plain"));
    }

    @Test
    void formOperationPassesToAfterConsumes() {
        String op = section("updateProfile");
        assertTrue(op.contains("skipAfter:AFTER_CONSUMES_updateProfile"),
                "form operation must be able to reach AFTER_CONSUMES:\n" + op);
        assertTrue(op.contains("^application/x-www-form-urlencoded"),
                "form operation should gate on its media type:\n" + op);
    }

    @Test
    void multipartOperationPassesToAfterConsumes() {
        String op = section("uploadAvatar");
        assertTrue(op.contains("^multipart/form-data"), "multipart gate missing:\n" + op);
        assertTrue(op.contains("skipAfter:AFTER_CONSUMES_uploadAvatar"),
                "multipart operation must be able to reach AFTER_CONSUMES:\n" + op);
    }

    @Test
    void octetStreamOperationPassesByDefaultPolicy() {
        String op = section("uploadBlob");
        assertTrue(op.contains("^application/octet-stream"), "octet-stream gate missing:\n" + op);
        assertTrue(op.contains("skipAfter:AFTER_CONSUMES_uploadBlob"),
                "octet-stream should pass under the default policy:\n" + op);
        assertFalse(op.contains("Uninspectable media type blocked"),
                "default policy must not emit the block action:\n" + op);
    }

    @Test
    void wildcardConsumesSkipsContentTypeGate() {
        String op = section("postAnything");
        assertTrue(op.contains("skipAfter:AFTER_CONSUMES_postAnything"),
                "wildcard operation must pass:\n" + op);
        assertFalse(op.contains("[^/\\s]+/[^/\\s]+"),
                "*/* must not emit a content-type gate at all:\n" + op);
        assertFalse(op.contains("@rx ^\\*"), "escaped literal * gate must be gone:\n" + op);
    }

    @Test
    void optionalBodySkipsBodyChecksWhenNoContentType() {
        String op = section("postNote");
        assertTrue(op.contains("SecRule &REQUEST_HEADERS:Content-Type \"@eq 0\""),
                "optional body should short-circuit on missing Content-Type:\n" + op);
        // required-property rule still present for requests that DO send a body
        assertTrue(op.contains("Missing required property json.text"),
                "required property rule should remain for present bodies:\n" + op);
    }

    @Test
    void requiredBodyDoesNotGetTheBodilessSkip() {
        String op = section("updateProfile");
        assertFalse(op.contains("SecRule &REQUEST_HEADERS:Content-Type \"@eq 0\""),
                "required body must not skip body checks:\n" + op);
    }
}
