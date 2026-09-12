package com.yourserver.webbridge;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PasscodeManager {

    private static class AuthData {
        String username;
        long expiryTime;

        AuthData(String username, long expiryTime) {
            this.username = username;
            this.expiryTime = expiryTime;
        }
    }

    private static final Map<String, AuthData> codeStorage = new ConcurrentHashMap<>();

    public static String generateCode(String username) {
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000); // 10 Minutes Expiry

        codeStorage.put(code, new AuthData(username, expiry));
        return code;
    }

    public static String verifyCode(String code) {
        AuthData data = codeStorage.get(code);
        if (data == null) return null;

        if (System.currentTimeMillis() > data.expiryTime) {
            codeStorage.remove(code);
            return null; // Expired
        }

        codeStorage.remove(code); // One-time use verification
        return data.username;
    }
}
