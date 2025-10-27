package com.redteam.labs.workorder.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class FileValidationUtil {

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "gif", "png", "pdf", "csv", "txt", "xlsm", "xlsx", "zip"
    );

    // File signatures for validation
    private static final String GIF_SIGNATURE = "GIF8";

    /**
     * Validates the file extension.
     *
     * @param fileName The name of the file.
     * @return True if the file extension is allowed, false otherwise.
     */
    public static boolean validateFileExtension(String fileName) {
        String fileExtension = getFileExtension(fileName);
        if (fileExtension == null) {
            return false; // No extension found
        }
        return ALLOWED_EXTENSIONS.contains(fileExtension);
    }

    public static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName.substring(lastIndex + 1).toLowerCase();
    }

    /**
     * Validates the file content based on its type.
     *
     * @param fileName The name of the file.
     * @param inputStream The input stream of the file.
     * @return True if the file content is valid, false otherwise.
     * @throws IOException If an I/O error occurs.
     */
    public static boolean validateFileContent(String fileName, InputStream inputStream) throws IOException {
        String fileExtension = getFileExtension(fileName);
        if (fileExtension == null) {
            return false; // No extension found
        }
        
        byte[] signature = null;
        switch(fileExtension) {
            case "jpg":
            case "jpeg":
                signature = new byte[3];
                if (inputStream.read(signature) != 3) {
                    return false; // Unable to read signature
                }
                return signature[0] == (byte) 0xFF && signature[1] == (byte) 0xD8 && signature[2] == (byte) 0xFF;
            case "gif":
                signature = new byte[4];
                if (inputStream.read(signature) != 4) {
                    return false; // Unable to read signature
                }
                String fileSignature = new String(signature);
                return GIF_SIGNATURE.equals(fileSignature);
            case "png":
                signature = new byte[8];
                if (inputStream.read(signature) != 8) {
                    return false; // Unable to read signature
                }
                return signature[0] == (byte) 0x89 && signature[1] == (byte) 0x50 &&
                       signature[2] == (byte) 0x4E && signature[3] == (byte) 0x47 &&
                       signature[4] == (byte) 0x0D && signature[5] == (byte) 0x0A &&
                       signature[6] == (byte) 0x1A && signature[7] == (byte) 0x0A;
            case "pdf":
                signature = new byte[4];
                if (inputStream.read(signature) != 4) {
                    return false; // Unable to read signature
                }
                return signature[0] == (byte) 0x25 && signature[1] == (byte) 0x50 &&
                       signature[2] == (byte) 0x44 && signature[3] == (byte) 0x46;
            case "csv":
                // For CSV files, we can check for consistent field count
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    int expectedFieldCount = -1;

                    while ((line = reader.readLine()) != null) {
                        String[] fields = line.split(",");
                        if (expectedFieldCount == -1) {
                            expectedFieldCount = fields.length; // Set the expected field count based on the first row
                        } else if (fields.length != expectedFieldCount) {
                            return false; // Row does not match the expected field count
                        }
                    }
                }
                return true; // File content is valid
            case "txt":
                // For text files, we can check for non-printable characters
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.matches("\\A\\p{Print}*\\z")) { // Check for printable characters
                            return false; // Contains non-printable characters
                        }
                    }
                }
                return true; // File content is valid
            case "xlsm":
            case "xlsx":
                signature = new byte[4];
                if (inputStream.read(signature) != 4) {
                    return false; // Unable to read signature
                }
                return signature[0] == (byte) 0x50 && signature[1] == (byte) 0x4B &&
                       signature[2] == (byte) 0x03 && signature[3] == (byte) 0x04;
            case "zip":
                return true;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileExtension);
        }
    }
}