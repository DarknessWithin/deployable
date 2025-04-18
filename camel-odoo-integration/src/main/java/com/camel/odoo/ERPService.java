package com.camel.odoo;

import org.apache.camel.Exchange;

public interface ERPService {
    void handle(Exchange exchange) throws Exception;
}
