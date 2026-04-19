package com.chitchat.app.util;

public final class FileUtil {

    private FileUtil() {}

    public static String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public static String contentDisposition(String filename) {
        String safe = filename != null ? filename : "file";
        return "attachment; filename=\"" + safe + "\"";
    }
}
