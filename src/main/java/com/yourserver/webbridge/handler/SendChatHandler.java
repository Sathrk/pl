package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.manager.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

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
        String username = CorsHelper.getQueryParam(query, "username", "WebUser");
        String message = CorsHelper.getQueryParam(query, "message", "");

        if (message.isEmpty()) {
            CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Message cannot be empty\"}");
            return;
        }

        // 1. Add to WebBridge Chat Manager Memory (So it appears on Web UI)
        ChatManager.addMessage("[Web] " + username, message);

        // 2. Broadcast inside Minecraft Server (Main Thread)
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage("§b[Web] §f" + username + ": §7" + message);
        });

        CorsHelper.sendJson(exchange, "{\"success\":true}");
    }
}
