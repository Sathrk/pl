package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AuthHandler implements HttpHandler {
    private static final Map<String, String> pendingCodes = new HashMap<>();

    public static String generateCode(String username) {
        String code = String.format("%06d", new Random().nextInt(999999));
        pendingCodes.put(code, username);
        return code;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String code = CorsHelper.getQueryParam(query, "code", "");

        if (pendingCodes.containsKey(code)) {
            String username = pendingCodes.remove(code);
            CorsHelper.sendJson(exchange, "{\"success\":true,\"username\":\"" + username + "\"}");
        } else {
            CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Invalid or expired code\"}");
        }
    }
}
