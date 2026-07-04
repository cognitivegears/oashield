package com.oashield.openapi.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import com.oashield.openapi.integration.util.CorazaContainerManager;
import com.oashield.openapi.integration.util.ModSecurityContainerManager;
import com.oashield.openapi.integration.util.WafContainerManager;

/**
 * Exploratory probes of undocumented engine behavior. NOT part of the regular
 * suite: run manually with
 *
 *   DOCKER_JAVA_PROPERTIES="api.version=1.44" mvn test -Dtest=EngineBehaviorProbeTest -DrunEngineProbes=true
 *
 * Findings are printed with an ENGINE-PROBE| prefix and recorded in
 * docs/engine-behavior.md. Probes drive design decisions for nullable handling,
 * optional request bodies, multipart support, Coraza JSON Schema draft support,
 * and XML validation.
 */
@EnabledIfSystemProperty(named = "runEngineProbes", matches = "true")
public class EngineBehaviorProbeTest {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ---------------------------------------------------------------
    // Probe 1: how does a JSON null value flatten into ARGS?
    // Statuses: 456=key absent, 457=value empty string, 458=value literal "null",
    //           459=present with some other value
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = { "coraza", "modsecurity3" })
    void probeJsonNullFlattening(String engine) throws Exception {
        String conf = "SecRuleEngine On\n"
                + "SecRequestBodyAccess On\n"
                // health-check bypass: the container wait strategy expects GET / to answer 200/403
                + "SecRule REQUEST_FILENAME \"@streq /\" \"id:2,phase:1,pass,nolog,ctl:ruleEngine=Off\"\n"
                + "SecRule REQUEST_HEADERS:Content-Type \"@rx (?i)json\" \"id:1,phase:1,pass,nolog,ctl:requestBodyProcessor=JSON\"\n"
                + "SecRule REQBODY_ERROR \"!@eq 0\" \"id:5,phase:2,deny,status:422\"\n"
                + "SecRule &ARGS:json.a \"@eq 0\" \"id:10,phase:2,deny,status:456\"\n"
                + "SecRule ARGS:json.a \"@rx ^$\" \"id:11,phase:2,deny,status:457\"\n"
                + "SecRule ARGS:json.a \"@streq null\" \"id:12,phase:2,deny,status:458\"\n"
                + "SecAction \"id:13,phase:2,deny,status:459\"\n";
        withWaf(engine, Map.of("main.conf", conf), waf -> {
            report(engine, "null-flatten", "a=null", post(waf, "/x", "application/json", "{\"a\":null}"));
            report(engine, "null-flatten", "a=empty-string", post(waf, "/x", "application/json", "{\"a\":\"\"}"));
            report(engine, "null-flatten", "a=x(control)", post(waf, "/x", "application/json", "{\"a\":\"x\"}"));
            report(engine, "null-flatten", "a-absent(control)", post(waf, "/x", "application/json", "{}"));
        });
    }

    // ---------------------------------------------------------------
    // Probe 2: empty / absent body signals for optional-requestBody gating.
    // Statuses: 456=no Content-Type header, 457=REQBODY_ERROR set, 459=fallthrough
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = { "coraza", "modsecurity3" })
    void probeEmptyBody(String engine) throws Exception {
        String conf = "SecRuleEngine On\n"
                + "SecRequestBodyAccess On\n"
                // health-check bypass: the container wait strategy expects GET / to answer 200/403
                + "SecRule REQUEST_FILENAME \"@streq /\" \"id:2,phase:1,pass,nolog,ctl:ruleEngine=Off\"\n"
                + "SecRule REQUEST_HEADERS:Content-Type \"@rx (?i)json\" \"id:1,phase:1,pass,nolog,ctl:requestBodyProcessor=JSON\"\n"
                + "SecRule &REQUEST_HEADERS:Content-Type \"@eq 0\" \"id:20,phase:2,deny,status:456\"\n"
                + "SecRule REQBODY_ERROR \"!@eq 0\" \"id:21,phase:2,deny,status:457\"\n"
                + "SecAction \"id:23,phase:2,deny,status:459\"\n";
        withWaf(engine, Map.of("main.conf", conf), waf -> {
            report(engine, "empty-body", "no-CT-no-body", post(waf, "/x", null, null));
            report(engine, "empty-body", "json-CT-empty-body", post(waf, "/x", "application/json", ""));
            report(engine, "empty-body", "json-CT-valid-body(control)", post(waf, "/x", "application/json", "{\"a\":1}"));
        });
    }

    // ---------------------------------------------------------------
    // Probe 3: multipart/form-data — text parts in ARGS_POST? file parts in FILES?
    // Statuses: 456=text part visible in ARGS_POST, 457=file part visible in FILES_NAMES,
    //           458=REQBODY_ERROR, 459=nothing matched
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = { "coraza", "modsecurity3" })
    void probeMultipart(String engine) throws Exception {
        String conf = "SecRuleEngine On\n"
                + "SecRequestBodyAccess On\n"
                // health-check bypass: the container wait strategy expects GET / to answer 200/403
                + "SecRule REQUEST_FILENAME \"@streq /\" \"id:2,phase:1,pass,nolog,ctl:ruleEngine=Off\"\n"
                + "SecRule REQBODY_ERROR \"!@eq 0\" \"id:31,phase:2,deny,status:458\"\n"
                + "SecRule &ARGS_POST:field1 \"@eq 1\" \"id:30,phase:2,deny,status:456\"\n"
                + "SecRule FILES_NAMES \"@rx .\" \"id:32,phase:2,deny,status:457\"\n"
                + "SecAction \"id:33,phase:2,deny,status:459\"\n";
        String boundary = "oashieldprobe";
        String textPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"field1\"\r\n\r\n"
                + "value1\r\n"
                + "--" + boundary + "--\r\n";
        String filePart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file1\"; filename=\"a.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "contents\r\n"
                + "--" + boundary + "--\r\n";
        withWaf(engine, Map.of("main.conf", conf), waf -> {
            report(engine, "multipart", "text-part",
                    post(waf, "/x", "multipart/form-data; boundary=" + boundary, textPart));
            report(engine, "multipart", "file-part",
                    post(waf, "/x", "multipart/form-data; boundary=" + boundary, filePart));
            report(engine, "multipart", "urlencoded(control)",
                    post(waf, "/x", "application/x-www-form-urlencoded", "field1=value1"));
        });
    }

    // ---------------------------------------------------------------
    // Probe 4: which JSON Schema keywords does Coraza's @validateSchema enforce,
    // under which $schema draft? 456=schema violation detected, 459=passed through.
    // A startup failure (exception) means the schema file failed to load.
    // ---------------------------------------------------------------
    @Test
    void probeCorazaSchemaDrafts() throws Exception {
        Map<String, String[]> cases = new LinkedHashMap<>();
        // name -> {schema, violating body}
        cases.put("d7-const", new String[] {
                "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"a\":{\"const\":5}}}",
                "{\"a\":6}" });
        cases.put("d7-dependentRequired", new String[] {
                "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"dependentRequired\":{\"a\":[\"b\"]}}",
                "{\"a\":1}" });
        cases.put("d7-dependencies", new String[] {
                "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"dependencies\":{\"a\":[\"b\"]}}",
                "{\"a\":1}" });
        cases.put("d7-ifthenelse", new String[] {
                "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"if\":{\"properties\":{\"a\":{\"const\":1}}},\"then\":{\"required\":[\"b\"]}}",
                "{\"a\":1}" });
        cases.put("2020-prefixItems", new String[] {
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{\"arr\":{\"type\":\"array\",\"prefixItems\":[{\"type\":\"integer\"}]}}}",
                "{\"arr\":[\"x\"]}" });
        cases.put("2020-const", new String[] {
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{\"a\":{\"const\":5}}}",
                "{\"a\":6}" });
        cases.put("noschema-const", new String[] {
                "{\"type\":\"object\",\"properties\":{\"a\":{\"const\":5}}}",
                "{\"a\":6}" });

        String conf = "SecRuleEngine On\n"
                + "SecRequestBodyAccess On\n"
                // health-check bypass: the container wait strategy expects GET / to answer 200/403
                + "SecRule REQUEST_FILENAME \"@streq /\" \"id:2,phase:1,pass,nolog,ctl:ruleEngine=Off\"\n"
                + "SecRule REQUEST_HEADERS:Content-Type \"@rx (?i)json\" \"id:1,phase:1,pass,nolog,ctl:requestBodyProcessor=JSON\"\n"
                + "SecRule REQUEST_BODY \"@validateSchema rules/probe-schema.json\" \"id:50,phase:2,deny,status:456\"\n"
                + "SecAction \"id:51,phase:2,deny,status:459\"\n";
        for (Map.Entry<String, String[]> e : cases.entrySet()) {
            try {
                withWaf("coraza", Map.of("main.conf", conf, "probe-schema.json", e.getValue()[0]), waf -> {
                    report("coraza", "schema-draft", e.getKey() + "/violating",
                            post(waf, "/x", "application/json", e.getValue()[1]));
                    report("coraza", "schema-draft", e.getKey() + "/conforming",
                            post(waf, "/x", "application/json", "{\"a\":5,\"b\":2,\"arr\":[1]}"));
                });
            } catch (Exception ex) {
                System.out.println("ENGINE-PROBE|coraza|schema-draft|" + e.getKey() + "|LOAD-ERROR: "
                        + ex.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Probe 5: XML support. Does the XML body processor flatten into ARGS / set
    // REQBODY_ERROR, and does @validateSchema accept an XSD?
    // Statuses: 456=xsd violation detected, 457=REQBODY_ERROR, 459=fallthrough
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = { "coraza", "modsecurity3" })
    void probeXmlValidation(String engine) throws Exception {
        String xsdPath = engine.equals("coraza")
                ? "rules/probe.xsd"
                : "/etc/modsecurity.d/oashield/probe.xsd";
        String conf = "SecRuleEngine On\n"
                + "SecRequestBodyAccess On\n"
                // health-check bypass: the container wait strategy expects GET / to answer 200/403
                + "SecRule REQUEST_FILENAME \"@streq /\" \"id:2,phase:1,pass,nolog,ctl:ruleEngine=Off\"\n"
                + "SecRule REQUEST_HEADERS:Content-Type \"@rx (?i)xml\" \"id:1,phase:1,pass,nolog,ctl:requestBodyProcessor=XML\"\n"
                + "SecRule REQBODY_ERROR \"!@eq 0\" \"id:61,phase:2,deny,status:457\"\n"
                + "SecRule XML \"@validateSchema " + xsdPath + "\" \"id:60,phase:2,deny,status:456\"\n"
                + "SecAction \"id:62,phase:2,deny,status:459\"\n";
        String xsd = "<?xml version=\"1.0\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "<xs:element name=\"pet\">\n"
                + "<xs:complexType>\n"
                + "<xs:sequence>\n"
                + "<xs:element name=\"id\" type=\"xs:integer\"/>\n"
                + "</xs:sequence>\n"
                + "</xs:complexType>\n"
                + "</xs:element>\n"
                + "</xs:schema>\n";
        try {
            withWaf(engine, Map.of("main.conf", conf, "probe.xsd", xsd), waf -> {
                report(engine, "xml", "valid-xml",
                        post(waf, "/x", "application/xml", "<pet><id>1</id></pet>"));
                report(engine, "xml", "xsd-violating-xml",
                        post(waf, "/x", "application/xml", "<pet><id>notanumber</id></pet>"));
                report(engine, "xml", "malformed-xml",
                        post(waf, "/x", "application/xml", "<pet><id>1</id>"));
            });
        } catch (Exception ex) {
            System.out.println("ENGINE-PROBE|" + engine + "|xml|LOAD-ERROR: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // harness
    // ------------------------------------------------------------------

    private interface ProbeBody {
        void run(String baseUrl) throws Exception;
    }

    private void withWaf(String engine, Map<String, String> files, ProbeBody body) throws Exception {
        Path rulesDir = Files.createTempDirectory("oashield-probe");
        for (Map.Entry<String, String> f : files.entrySet()) {
            Files.writeString(rulesDir.resolve(f.getKey()), f.getValue());
        }
        WafContainerManager mgr = "coraza".equals(engine)
                ? new CorazaContainerManager(rulesDir.toAbsolutePath().toString())
                : new ModSecurityContainerManager(rulesDir.toAbsolutePath().toString());
        try {
            String baseUrl = mgr.start();
            body.run(baseUrl);
        } finally {
            mgr.stop();
        }
    }

    private static int post(String baseUrl, String path, String contentType, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .POST(body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static void report(String engine, String probe, String testCase, int status) {
        System.out.println("ENGINE-PROBE|" + engine + "|" + probe + "|" + testCase + "|status=" + status);
    }
}
