package com.redteam.labs.workorder.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayloadDecoderUtils {

    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    public static String doubleUrlDecode(String input) {
        return urlDecode(urlDecode(input));
    }

    public static String base64Decode(String input) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(input);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            return input; // Not valid Base64
        }
    }

    public static String hexDecode(String input) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.length(); i += 2) {
                String hex = input.substring(i, Math.min(i + 2, input.length()));
                sb.append((char) Integer.parseInt(hex, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }

    public static String unicodeDecode(String input) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String unicodeChar = matcher.group(1);
            char ch = (char) Integer.parseInt(unicodeChar, 16);
            matcher.appendReplacement(sb, Character.toString(ch));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String htmlEntityDecode(String input) {
        input = decodeHexHtmlEntity(input); // Decode hex entities first
        input = decodeDecimalHtmlEntity(input); // Then decode decimal entities
        return input
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&")
            .replaceAll("&quot;", "\"");
        
        
    }
    
    public static String decodeDecimalHtmlEntity(String input) {
        Pattern pattern = Pattern.compile("&#(\\d+);");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(sb, Character.toString((char) code));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    public static String decodeHexHtmlEntity(String input) {
        Pattern pattern = Pattern.compile("&#x([0-9a-fA-F]+);");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Character.toString((char) code));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String rot13(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                result.append((char) ((c - 'a' + 13) % 26 + 'a'));
            } else if (c >= 'A' && c <= 'Z') {
                result.append((char) ((c - 'A' + 13) % 26 + 'A'));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String decodeChain(String input) {
        // Run all decoders in a sequence for chained decoding
        String decoded = input;
        decoded = doubleUrlDecode(decoded);
        decoded = base64Decode(decoded);
        decoded = hexDecode(decoded);
        decoded = unicodeDecode(decoded);
        decoded = htmlEntityDecode(decoded);
        decoded = rot13(decoded);
        return decoded;
    }

    public static void main(String[] args) {
        String test = "%2575%2573%2572%252e%256a%2573%2570"; // Encoded twice
        System.out.println("Decoded chain: " + decodeChain(test));
    }
}