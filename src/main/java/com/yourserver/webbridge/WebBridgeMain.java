package com.yourserver.webbridge;

import com.sun.net.httpserver.HttpServer;
import com.yourserver.webbridge.handler.*;
import com.yourserver.webbridge.listener.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class WebBridgeMain extends JavaPlugin {
    private HttpServer server;

    @Override
    public void onEnable() {
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);

            server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/map-image", new MapImageHandler(this));
            server.createContext("/api/chat", new GetChatHandler());
            server.createContext("/api/send-chat", new SendChatHandler(this));

            server.setExecutor(null);
            server.start();

            getLogger().info("WebBridge successfully started on port 8080!");
        } catch (Exception e) {
            getLogger().severe("Failed to start WebBridge: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop(0);
            getLogger().info("WebBridge stopped.");
        }
    }
}
