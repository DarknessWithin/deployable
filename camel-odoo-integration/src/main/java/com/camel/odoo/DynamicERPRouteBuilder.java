package com.camel.odoo;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class DynamicERPRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // 🔢 Get port from environment variable or default to 8080
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // 🌐 Configure REST DSL to use Netty HTTP
        restConfiguration()
                .component("netty-http")
                .host("0.0.0.0")
                .port(port)
                .bindingMode(RestBindingMode.off)
                .dataFormatProperty("prettyPrint", "true");

        // 🧭 Define REST endpoints
        rest("/erp")
                .post("/sync")
                .consumes("application/json")
                .produces("application/json")
                .to("direct:start")
                .get("/get-data")
                .produces("application/json")
                .to("direct:getData");

        // 🚨 Global error handler
        onException(Exception.class)
                .log("🚨 Error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody().constant("{\"error\": \"Internal Server Error\"}");

        // 🔁 Handle POST /erp/sync
        from("direct:start")
                .routeId("erp-dynamic-sync")
                .log("📥 Received sync request: ${body}")
                .process(new com.camel.odoo.processor.DynamicERPProcessor())
                .log("📤 Sync response: ${body}");

        // 🔍 Handle GET /erp/get-data
        from("direct:getData")
                .routeId("erp-get-data")
                .log("📥 Received get-data request for ERP: ${header.erp}")
                .process(exchange -> {
                    String erp = exchange.getIn().getHeader("erp", String.class);
                    if (erp == null || erp.isEmpty()) {
                        throw new IllegalArgumentException("Missing required 'erp' header");
                    }

                    String beanName = erp.toLowerCase() + "Service";
                    Object serviceBean = exchange.getContext().getRegistry().lookupByName(beanName);

                    if (serviceBean == null) {
                        throw new IllegalArgumentException("No service registered for ERP: " + erp);
                    }

                    serviceBean.getClass().getMethod("handle", Exchange.class).invoke(serviceBean, exchange);
                })
                .log("📤 Returning ERP data: ${body}");
    }
}
