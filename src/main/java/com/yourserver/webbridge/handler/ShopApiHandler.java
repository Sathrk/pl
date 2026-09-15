package com.webbridge.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ShopApiHandler implements HttpHandler {

    private final JavaPlugin plugin;

    public ShopApiHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS for Dashboard
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        JSONObject responseJson = new JSONObject();
        JSONArray itemsArray = new JSONArray();

        // Path to EconomyShopGUI sections folder
        File ecoShopFolder = new File(plugin.getDataFolder().getParentFile(), "EconomyShopGUI/sections");

        if (ecoShopFolder.exists() && ecoShopFolder.isDirectory()) {
            File[] sectionFiles = ecoShopFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (sectionFiles != null) {
                for (File file : sectionFiles) {
                    String categoryName = file.getName().replace(".yml", "").toUpperCase();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    // Scan all items inside this category section
                    for (String key : config.getKeys(false)) {
                        ConfigurationSection itemSec = config.getConfigurationSection(key);
                        if (itemSec != null) {
                            String material = itemSec.getString("material", key).toUpperCase();
                            double buyPrice = itemSec.getDouble("buy", 0.0);

                            // Only show items that are purchasable (price > 0)
                            if (buyPrice > 0) {
                                JSONObject itemObj = new JSONObject();
                                itemObj.put("id", key);
                                itemObj.put("material", material);
                                itemObj.put("price", buyPrice);
                                itemObj.put("section", categoryName);
                                itemsArray.put(itemObj);
                            }
                        }
                    }
                }
            }
            responseJson.put("success", true);
            responseJson.put("items", itemsArray);
        } else {
            // Fallback response if EconomyShopGUI folder is not found
            responseJson.put("success", false);
            responseJson.put("message", "EconomyShopGUI folder not found at plugins/EconomyShopGUI/sections/");
        }

        byte[] responseBytes = responseJson.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
