package com.yourserver.webbridge.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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

        // EconomyShopGUI main folder & sections folder
        File ecoShopFolder = new File(plugin.getDataFolder().getParentFile(), "EconomyShopGUI/sections");
        if (!ecoShopFolder.exists() || !ecoShopFolder.isDirectory()) {
            ecoShopFolder = new File(plugin.getDataFolder().getParentFile(), "EconomyShopGUI/shops");
        }

        if (ecoShopFolder.exists() && ecoShopFolder.isDirectory()) {
            File[] sectionFiles = ecoShopFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (sectionFiles != null) {
                for (File file : sectionFiles) {
                    String categoryName = file.getName().replace(".yml", "").toUpperCase();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    // Scan sections recursively
                    parseConfigSection(config, categoryName, itemsArray);
                }
            }
            responseJson.addProperty("success", true);
            responseJson.add("items", itemsArray);
        } else {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "EconomyShopGUI sections/shops folder not found!");
        }

        byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private void parseConfigSection(ConfigurationSection section, String categoryName, JsonArray itemsArray) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection itemSec = section.getConfigurationSection(key);
                if (itemSec == null) continue;

                // Extract price (handles "buy", "buy-price", "price")
                double buyPrice = 0.0;
                if (itemSec.contains("buy")) {
                    buyPrice = itemSec.getDouble("buy");
                } else if (itemSec.contains("buy-price")) {
                    buyPrice = itemSec.getDouble("buy-price");
                } else if (itemSec.contains("price")) {
                    buyPrice = itemSec.getDouble("price");
                }

                // Extract material/item type
                String material = itemSec.getString("material", itemSec.getString("item", key)).toUpperCase();

                // If valid item entry found
                if (buyPrice >= 0 && (itemSec.contains("material") || itemSec.contains("item") || itemSec.contains("buy"))) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", key);
                    itemObj.addProperty("material", material);
                    itemObj.addProperty("price", buyPrice);
                    itemObj.addProperty("section", categoryName);
                    itemsArray.add(itemObj);
                } else {
                    // Recurse into nested sub-sections if any
                    parseConfigSection(itemSec, categoryName, itemsArray);
                }
            }
        }
    }
}
