package com.camel.odoo;

import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;

public class ERPNextServiceTest {

    public static void main(String[] args) {
        try {
            // Create a Camel context
            CamelContext context = new DefaultCamelContext();
            Exchange exchange = new DefaultExchange(context);

            // Set the message and required headers
            DefaultMessage message = new DefaultMessage(context);
            message.setHeader("url", "https://my-new-company.erpnext.com"); // ✅ not with /api/resource/User
            // Replace with your actual ERPNext URL
            message.setHeader("apikey", "Rw0qH3SCftxjKKkey3fJ9Q==");                          // Replace with your actual API key
            message.setHeader("apisecret", "cedhcNevWC//BwHoJuMnyA==");                    // Replace with your actual API secret
            message.setHeader("model", "User");                                   // ERPNext doctype (e.g., "User", "Item", etc.)
            message.setHeader("kwargs", "name,email");                            // Comma-separated fields to filter

            exchange.setIn(message);

            // Call your service
            ERPNextService service = new ERPNextService();
            service.handle(exchange);

            // Print the response
            Object body = exchange.getMessage().getBody();
            System.out.println("Filtered ERPNext Data:\n" + body.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
