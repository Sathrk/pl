package com.yourserver.webbridge;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    public static class ClaimItem {
        private final Material material;
        private final int amount;

        public ClaimItem(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
    }

    // Player Username -> List of Pending ClaimItems
    private static final Map<String, List<ClaimItem>> claimStorage = new ConcurrentHashMap<>();

    // Web API se claim add karne ke liye
    public static void addClaim(String username, String materialName, int amount) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.DIRT; // Fallback material

        ClaimItem item = new ClaimItem(mat, amount);
        claimStorage.computeIfAbsent(username.toLowerCase(), k -> new ArrayList<>()).add(item);
    }

    public static List<ClaimItem> getClaims(String username) {
        return claimStorage.getOrDefault(username.toLowerCase(), new ArrayList<>());
    }

    public static void clearClaims(String username) {
        claimStorage.remove(username.toLowerCase());
    }
}
