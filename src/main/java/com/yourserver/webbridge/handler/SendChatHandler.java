package com.yourpackage.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SendChatHandler implements HttpHandler {
    private final JavaPlugin plugin;

    public SendChatHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String user = URLDecoder.decode(CorsHelper.getQueryParam(query, "username", "WebUser"), StandardCharsets.UTF_8);
        String msg = URLDecoder.decode(CorsHelper.getQueryParam(query, "message", ""), StandardCharsets.UTF_8);

        if (!msg.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage("§8[§bWeb§8] §f" + user + ": " + msg);
            });
            CorsHelper.sendJson(exchange, "{\"success\":true}");
        } else {
            CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Empty message\"}");
        }
    }
}
