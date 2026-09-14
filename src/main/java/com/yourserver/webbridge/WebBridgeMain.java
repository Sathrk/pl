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
            // Register Event Listeners
            getServer().getPluginManager().registerEvents(new ChatListener(), this);

            // Bind to Port 8080 (Fallback if port is busy)
            int port = 8080;
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register Endpoints
            server.createContext("/api/map-image", new MapImageHandler(this));
            server.createContext("/api/chat", new GetChatHandler());
            server.createContext("/api/send-chat", new SendChatHandler(this));

            server.setExecutor(null);
            server.start();

            getLogger().info("WebBridge successfully enabled on port " + port);
        } catch (Exception e) {
            getLogger().severe("Could not start HTTP Server: " + e.getMessage());
            e.printStackTrace();
            // Plugin ko safely disable karein crash hone se bachane ke liye
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop(0);
            getLogger().info("WebBridge HTTP Server stopped.");
        }
    }
}
