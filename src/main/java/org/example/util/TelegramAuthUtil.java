package org.example.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TelegramAuthUtil {
    private static final String BOT_TOKEN = "8231610553:AAE1wMfbQGMTjw5_pCv5_E4oEzWEUc7KCuc";

    public static boolean validateInitData(String initData) {
        if (initData == null || initData.isEmpty()) {
            return false;
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = initData.split("&");
        String receivedHash = null;

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length != 2) continue;
            String key = keyValue[0];
            String value = keyValue[1];
            if ("hash".equals(key)) {
                receivedHash = value;
            } else {
                params.put(key, value);
            }
        }

        if (receivedHash == null) {
            return false;
        }

        // Сортируем ключи
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        // Формируем data-check-string
        StringBuilder dataCheck = new StringBuilder();
        for (String key : sortedKeys) {
            if (dataCheck.length() > 0) {
                dataCheck.append("\n");
            }
            dataCheck.append(key).append("=").append(params.get(key));
        }

        // Вычисляем HMAC-SHA256 от "WebAppData" + data-check-string
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(BOT_TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(("WebAppData\n" + dataCheck).getBytes(StandardCharsets.UTF_8));
            String computedHash = org.apache.commons.codec.binary.Hex.encodeHexString(hashBytes);

            return computedHash.equalsIgnoreCase(receivedHash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}