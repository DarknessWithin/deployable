package com.camel.odoo;

import java.util.HashMap;
import java.util.Map;

public class ERPServiceRegistry {

    private static final Map<String, ERPService> services = new HashMap<>();

    static {
        services.put("odoo", (ERPService) new OdooService());
    }

    public static ERPService getService(String erpName) {
        return services.get(erpName.toLowerCase());
    }
}
