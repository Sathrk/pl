package com.survival.webdashboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PasscodeManager {
    private final Map<String, String> playerPasscodes = new HashMap<>(); // Player -> Code
    private final Map<String, String> codeToPlayer = new HashMap<>();   // Code -> Player

    public String generatePasscode(String username) {
        // Purana code remove karein agar pehle se exist karta ho
        if (playerPasscodes.containsKey(username)) {
            codeToPlayer.remove(playerPasscodes.get(username));
        }

        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));
        
        playerPasscodes.put(username, code);
        codeToPlayer.put(code, username);
        return code;
    }

    public String verifyCode(String code) {
        return codeToPlayer.get(code); // Returns Username if valid, else null
    }
}
