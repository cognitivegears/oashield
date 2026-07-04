package com.oashield.openapi.integration.actions;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HttpRequestAction {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestAction.class);

    /**
     * Execute a GET request.
     *
     * @param url the URL to send the GET request to
     * @return the Response object
     */
    public static Response executeGetRequest(String url) {
        logger.info("Executing GET request to URL: {}", url);
        return RestAssured.get(url);
    }

    /**
     * Execute a POST request with JSON body.
     *
     * @param url the URL to send the POST request to
     * @param requestBody the JSON body as String
     * @return the Response object
     */
    public static Response executePostRequest(String url, String requestBody) {
        logger.info("Executing POST request to URL: {} with body: {}", url, requestBody);
        return RestAssured.given()
                .contentType("application/json")
                .body(requestBody)
                .post(url);
    }

    /**
     * Execute a POST request with an explicit content type (form, multipart,
     * binary, ...) and return the status code. Uses java.net.http directly:
     * RestAssured's encoder registry re-encodes or rejects raw form/multipart
     * string bodies, but the WAF must see the body verbatim.
     *
     * @param url the URL to send the POST request to
     * @param contentType the Content-Type header value, or null to omit the header
     * @param requestBody the raw body as String, or null for no body
     * @return the HTTP status code
     */
    public static int executeRawPostRequest(String url, String contentType, String requestBody) {
        logger.info("Executing raw POST request to URL: {} with content type: {}", url, contentType);
        try {
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(requestBody == null
                            ? java.net.http.HttpRequest.BodyPublishers.noBody()
                            : java.net.http.HttpRequest.BodyPublishers.ofString(requestBody,
                                    java.nio.charset.StandardCharsets.UTF_8));
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }
            return java.net.http.HttpClient.newHttpClient()
                    .send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())
                    .statusCode();
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Raw POST request to " + url + " failed", e);
        }
    }

    /**
     * Execute an HTTP request with specified method.
     *
     * @param method HTTP method name, e.g., "GET", "POST"
     * @param url the URL to send the request to
     * @return the Response object
     */
    public static Response executeMethodRequest(String method, String url) {
        logger.info("Executing {} request to URL: {}", method, url);
        return RestAssured.request(method, url);
    }

    /**
     * Execute a GET request with query parameters.
     *
     * @param url the URL to send the GET request to
     * @param params map of query parameters
     * @return the Response object
     */
    public static Response executeGetRequestWithParams(String url, Map<String, String> params) {
        logger.info("Executing GET request to URL: {} with params: {}", url, params);
        return RestAssured.given()
                .queryParams(params)
                .get(url);
    }
}
