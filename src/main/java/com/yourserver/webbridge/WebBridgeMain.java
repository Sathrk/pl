package com.yourserver.webbridge;

import com.sun.net.httpserver.HttpServer;
import com.yourserver.webbridge.handler.*;
import com.yourserver.webbridge.listener.ChatListener;
import com.yourserver.webbridge.manager.ClaimManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.List;

public class WebBridgeMain extends JavaPlugin implements CommandExecutor {
    private HttpServer server;

    @Override
    public void onEnable() {
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
        } catch (Exception e) {
            getLogger().warning("ChatListener registration warning: " + e.getMessage());
        }

        if (getCommand("web") != null) getCommand("web").setExecutor(this);
        if (getCommand("claim") != null) getCommand("claim").setExecutor(this);

        try {
            int port = 12935;
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/api/map-image", new MapImageHandler(this));
            server.createContext("/api/chat", new GetChatHandler());
            server.createContext("/api/send-chat", new SendChatHandler(this));
            server.createContext("/api/inventory", new GetInventoryHandler());
            server.createContext("/api/map", new GetMapHandler());
            server.createContext("/api/login", new AuthHandler());
            server.createContext("/api/store-claim", new ClaimHandler()); // <--- Store Claim Endpoint

            server.setExecutor(null);
            server.start();

            getLogger().info("WebBridge HTTP Server started successfully on port " + port);
        } catch (Exception e) {
            getLogger().severe("Failed to start HTTP Server on port 12935: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("web")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("login")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used in-game.");
                    return true;
                }
                Player player = (Player) sender;
                String code = AuthHandler.generateCode(player.getName());
                player.sendMessage("§a[WebBridge] Your login code is: §e" + code);
                player.sendMessage("§7Enter this code on the web dashboard to log in.");
                return true;
            }
            sender.sendMessage("§a[WebBridge] Usage: /web login");
            return true;
        }

        if (command.getName().equalsIgnoreCase("claim")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can claim items.");
                return true;
            }

            Player player = (Player) sender;
            if (!ClaimManager.hasClaims(player.getName())) {
                player.sendMessage("§c[WebBridge] You have no pending store claims!");
                return true;
            }

            List<ItemStack> items = ClaimManager.getAndClearClaims(player.getName());
            for (ItemStack item : items) {
                player.getInventory().addItem(item);
            }

            player.sendMessage("§a[WebBridge] Successfully claimed " + items.size() + " reward package(s)!");
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
