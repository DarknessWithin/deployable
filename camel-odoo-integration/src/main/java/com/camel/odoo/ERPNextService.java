package com.camel.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;

public class ERPNextService implements ERPService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        String baseUrl = in.getHeader("url", String.class);              // e.g. https://your-site.frappe.cloud
        String apiKey = CryptoUtil.decrypt(in.getHeader("apikey", String.class));            // e.g. ea61d89fc8d22c3
        String apiSecret = CryptoUtil.decrypt(in.getHeader("apisecret", String.class));      // e.g. your_api_secret
        String doctype = in.getHeader("model", String.class);            // e.g. "User"
        String kwargs = in.getHeader("kwargs", String.class);            // e.g. "name,email"

        if (baseUrl == null || apiKey == null || apiSecret == null || doctype == null) {
            throw new IllegalArgumentException("Missing required headers: 'url', 'apikey', 'apisecret', or 'model'");
        }

        String fullUrl = baseUrl + "/api/resource/" + doctype;

        HttpGet httpGet = new HttpGet(fullUrl);
        httpGet.setHeader("Authorization", "token " + apiKey + ":" + apiSecret);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-Type", "application/json");


        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpGet)) {

            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new RuntimeException("ERPNext API call failed with status " + statusCode + ": " + responseBody);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.get("data");

            if (kwargs != null && !kwargs.isEmpty() && data.isArray()) {
                ArrayNode filtered = objectMapper.createArrayNode();
                for (JsonNode node : data) {
                    filtered.add(filterFields(node, kwargs));
                }
                exchange.getMessage().setBody(filtered);
            } else {
                exchange.getMessage().setBody(data);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while accessing ERPNext API", e);
        }
    }

    private JsonNode filterFields(JsonNode node, String kwargs) {
        String[] fields = kwargs.split(",");
        ObjectNode result = objectMapper.createObjectNode();
        for (String field : fields) {
            if (node.has(field)) {
                result.set(field, node.get(field));
            }
        }
        return result;
    }
}
