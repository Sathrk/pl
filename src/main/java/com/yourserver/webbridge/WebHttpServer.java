package com.yourpackage.webserver;

import com.sun.net.httpserver.HttpServer;
import com.yourpackage.webserver.handler.*;
import com.yourpackage.webserver.listener.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class WebHttpServer extends JavaPlugin {
    private HttpServer server;

    @Override
    public void onEnable() {
        try {
            // Event Listeners Register
            getServer().getPluginManager().registerEvents(new ChatListener(), this);

            // HTTP Server Init
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Handlers Attachment
            server.createContext("/api/map-image", new MapImageHandler(this));
            server.createContext("/api/chat", new GetChatHandler());
            server.createContext("/api/send-chat", new SendChatHandler(this));

            server.setExecutor(null);
            server.start();

            getLogger().info("WebHttpServer successfully started on port 8080!");
        } catch (Exception e) {
            getLogger().severe("Failed to start WebHttpServer: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop(0);
            getLogger().info("WebHttpServer stopped.");
        }
    }
}
