package com.survival.webdashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.Color; // Standard AWT Color (Import fix)
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WebHttpServer {

    private final WebBridgeMain plugin;
    private HttpServer httpServer;
    private final int port = 12935;

    public WebHttpServer(WebBridgeMain plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // API Routing
            httpServer.createContext("/api/verify", new VerifyHandler());
            httpServer.createContext("/api/inventory", new InventoryHandler());
            httpServer.createContext("/api/add-claim", new AddClaimHandler());
            httpServer.createContext("/api/map", new MapDataHandler());
            httpServer.createContext("/api/map-image", new MapImageHandler());

            httpServer.setExecutor(null);
            httpServer.start();
            plugin.getLogger().info("Web HTTP Server listening on port " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not start Web HTTP Server: " + e.getMessage());
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    // --- 1. Passcode Verification Endpoint ---
    private class VerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send204(exchange); return; }

            String code = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            String username = plugin.getPasscodeManager().verifyCode(code);

            if (username != null) {
                sendJson(exchange, "{\"success\":true,\"username\":\"" + username + "\"}");
            } else {
                sendJson(exchange, "{\"success\":false,\"message\":\"Invalid or expired passcode\"}");
            }
        }
    }

    // --- 2. Live Player Inventory (Hotbar) Sync ---
    private class InventoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send204(exchange); return; }

            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");

            Player player = (username != null) ? Bukkit.getPlayerExact(username) : null;
            if (player != null && player.isOnline()) {
                StringBuilder json = new StringBuilder("{\"online\":true,\"hotbar\":[");
                for (int i = 0; i < 9; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    String itemType = (item != null && item.getType() != org.bukkit.Material.AIR) ? item.getType().name() : "AIR";
                    int amount = (item != null) ? item.getAmount() : 0;
                    json.append("{\"slot\":").append(i).append(",\"item\":\"").append(itemType).append("\",\"amount\":").append(amount).append("}");
                    if (i < 8) json.append(",");
                }
                json.append("]}");
                sendJson(exchange, json.toString());
            } else {
                sendJson(exchange, "{\"online\":false}");
            }
        }
    }

    // --- 3. Web Store Item Queue Handler ---
    private class AddClaimHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send204(exchange); return; }

            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            String item = getQueryParam(query, "item");
            int amount = Integer.parseInt(getQueryParam(query, "amount", "1"));

            if (username != null && item != null) {
                plugin.getClaimManager().addClaim(username, item, amount);
                sendJson(exchange, "{\"success\":true}");
            } else {
                sendJson(exchange, "{\"success\":false,\"message\":\"Missing arguments\"}");
            }
        }
    }

    // --- 4. Radar Map Players Location Handler ---
    private class MapDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send204(exchange); return; }

            StringBuilder json = new StringBuilder("{\"players\":[");
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (count > 0) json.append(",");
                json.append("{")
                    .append("\"name\":\"").append(p.getName()).append("\",")
                    .append("\"x\":").append(p.getLocation().getBlockX()).append(",")
                    .append("\"y\":").append(p.getLocation().getBlockY()).append(",")
                    .append("\"z\":").append(p.getLocation().getBlockZ())
                    .append("}");
                count++;
            }
            json.append("]}");
            sendJson(exchange, json.toString());
        }
    }

    // --- 5. Real-Time 2D World Terrain Image Handler (FIXED & Dynamic Coordinates) ---
    private class MapImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send204(exchange); return; }

            String query = exchange.getRequestURI().getQuery();
            int centerX = Integer.parseInt(getQueryParam(query, "x", "0"));
            int centerZ = Integer.parseInt(getQueryParam(query, "z", "0"));

            // Async Thread - Bukkit Server par zero lag
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    World mainWorld = Bukkit.getWorlds().get(0);
                    int radius = 100; // View distance radius
                    int size = radius * 2;
                    BufferedImage mapImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

                    for (int x = 0; x < size; x++) {
                        for (int z = 0; z < size; z++) {
                            int worldX = centerX - radius + x;
                            int worldZ = centerZ - radius + z;
                            
                            // Safe Block fetching
                            Block topBlock = mainWorld.getHighestBlockAt(worldX, worldZ);
                            Color color = getBlockColor(topBlock.getType().name());
                            mapImage.setRGB(x, z, color.getRGB());
                        }
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(mapImage, "png", baos);
                    String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

                    sendJson(exchange, "{\"image\":\"" + base64Image + "\"}");
                } catch (Exception e) {
                    plugin.getLogger().severe("Map Render Error: " + e.getMessage());
                    try {
                        sendJson(exchange, "{\"image\":\"\"}");
                    } catch (IOException ignored) {}
                }
            });
        }
    }

    private Color getBlockColor(String materialName) {
        if (materialName.contains("GRASS")) return new Color(86, 173, 76);
        if (materialName.contains("WATER")) return new Color(52, 114, 222);
        if (materialName.contains("SAND")) return new Color(219, 211, 160);
        if (materialName.contains("STONE") || materialName.contains("DEEPSLATE")) return new Color(128, 128, 128);
        if (materialName.contains("LEAVES") || materialName.contains("LOG") || materialName.contains("WOOD")) return new Color(45, 107, 34);
        if (materialName.contains("DIRT")) return new Color(134, 96, 67);
        if (materialName.contains("SNOW") || materialName.contains("ICE")) return new Color(240, 248, 255);
        if (materialName.contains("LAVA")) return new Color(237, 85, 23);
        return new Color(30, 32, 40);
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void send204(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getQueryParam(String query, String key) {
        return getQueryParam(query, key, null);
    }

    private String getQueryParam(String query, String key, String defaultValue) {
        if (query == null) return defaultValue;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1 && pair[0].equalsIgnoreCase(key)) return pair[1];
        }
        return defaultValue;
    }
}
