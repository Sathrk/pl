package com.yourserver.webbridge;

import com.sun.net.httpserver.HttpServer;
import com.yourserver.webbridge.handler.*;
import com.yourserver.webbridge.listener.ChatListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class WebBridgeMain extends JavaPlugin implements CommandExecutor {
    private HttpServer server;

    @Override
    public void onEnable() {
        // Register Event Listeners
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
        } catch (Exception e) {
            getLogger().warning("ChatListener registration warning: " + e.getMessage());
        }

        // Register Command Executors declared in plugin.yml
        if (getCommand("web") != null) {
            getCommand("web").setExecutor(this);
        }
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(this);
        }

        // Start HTTP Server
        try {
            int port = 8080;
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/api/map-image", new MapImageHandler(this));
            server.createContext("/api/chat", new GetChatHandler());
            server.createContext("/api/send-chat", new SendChatHandler(this));

            server.setExecutor(null);
            server.start();

            getLogger().info("WebBridge HTTP Server started on port " + port);
        } catch (Exception e) {
            getLogger().severe("Failed to start HTTP Server on port 8080: " + e.getMessage());
            getLogger().severe("Check if port 8080 is already used by another application.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("web")) {
            sender.sendMessage("§a[WebBridge] Use the web dashboard interface to interact.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("claim")) {
            sender.sendMessage("§a[WebBridge] No store claims pending.");
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop(0);
            getLogger().info("WebBridge HTTP Server stopped.");
        }
    }
}
