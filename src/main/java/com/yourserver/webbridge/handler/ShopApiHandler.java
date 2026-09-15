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

        // EconomyShopGUI ke sections folder ka path
        File ecoShopFolder = new File(plugin.getDataFolder().getParentFile(), "EconomyShopGUI/sections");

        if (ecoShopFolder.exists() && ecoShopFolder.isDirectory()) {
            File[] sectionFiles = ecoShopFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (sectionFiles != null) {
                for (File file : sectionFiles) {
                    String categoryName = file.getName().replace(".yml", "").toUpperCase();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                    for (String key : config.getKeys(false)) {
                        ConfigurationSection itemSec = config.getConfigurationSection(key);
                        if (itemSec != null) {
                            String material = itemSec.getString("material", key).toUpperCase();
                            double buyPrice = itemSec.getDouble("buy", 0.0);

                            if (buyPrice > 0) {
                                JsonObject itemObj = new JsonObject();
                                itemObj.addProperty("id", key);
                                itemObj.addProperty("material", material);
                                itemObj.addProperty("price", buyPrice);
                                itemObj.addProperty("section", categoryName);
                                itemsArray.add(itemObj);
                            }
                        }
                    }
                }
            }
            responseJson.addProperty("success", true);
            responseJson.add("items", itemsArray);
        } else {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "EconomyShopGUI folder not found!");
        }

        byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
