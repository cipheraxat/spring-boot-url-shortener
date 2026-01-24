package com.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARS.length();

    public String encode(long value) {
        if (value == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_CHARS.charAt((int) (value % BASE)));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    public long decode(String str) {
        long result = 0;
        for (char c : str.toCharArray()) {
            result = result * BASE + BASE62_CHARS.indexOf(c);
        }
        return result;
    }

    public String encodeToLength(long value, int length) {
        String encoded = encode(value);
        while (encoded.length() < length) {
            encoded = "0" + encoded;
        }
        return encoded;
    }
}