package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;

public class GetInventoryHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String username = CorsHelper.getQueryParam(query, "username", "");

        if (username.isEmpty()) {
            CorsHelper.sendJson(exchange, "{\"error\":\"Username required\"}");
            return;
        }

        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            CorsHelper.sendJson(exchange, "{\"online\":false}");
            return;
        }

        CorsHelper.sendJson(exchange, "{\"online\":true, \"player\":\"" + player.getName() + "\"}");
    }
}
