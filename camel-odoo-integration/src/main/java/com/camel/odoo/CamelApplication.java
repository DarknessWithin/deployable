package com.camel.odoo;


import org.apache.camel.main.Main;
import org.apache.camel.main.MainListenerSupport;

public class CamelApplication {
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(DynamicERPRouteBuilder.class);
        main.bind("odooService", new OdooService());
        main.bind("sapService", new SAPService());
        main.bind("erpnextService", new ERPNextService());
// Add more as needed

        main.addMainListener(new MainListenerSupport() {
            public void afterStart(Main main) {
                System.out.println("✅ Camel ERP Service started on http://localhost:8080/erp/sync");
            }

            public void beforeStop(Main main) {
                System.out.println("🛑 Camel ERP Service is shutting down...");
            }
        });

        main.run(args);
    }
}



