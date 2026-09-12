package com.yourserver.webbridge;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WebBridgeMain extends JavaPlugin implements CommandExecutor {

    private WebHttpServer webServer;
    private final int PORT = 12935;

    @Override
    public void onEnable() {
        try {
            webServer = new WebHttpServer(PORT);
            getLogger().info("Web Bridge HTTP Server running on port: " + PORT);
        } catch (Exception e) {
            getLogger().severe("Could not start Web Bridge server on port " + PORT + ": " + e.getMessage());
        }

        if (getCommand("web") != null) {
            getCommand("web").setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Is command ko sirf in-game player use kar sakta hai!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("login")) {
            String code = PasscodeManager.generateCode(player.getName());
            player.sendMessage(ChatColor.GREEN + "[WebAuth] " + ChatColor.WHITE + "Aapka Web Dashboard Login Passcode: " 
                + ChatColor.YELLOW + ChatColor.BOLD + code);
            player.sendMessage(ChatColor.GRAY + "Yeh code agle 10 minutes tak valid hai.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /web login");
        return true;
    }
}


   @Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage("Is command ko sirf in-game player use kar sakta hai!");
        return true;
    }

    Player player = (Player) sender;

    if (command.getName().equalsIgnoreCase("web") && args.length > 0 && args[0].equalsIgnoreCase("login")) {
        String code = PasscodeManager.generateCode(player.getName());
        player.sendMessage(ChatColor.GREEN + "[WebAuth] " + ChatColor.WHITE + "Aapka Web Login Passcode: " 
            + ChatColor.YELLOW + ChatColor.BOLD + code);
        return true;
    }

    if (command.getName().equalsIgnoreCase("claim")) {
        List<ClaimManager.ClaimItem> pendingClaims = ClaimManager.getClaims(player.getName());

        if (pendingClaims.isEmpty()) {
            player.sendMessage(ChatColor.RED + "[WebClaim] Aapke paas koi pending claim rewards nahi hain!");
            return true;
        }

        // Inventory space check
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "[WebClaim] Aapki inventory full hai! Space khali karke dubara try karein.");
            return true;
        }

        int claimedCount = 0;
        for (ClaimManager.ClaimItem claim : pendingClaims) {
            ItemStack stack = new ItemStack(claim.getMaterial(), claim.getAmount());
            player.getInventory().addItem(stack);
            claimedCount++;
        }

        ClaimManager.clearClaims(player.getName());
        player.sendMessage(ChatColor.GREEN + "[WebClaim] Successfully claimed " + claimedCount + " item stack(s)!");
        return true;
    }

    return false;
}
