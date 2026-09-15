package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.WebBridgeMain;
import com.yourserver.webbridge.manager.ClaimManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.io.IOException;

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

        // 1. GET: Fetch Available Shop Items & Prices
        if ("GET".equalsIgnoreCase(method)) {
            String shopJson = "{"
                + "\"items\":["
                + "{\"id\":\"DIAMOND\",\"name\":\"Diamond\",\"price\":100.0},"
                + "{\"id\":\"NETHERITE_INGOT\",\"name\":\"Netherite Ingot\",\"price\":500.0},"
                + "{\"id\":\"GOLDEN_APPLE\",\"name\":\"Golden Apple\",\"price\":50.0}"
                + "]}";
            CorsHelper.sendJson(exchange, shopJson);
            return;
        }

        // 2. POST: Buy Item using Vault Economy
        if ("POST".equalsIgnoreCase(method)) {
            String username = CorsHelper.getQueryParam(query, "username", "");
            String itemId = CorsHelper.getQueryParam(query, "item", "").toUpperCase();
            int amount = 1;

            try {
                amount = Integer.parseInt(CorsHelper.getQueryParam(query, "amount", "1"));
            } catch (NumberFormatException ignored) {}

            if (username.isEmpty() || itemId.isEmpty()) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Missing username or item\"}");
                return;
            }

            Economy economy = WebBridgeMain.getEconomy();
            if (economy == null) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Vault Economy plugin not found on server!\"}");
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            
            // Hardcoded prices matching GET request (or dynamic EconomyShopGUI integration)
            double pricePerItem = itemId.equals("NETHERITE_INGOT") ? 500.0 : (itemId.equals("GOLDEN_APPLE") ? 50.0 : 100.0);
            double totalPrice = pricePerItem * amount;

            if (!economy.has(player, totalPrice)) {
                CorsHelper.sendJson(exchange, String.format("{\"success\":false,\"message\":\"Insufficient funds! Need $%.2f\"}", totalPrice));
                return;
            }

            // Deduct money via Vault API
            economy.withdrawPlayer(player, totalPrice);

            // Add item to claim queue
            try {
                Material material = Material.valueOf(itemId);
                ClaimManager.addClaim(username, material, amount);
                CorsHelper.sendJson(exchange, String.format("{\"success\":true,\"message\":\"Purchased successfully for $%.2f! Type /claim in-game.\",\"balance\":%.2f}", 
                    totalPrice, economy.getBalance(player)));
            } catch (IllegalArgumentException e) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Invalid material type\"}");
            }
        }
    }
}
