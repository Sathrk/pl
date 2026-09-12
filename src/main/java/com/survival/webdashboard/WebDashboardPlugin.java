package com.survival.webdashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WebDashboardPlugin extends JavaPlugin implements CommandExecutor {

    private HttpServer httpServer;
    private static final int PORT = 12935;

    @Override
    public void onEnable() {
        // HTTP Web Server Start
        startWebServer();

        // Register /web Command Listener
        if (this.getCommand("web") != null) {
            this.getCommand("web").setExecutor(this);
        }

        getLogger().info("WebDashboard API Plugin successfully enabled on port " + PORT);
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        getLogger().info("WebDashboard API Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("web")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("login")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage("§a[WebDashboard] Login successful! Welcome " + player.getName());
                } else {
                    sender.sendMessage("Sirf in-game players is command ko run kar sakte hain.");
                }
                return true;
            }
        }
        return false;
    }

    private void startWebServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);

            httpServer.createContext("/api/map", new MapDataHandler());
            httpServer.createContext("/api/map-image", new MapImageHandler());

            httpServer.setExecutor(null);
            httpServer.start();
        } catch (IOException e) {
            getLogger().severe("Failed to start Web API HTTP Server: " + e.getMessage());
        }
    }

    // --- API 1: Online Players Coordinates ---
    private class MapDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

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

            sendJsonResponse(exchange, json.toString());
        }
    }

    // --- API 2: Real-time 2D Terrain Base64 Renderer ---
    private class MapImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Sync Thread execution for safely scanning Bukkit Blocks
            Bukkit.getScheduler().runTask(WebDashboardPlugin.this, () -> {
                try {
                    World mainWorld = Bukkit.getWorlds().get(0);
                    int centerX = 0;
                    int centerZ = 0;
                    int radius = 100;

                    String base64Image = renderWorldTerrainBase64(mainWorld, centerX, centerZ, radius);
                    String json = "{\"image\":\"" + base64Image + "\"}";

                    sendJsonResponse(exchange, json);
                } catch (Exception e) {
                    getLogger().severe("Map render error: " + e.getMessage());
                }
            });
        }
    }

    private String renderWorldTerrainBase64(World world, int centerX, int centerZ, int radius) {
        int size = radius * 2;
        BufferedImage mapImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int worldX = centerX - radius + x;
                int worldZ = centerZ - radius + z;

                Block topBlock = world.getHighestBlockAt(worldX, worldZ);
                Color color = getBlockColor(topBlock.getType().name());

                mapImage.setRGB(x, z, color.getRGB());
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(mapImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            getLogger().severe("Error encoding map image: " + e.getMessage());
            return "";
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

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJsonResponse(HttpExchange exchange, String responseText) throws IOException {
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
