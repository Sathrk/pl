package com.survival.webdashboard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimManager {

    public static class ClaimItem {
        public String item;
        public int amount;

        public ClaimItem(String item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }

    private final Map<String, List<ClaimItem>> pendingClaims = new HashMap<>();

    public void addClaim(String username, String item, int amount) {
        pendingClaims.putIfAbsent(username.toLowerCase(), new ArrayList<>());
        pendingClaims.get(username.toLowerCase()).add(new ClaimItem(item, amount));
    }

    public boolean processClaims(Player player) {
        String name = player.getName().toLowerCase();
        if (!pendingClaims.containsKey(name) || pendingClaims.get(name).isEmpty()) {
            return false;
        }

        List<ClaimItem> items = pendingClaims.get(name);
        for (ClaimItem ci : items) {
            Material mat = Material.matchMaterial(ci.item);
            if (mat != null) {
                player.getInventory().addItem(new ItemStack(mat, ci.amount));
            }
        }
        pendingClaims.remove(name);
        return true;
    }
}
