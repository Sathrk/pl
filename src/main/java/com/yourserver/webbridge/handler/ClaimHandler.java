package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.manager.ClaimManager;
import org.bukkit.Material;

import java.io.IOException;

public class ClaimHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String username = CorsHelper.getQueryParam(query, "username", "");
        String itemStr = CorsHelper.getQueryParam(query, "item", "").toUpperCase();
        int amount = 1;

        try {
            amount = Integer.parseInt(CorsHelper.getQueryParam(query, "amount", "1"));
        } catch (NumberFormatException ignored) {}

        if (username.isEmpty() || itemStr.isEmpty()) {
            CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Missing username or item\"}");
            return;
        }

        try {
            Material material = Material.valueOf(itemStr);
            ClaimManager.addClaim(username, material, amount);
            CorsHelper.sendJson(exchange, "{\"success\":true,\"message\":\"Item queued for claim! Type /claim in-game.\"}");
        } catch (IllegalArgumentException e) {
            CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Invalid item type\"}");
        }
    }
}
