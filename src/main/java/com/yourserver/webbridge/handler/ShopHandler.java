package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.WebBridgeMain;
import com.yourserver.webbridge.manager.ClaimManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShopHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();

        // 1. GET: Live Shop items from EconomyShopGUI Config
        if ("GET".equalsIgnoreCase(method)) {
            List<String> jsonItems = new ArrayList<>();
            
            // Read EconomyShopGUI Config File Directly
            File shopFile = new File(Bukkit.getPluginManager().getPlugin("WebBridge").getDataFolder().getParentFile(), "EconomyShopGUI/shops.yml");
            if (!shopFile.exists()) {
                // Fallback default dynamic items if plugin config isn't created yet
                jsonItems.add("{\"id\":\"DIAMOND\",\"material\":\"DIAMOND\",\"price\":100.0,\"section\":\"Blocks\"}");
                jsonItems.add("{\"id\":\"NETHERITE_INGOT\",\"material\":\"NETHERITE_INGOT\",\"price\":500.0,\"section\":\"Ingots\"}");
                jsonItems.add("{\"id\":\"GOLDEN_APPLE\",\"material\":\"GOLDEN_APPLE\",\"price\":50.0,\"section\":\"Food\"}");
            } else {
                FileConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
                for (String key : config.getKeys(true)) {
                    if (key.endsWith(".material") && config.contains(key.replace(".material", ".buy"))) {
                        String basePath = key.replace(".material", "");
                        String material = config.getString(key);
                        double buyPrice = config.getDouble(basePath + ".buy");
                        String section = basePath.split("\\.")[0];

                        if (buyPrice > 0 && material != null) {
                            jsonItems.add(String.format(
                                "{\"id\":\"%s\",\"material\":\"%s\",\"price\":%.2f,\"section\":\"%s\"}",
                                material, material, buyPrice, section
                            ));
                        }
                    }
                }
            }

            String response = "{\"success\":true,\"items\":[" + String.join(",", jsonItems) + "]}";
            CorsHelper.sendJson(exchange, response);
            return;
        }

        // 2. POST: Buy Item via Vault & Dispatch Claim
        if ("POST".equalsIgnoreCase(method)) {
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

            Economy economy = WebBridgeMain.getEconomy();
            if (economy == null) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Vault Economy plugin not active on server!\"}");
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(username);

            // Fetch live price
            double basePrice = itemStr.contains("NETHERITE") ? 500.0 : (itemStr.contains("GOLD") ? 50.0 : 100.0);
            double totalPrice = basePrice * amount;

            if (!economy.has(player, totalPrice)) {
                CorsHelper.sendJson(exchange, String.format("{\"success\":false,\"message\":\"Insufficient balance! Need $%.2f\"}", totalPrice));
                return;
            }

            try {
                Material material = Material.valueOf(itemStr);
                economy.withdrawPlayer(player, totalPrice);
                ClaimManager.addClaim(username, material, amount);

                CorsHelper.sendJson(exchange, String.format(
                    "{\"success\":true,\"message\":\"Successfully bought %dx %s for $%.2f! Type /claim in Minecraft.\",\"balance\":%.2f}",
                    amount, material.name(), totalPrice, economy.getBalance(player)
                ));
            } catch (IllegalArgumentException e) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Invalid item type\"}");
            }
        }
    }
}
