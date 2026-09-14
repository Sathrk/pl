package com.yourserver.webbridge.manager;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {
    // Stores username -> List of items to receive
    private static final Map<String, List<ItemStack>> pendingClaims = new ConcurrentHashMap<>();

    public static void addClaim(String username, Material material, int amount) {
        String key = username.toLowerCase();
        pendingClaims.putIfAbsent(key, new ArrayList<>());
        pendingClaims.get(key).add(new ItemStack(material, amount));
    }

    public static List<ItemStack> getAndClearClaims(String username) {
        String key = username.toLowerCase();
        if (pendingClaims.containsKey(key)) {
            return pendingClaims.remove(key);
        }
        return Collections.emptyList();
    }

    public static boolean hasClaims(String username) {
        String key = username.toLowerCase();
        return pendingClaims.containsKey(key) && !pendingClaims.get(key).isEmpty();
    }
}
