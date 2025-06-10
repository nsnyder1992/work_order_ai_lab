package com.redteam.labs.workorder.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public class FileUploadScanner {

    // Suspicious content patterns (JSP/Webshell/Script)
    private static final Pattern[] fileContentPatterns = {
        Pattern.compile("(?i)<%.*Runtime\\.getRuntime\\(\\).*%>"),
        Pattern.compile("(?i)request\\.getParameter\\("),
        Pattern.compile("(?i)<%.*ProcessBuilder\\("),
        Pattern.compile("(?i)<jsp:(include|forward).*?page\\s*=.*?>"),
        Pattern.compile("(?i)java\\.lang\\.Runtime"),
        Pattern.compile("(?i)<%=.*%>"),
        Pattern.compile("(?i)eval\\("),
        Pattern.compile("(?i)System\\.exit\\("),
        Pattern.compile("(?i)ObjectInputStream|ObjectOutputStream") // RCE payloads
    };

    // Suspicious file extensions
    private static final Set<String> dangerousExtensions = new HashSet<>(Arrays.asList("jsp", "php", "asp", "aspx", "exe", "bat", "sh", "cmd"));

    public static boolean isMaliciousFile(File file) {
        try {
            String extension = getExtension(file.getName());
            if (dangerousExtensions.contains(extension)) {
                System.out.println("[⚠] Suspicious file extension: " + extension);
                return true;
            }

            String content = readFileContent(file);
            for (Pattern pattern : fileContentPatterns) {
                if (pattern.matcher(content).find()) {
                    System.out.println("[🚨] Malicious pattern found in: " + file.getName());
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("[!] Could not scan file: " + file.getName());
        }
        return false;
    }

    public static String readFileContent(File file) throws IOException {
        Path path = file.toPath();
        return String.join("\n", Files.readAllLines(path));
    }
    
    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? "" : filename.substring(dot + 1).toLowerCase();
    }

    public static void main(String[] args) {
        File testFile = new File("uploads/suspicious.jsp");
        boolean result = isMaliciousFile(testFile);
        System.out.println("Malicious? " + result);
    }
}