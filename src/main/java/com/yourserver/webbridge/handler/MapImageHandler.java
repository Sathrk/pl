package com.yourpackage.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MapImageHandler implements HttpHandler {
    private final JavaPlugin plugin;

    public MapImageHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        int centerX = Integer.parseInt(CorsHelper.getQueryParam(query, "x", "0"));
        int centerZ = Integer.parseInt(CorsHelper.getQueryParam(query, "z", "0"));
        int radius = Integer.parseInt(CorsHelper.getQueryParam(query, "radius", "120"));

        int width = radius * 2;
        int height = (int) (width / (1150.0 / 300.0));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                World mainWorld = Bukkit.getWorlds().get(0);
                BufferedImage mapImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < height; z++) {
                        int worldX = centerX - (width / 2) + x;
                        int worldZ = centerZ - (height / 2) + z;

                        Block topBlock = mainWorld.getHighestBlockAt(worldX, worldZ);
                        mapImage.setRGB(x, z, getBlockColor(topBlock.getType().name()));
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(mapImage, "png", baos);
                String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

                CorsHelper.sendJson(exchange, "{\"image\":\"" + base64Image + "\"}");
            } catch (Exception e) {
                plugin.getLogger().severe("Map Render Error: " + e.getMessage());
                try {
                    CorsHelper.sendJson(exchange, "{\"image\":\"\"}");
                } catch (IOException ignored) {}
            }
        });
    }

    private int getBlockColor(String blockType) {
        if (blockType.contains("GRASS")) return new Color(34, 139, 34).getRGB();
        if (blockType.contains("WATER")) return new Color(30, 144, 255).getRGB();
        if (blockType.contains("SAND")) return new Color(238, 214, 175).getRGB();
        if (blockType.contains("LEAVES")) return new Color(0, 100, 0).getRGB();
        if (blockType.contains("STONE")) return new Color(128, 128, 128).getRGB();
        return new Color(80, 80, 80).getRGB();
    }
}
