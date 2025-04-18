package com.camel.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

public class OdooService implements ERPService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(Exchange exchange) throws Exception {
        String username = exchange.getIn().getHeader("username", String.class);
        String password = exchange.getIn().getHeader("password", String.class);
        String url = exchange.getIn().getHeader("url", String.class);
        String db = exchange.getIn().getHeader("db", String.class);
        String model = exchange.getIn().getHeader("model", String.class);
        if (model == null || model.isBlank()) model = "res.partner";

        // Optional: kwargs as Map<String, Object> for field filtering
        Map<String, Object> kwargs = exchange.getIn().getHeader("kwargs", Map.class);
        Set<String> filterFields = (kwargs != null) ? kwargs.keySet() : Collections.emptySet();

        int uid = authenticate(url, db, username, password);
        List<Map<String, Object>> data = fetchFilteredOdooData(url, db, uid, password, model, filterFields);
        exchange.getMessage().setBody(objectMapper.writeValueAsString(data));
    }

    private int authenticate(String url, String db, String username, String password) throws Exception {
        String payload = String.format(
                "{\"jsonrpc\": \"2.0\", \"method\": \"call\", " +
                        "\"params\": {\"service\": \"common\", \"method\": \"login\", " +
                        "\"args\": [\"%s\", \"%s\", \"%s\"]}, \"id\": 1}",
                db, username, password);

        String response = postJson(url + "/jsonrpc", payload);
        JsonNode json = objectMapper.readTree(response);
        JsonNode resultNode = json.get("result");

        if (resultNode == null || !resultNode.isInt()) {
            throw new IllegalStateException("Invalid authentication response: " + response);
        }

        int uid = resultNode.asInt();
        System.out.println("✅ Authenticated with UID: " + uid);
        return uid;
    }

    private List<Map<String, Object>> fetchFilteredOdooData(String url, String db, int uid, String password,
                                                            String model, Set<String> desiredFields) throws Exception {

        // 1. Fetch all fields from the model using `fields_get`
        String fieldsGetPayload = String.format(
                "{\"jsonrpc\": \"2.0\", \"method\": \"call\", " +
                        "\"params\": {\"service\": \"object\", \"method\": \"execute_kw\", " +
                        "\"args\": [\"%s\", %d, \"%s\", \"%s\", \"fields_get\", [], {\"attributes\": [\"string\", \"type\"]}]}, \"id\": 2}",
                db, uid, password, model);

        String fieldsResponse = postJson(url + "/jsonrpc", fieldsGetPayload);
        JsonNode allFieldsJson = objectMapper.readTree(fieldsResponse).get("result");

        if (allFieldsJson == null || !allFieldsJson.isObject()) {
            throw new IllegalStateException("Failed to fetch model fields.");
        }

        // 2. Determine which fields to request based on kwargs
        List<String> fieldsToFetch = new ArrayList<>();
        for (Iterator<String> it = allFieldsJson.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            if (desiredFields.isEmpty() || desiredFields.contains(fieldName)) {
                fieldsToFetch.add(fieldName);
            }
        }

        // 3. Now query actual data with filtered fields
        String fieldsArray = objectMapper.writeValueAsString(fieldsToFetch);
        String dataPayload = String.format(
                "{\"jsonrpc\": \"2.0\", \"method\": \"call\", " +
                        "\"params\": {\"service\": \"object\", \"method\": \"execute_kw\", " +
                        "\"args\": [\"%s\", %d, \"%s\", \"%s\", \"search_read\", [[]], {\"fields\": %s, \"limit\": 6}]}, \"id\": 3}",
                db, uid, password, model, fieldsArray);

        String dataResponse = postJson(url + "/jsonrpc", dataPayload);
        JsonNode result = objectMapper.readTree(dataResponse).get("result");

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (result != null && result.isArray()) {
            for (JsonNode node : result) {
                resultList.add(objectMapper.convertValue(node, Map.class));
            }
        }

        return resultList;
    }

    private String postJson(String url, String jsonPayload) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .build();

        // Use Apache's SSLContext builder with TrustAllStrategy
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

        // Use Apache's SSLConnectionSocketFactory
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(socketFactory)
                                .build())
                .build()) {

            HttpPost post = new HttpPost(url);
            post.setHeader("Content-type", "application/json");
            post.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

            try (var response = client.execute(post)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                } else {
                    throw new IOException("No response from server");
                }
            }
        }
    }

    private SSLContext createTrustAllSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        }, new java.security.SecureRandom());
        return sslContext;
    }
}
