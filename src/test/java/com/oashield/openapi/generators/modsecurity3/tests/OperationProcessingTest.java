package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for operation processing functionality
 */
public class OperationProcessingTest {

    @Test
    public void testPostProcessOperationsWithModels_basic() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        OperationsMap operationsMap = mock(OperationsMap.class);
        OperationMap operationMap = mock(OperationMap.class);
        List<CodegenOperation> operationList = new ArrayList<>();
        CodegenOperation operation = new CodegenOperation();
        operation.path = "/test/path";
        operation.operationId = "testOperation";
        operation.vendorExtensions = new HashMap<>();
        operation.allParams = new ArrayList<>(); // Initialize allParams list
        operationList.add(operation);

        when(operationsMap.getOperations()).thenReturn(operationMap);
        when(operationMap.getOperation()).thenReturn(operationList);

        // Capture initial globalIndex
        Long initialGlobalIndex = generator.globalIndex;

        OperationsMap processedOperationsMap = generator.postProcessOperationsWithModels(operationsMap, new ArrayList<>());

        // Verify vendor extensions are added to the operation
        assertTrue(operation.vendorExtensions.containsKey("x-codegen-pathRegex"));
        assertEquals("/test/path".replaceAll("\\{.*?\\}", "[^/]+"), operation.vendorExtensions.get("x-codegen-pathRegex"));
        for (int i = 1; i <= 20; i++) {
            assertTrue(operation.vendorExtensions.containsKey("x-codegen-globalIndex_" + i));
            assertEquals(initialGlobalIndex + i - 1, operation.vendorExtensions.get("x-codegen-globalIndex_" + i));
        }

        // Verify vendor extension is added to the results map
        verify(processedOperationsMap).put(eq("vendorExtensions"), any(Map.class));
    }

    @Test
    public void testPostProcessOperationsWithModels_consumes() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        OperationsMap operationsMap = mock(OperationsMap.class);
        OperationMap operationMap = mock(OperationMap.class);
        List<CodegenOperation> operationList = new ArrayList<>();

        // Operation with JSON consume
        CodegenOperation jsonOperation = new CodegenOperation();
        jsonOperation.path = "/test/json";
        jsonOperation.operationId = "jsonOperation";
        jsonOperation.vendorExtensions = new HashMap<>();
        jsonOperation.allParams = new ArrayList<>();
        jsonOperation.hasConsumes = true;
        List<Map<String, String>> jsonConsumes = new ArrayList<>();
        Map<String, String> jsonConsume = new HashMap<>();
        jsonConsume.put("mediaType", "application/json");
        jsonConsume.put("isJson", "true");
        jsonConsumes.add(jsonConsume);
        jsonOperation.consumes = jsonConsumes;
        operationList.add(jsonOperation);

        // Operation with XML consume
        CodegenOperation xmlOperation = new CodegenOperation();
        xmlOperation.path = "/test/xml";
        xmlOperation.operationId = "xmlOperation";
        xmlOperation.vendorExtensions = new HashMap<>();
        xmlOperation.allParams = new ArrayList<>();
        xmlOperation.hasConsumes = true;
        List<Map<String, String>> xmlConsumes = new ArrayList<>();
        Map<String, String> xmlConsume = new HashMap<>();
        xmlConsume.put("mediaType", "application/xml");
        xmlConsume.put("isXml", "true");
        xmlConsumes.add(xmlConsume);
        xmlOperation.consumes = xmlConsumes;
        operationList.add(xmlOperation);

        // Operation with no consumes
        CodegenOperation noConsumesOperation = new CodegenOperation();
        noConsumesOperation.path = "/test/noconsumes";
        noConsumesOperation.operationId = "noConsumesOperation";
        noConsumesOperation.vendorExtensions = new HashMap<>();
        noConsumesOperation.allParams = new ArrayList<>();
        noConsumesOperation.hasConsumes = false;
        operationList.add(noConsumesOperation);

        when(operationsMap.getOperations()).thenReturn(operationMap);
        when(operationMap.getOperation()).thenReturn(operationList);

        generator.postProcessOperationsWithModels(operationsMap, new ArrayList<>());

        // Verify JSON and XML vendor extensions
        assertTrue((Boolean) jsonOperation.vendorExtensions.get("x-codegen-isJson"));
        assertFalse((Boolean) jsonOperation.vendorExtensions.get("x-codegen-isXml"));
        assertFalse((Boolean) xmlOperation.vendorExtensions.get("x-codegen-isJson"));
        assertTrue((Boolean) xmlOperation.vendorExtensions.get("x-codegen-isXml"));
        assertFalse((Boolean) noConsumesOperation.vendorExtensions.get("x-codegen-isJson"));
        assertFalse((Boolean) noConsumesOperation.vendorExtensions.get("x-codegen-isXml"));
    }

    @Test
    public void testPostProcessOperationsWithModels_requiredArrayParameter() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        OperationsMap operationsMap = mock(OperationsMap.class);
        OperationMap operationMap = mock(OperationMap.class);
        List<CodegenOperation> operationList = new ArrayList<>();

        CodegenOperation operation = new CodegenOperation();
        operation.path = "/test/array";
        operation.operationId = "arrayOperation";
        operation.vendorExtensions = new HashMap<>();
        operation.allParams = new ArrayList<>();

        // Required array parameter without minItems
        CodegenParameter requiredArrayParam = new CodegenParameter();
        requiredArrayParam.baseName = "requiredArray";
        requiredArrayParam.required = true;
        requiredArrayParam.isArray = true;
        operation.allParams.add(requiredArrayParam);

        // Required array parameter with minItems = 0
        CodegenParameter requiredArrayParamWithMinZero = new CodegenParameter();
        requiredArrayParamWithMinZero.baseName = "requiredArrayMinZero";
        requiredArrayParamWithMinZero.required = true;
        requiredArrayParamWithMinZero.isArray = true;
        requiredArrayParamWithMinZero.setMinItems(0);
        operation.allParams.add(requiredArrayParamWithMinZero);

        // Optional array parameter without minItems
        CodegenParameter optionalArrayParam = new CodegenParameter();
        optionalArrayParam.baseName = "optionalArray";
        optionalArrayParam.required = false;
        optionalArrayParam.isArray = true;
        operation.allParams.add(optionalArrayParam);

        operationList.add(operation);

        when(operationsMap.getOperations()).thenReturn(operationMap);
        when(operationMap.getOperation()).thenReturn(operationList);

        OperationsMap processedOperationsMap = generator.postProcessOperationsWithModels(operationsMap, new ArrayList<>());

        // Verify minItems is set to 1 for required arrays without minItems or with minItems = 0
        assertEquals(1, requiredArrayParam.getMinItems());
        assertEquals(1, requiredArrayParamWithMinZero.getMinItems());
        assertNull(optionalArrayParam.getMinItems());
    }

    @Test
    public void launchCodeGenerator() {
        // Simple test to check if the code generator runs without exceptions
        // This is a convenience test for running the generator in a debugger
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir("output/modsecurity3");

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();

        // No assertions - we're just making sure it doesn't throw exceptions
    }
}
