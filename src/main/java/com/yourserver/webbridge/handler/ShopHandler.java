package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.WebBridgeMain;
import com.yourserver.webbridge.manager.ClaimManager;
import me.gy2002.economyshopgui.api.EconomyShopGUIHook;
import me.gy2002.economyshopgui.objects.ShopItem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // Check if EconomyShopGUI is loaded on server
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") == null && 
            Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium") == null) {
            CorsHelper.sendJson(exchange, "{\"error\":\"EconomyShopGUI plugin not found on server\"}");
            return;
        }

        // 1. GET Request: Read Live Items directly from EconomyShopGUI API
        if ("GET".equalsIgnoreCase(method)) {
            List<String> jsonItems = new ArrayList<>();

            // Fetch shop items from EconomyShopGUI memory
            Map<String, ShopItem> shopItems = EconomyShopGUIHook.getShopItems();
            if (shopItems != null) {
                for (Map.Entry<String, ShopItem> entry : shopItems.entrySet()) {
                    ShopItem item = entry.getValue();
                    if (item != null && item.getItemToGive() != null) {
                        String matName = item.getItemToGive().getType().name();
                        double buyPrice = item.getBuyPrice();

                        // Exclude non-buyable items
                        if (buyPrice > 0) {
                            jsonItems.add(String.format(
                                "{\"id\":\"%s\",\"material\":\"%s\",\"price\":%.2f,\"section\":\"%s\"}",
                                entry.getKey(), matName, buyPrice, item.getShopSection()
                            ));
                        }
                    }
                }
            }

            String response = "{\"success\":true,\"items\":[" + String.join(",", jsonItems) + "]}";
            CorsHelper.sendJson(exchange, response);
            return;
        }

        // 2. POST Request: Buy Item from EconomyShopGUI and add to /claim
        if ("POST".equalsIgnoreCase(method)) {
            String username = CorsHelper.getQueryParam(query, "username", "");
            String itemId = CorsHelper.getQueryParam(query, "item", "");
            int amount = 1;

            try {
                amount = Integer.parseInt(CorsHelper.getQueryParam(query, "amount", "1"));
            } catch (NumberFormatException ignored) {}

            if (username.isEmpty() || itemId.isEmpty()) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Missing player or item ID\"}");
                return;
            }

            ShopItem sItem = EconomyShopGUIHook.getShopItem(itemId);
            if (sItem == null) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Item no longer exists in EconomyShopGUI\"}");
                return;
            }

            double totalPrice = sItem.getBuyPrice() * amount;
            Economy economy = WebBridgeMain.getEconomy();

            if (economy == null) {
                CorsHelper.sendJson(exchange, "{\"success\":false,\"message\":\"Vault Economy not active\"}");
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(username);

            if (!economy.has(player, totalPrice)) {
                CorsHelper.sendJson(exchange, String.format("{\"success\":false,\"message\":\"Insufficient funds! Needs $%.2f\"}", totalPrice));
                return;
            }

            // Deduct balance and queue item for /claim
            economy.withdrawPlayer(player, totalPrice);
            Material mat = sItem.getItemToGive().getType();
            ClaimManager.addClaim(username, mat, amount);

            CorsHelper.sendJson(exchange, String.format(
                "{\"success\":true,\"message\":\"Purchased %dx %s for $%.2f! Type /claim in-game.\",\"balance\":%.2f}",
                amount, mat.name(), totalPrice, economy.getBalance(player)
            ));
        }
    }
}
