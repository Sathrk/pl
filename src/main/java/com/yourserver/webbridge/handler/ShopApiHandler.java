package com.yourserver.webbridge.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.gyantom.economyshopgui.api.EconomyShopGUIHook;
import me.gyantom.economyshopgui.objects.ShopItem;
import me.gyantom.economyshopgui.util.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ShopApiHandler implements HttpHandler {

    private final JavaPlugin plugin;

    public ShopApiHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        JsonObject responseJson = new JsonObject();
        JsonArray itemsArray = new JsonArray();

        // Check if EconomyShopGUI plugin is loaded on server
        if (Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI")) {
            try {
                // Loop through all active shop categories in server memory
                for (ShopCategory category : EconomyShopGUIHook.getShopCategories()) {
                    String categoryName = category.getCategoryName().toUpperCase();

                    // Get all items in this specific category
                    for (ShopItem item : EconomyShopGUIHook.getShopItems(category)) {
                        if (item != null) {
                            String material = item.getItemToGive().getType().name();
                            double buyPrice = item.getBuyPrice();

                            JsonObject itemObj = new JsonObject();
                            itemObj.addProperty("id", item.getItemName());
                            itemObj.addProperty("material", material);
                            itemObj.addProperty("price", buyPrice);
                            itemObj.addProperty("section", categoryName);
                            
                            itemsArray.add(itemObj);
                        }
                    }
                }

                responseJson.addProperty("success", true);
                responseJson.add("items", itemsArray);
            } catch (Exception e) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Error fetching EconomyShopGUI API data: " + e.getMessage());
            }
        } else {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "EconomyShopGUI plugin is not enabled on server!");
        }

        byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
