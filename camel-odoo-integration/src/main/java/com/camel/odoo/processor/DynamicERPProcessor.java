package com.camel.odoo.processor;

import com.camel.odoo.ERPService;
import com.camel.odoo.ERPServiceRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class DynamicERPProcessor implements Processor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        JsonNode root = objectMapper.readTree(body);

        String erp = root.path("erp").asText(null);
        if (erp == null) {
            sendError(exchange, "Missing 'erp' field");
            return;
        }

        String username = root.path("username").asText(null);
        String password = root.path("password").asText(null);
        String db = root.path("db").asText(null);
        String url = root.path("url").asText(null);

        if (username == null || password == null || db == null || url == null) {
            sendError(exchange, "Missing one or more required fields: username, password, db, url");
            return;
        }

        exchange.getIn().setHeader("username", username);
        exchange.getIn().setHeader("password", password);
        exchange.getIn().setHeader("db", db);
        exchange.getIn().setHeader("url", url);

        ERPService erpService = ERPServiceRegistry.getService(erp);
        if (erpService == null) {
            sendError(exchange, "Unsupported ERP type: " + erp);
            return;
        }

        // Call the ERP handler
        erpService.handle(exchange);
    }


    // Method to send error response in case of invalid input or unsupported ERP
    private void sendError(Exchange exchange, String errorMessage) {
        exchange.getMessage().setBody("{\"error\": \"" + errorMessage + "\"}");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400); // Bad Request
    }
}
