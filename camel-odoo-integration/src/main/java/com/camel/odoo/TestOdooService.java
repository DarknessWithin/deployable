package com.camel.odoo;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestOdooService {

    private static final String ODOO_URL = "https://my-new-company.odoo.com";
    private static final String DB_NAME = "my-new-company";
    private static final String USERNAME = "tanishq_s@ch.iitr.ac.in";
    private static final String PASSWORD = "Muqvar-kiqsec-5jogro";

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        int uid = authenticate();
        if (uid == -1) {
            System.out.println("❌ Authentication failed.");
            return;
        }

        System.out.println("✅ Authenticated with UID: " + uid);

        String data = fetchOdooData(uid);
        System.out.println("📦 Odoo Data: " + data);
    }

    private static int authenticate() throws IOException {
        String payload = String.format(
                "{ \"jsonrpc\": \"2.0\", \"method\": \"call\", \"params\": { " +
                        "\"service\": \"common\", \"method\": \"login\", " +
                        "\"args\": [\"%s\", \"%s\", \"%s\"] }, \"id\": 1 }",
                DB_NAME, USERNAME, PASSWORD
        );

        RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(ODOO_URL + "/jsonrpc")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            JsonNode json = objectMapper.readTree(response.body().string());
            JsonNode result = json.get("result");
            return (result != null && result.isInt()) ? result.asInt() : -1;
        }
    }

    private static String fetchOdooData(int uid) throws IOException {
        String payload = String.format(
                "{ \"jsonrpc\": \"2.0\", \"method\": \"call\", \"params\": { " +
                        "\"service\": \"object\", \"method\": \"execute_kw\", " +
                        "\"args\": [\"%s\", %d, \"%s\", \"res.partner\", \"search_read\", [[]] ] }, \"id\": 2 }",
                DB_NAME, uid, PASSWORD
        );

        RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(ODOO_URL + "/jsonrpc")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);
            return response.body().string();
        }
    }
}
