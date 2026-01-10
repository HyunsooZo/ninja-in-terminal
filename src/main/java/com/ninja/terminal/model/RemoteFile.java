package com.ninja.terminal.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class RemoteFile {

    private String filename;
    private String path;
    private long size;
    private int permissions;
    private LocalDateTime modifiedTime;
    private boolean isDirectory;
    private String owner;
    private String group;

    public RemoteFile(String filename, String path, long size, int permissions,
                      LocalDateTime modifiedTime, boolean isDirectory) {
        this.filename = filename;
        this.path = path;
        this.size = size;
        this.permissions = permissions;
        this.modifiedTime = modifiedTime;
        this.isDirectory = isDirectory;
    }

    public RemoteFile(String filename, String path, long size, int permissions,
                      Date modifiedTime, boolean isDirectory) {
        this(filename, path, size, permissions,
             modifiedTime != null ? LocalDateTime.ofInstant(modifiedTime.toInstant(), ZoneId.systemDefault()) : null,
             isDirectory);
    }

    // Getters and Setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public int getPermissions() { return permissions; }
    public void setPermissions(int permissions) { this.permissions = permissions; }

    public LocalDateTime getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(LocalDateTime modifiedTime) { this.modifiedTime = modifiedTime; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    /**
     * Get full path including filename
     */
    public String getFullPath() {
        if (path.endsWith("/")) {
            return path + filename;
        }
        return path + "/" + filename;
    }

    /**
     * Get human-readable file size
     */
    public String getFormattedSize() {
        if (isDirectory) {
            return "-";
        }

        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Get Unix-style permission string (e.g., "rwxr-xr-x")
     */
    public String getPermissionString() {
        StringBuilder sb = new StringBuilder();

        // Owner permissions
        sb.append((permissions & 0400) != 0 ? 'r' : '-');
        sb.append((permissions & 0200) != 0 ? 'w' : '-');
        sb.append((permissions & 0100) != 0 ? 'x' : '-');

        // Group permissions
        sb.append((permissions & 0040) != 0 ? 'r' : '-');
        sb.append((permissions & 0020) != 0 ? 'w' : '-');
        sb.append((permissions & 0010) != 0 ? 'x' : '-');

        // Other permissions
        sb.append((permissions & 0004) != 0 ? 'r' : '-');
        sb.append((permissions & 0002) != 0 ? 'w' : '-');
        sb.append((permissions & 0001) != 0 ? 'x' : '-');

        return sb.toString();
    }

    /**
     * Get file type icon name for CSS styling
     */
    public String getFileTypeIcon() {
        if (isDirectory) {
            return "icon-folder";
        }

        String extension = getFileExtension();
        switch (extension) {
            case "txt", "log", "md" -> {
                return "icon-file-text";
            }
            case "java", "js", "py", "c", "cpp", "h" -> {
                return "icon-file-code";
            }
            case "jpg", "jpeg", "png", "gif", "bmp" -> {
                return "icon-file-image";
            }
            case "zip", "tar", "gz", "rar", "7z" -> {
                return "icon-file-archive";
            }
            default -> {
                return "icon-file";
            }
        }
    }

    /**
     * Get file extension
     */
    public String getFileExtension() {
        if (isDirectory || filename == null) {
            return "";
        }

        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    @Override
    public String toString() {
        return filename;
    }
}
