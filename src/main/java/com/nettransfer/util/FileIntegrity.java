package com.nettransfer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileIntegrity {

    public static String calculateSHA256(File file)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexHash = new StringBuilder();

        for (byte b : hashBytes) {
            hexHash.append(String.format("%02x", b & 0xff));
        }

        return hexHash.toString();
    }
}