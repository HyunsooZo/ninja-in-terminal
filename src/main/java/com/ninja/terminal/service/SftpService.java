package com.ninja.terminal.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.model.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SftpService {

    private static final Logger log = LoggerFactory.getLogger(SftpService.class);

    private Session session;
    private ChannelSftp sftpChannel;
    private String currentPath = "/";

    /**
     * Connect to SFTP using an existing SSH session
     */
    public void connect(Session session) throws Exception {
        this.session = session;

        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        // Get initial path
        currentPath = sftpChannel.pwd();
        log.info("SFTP connected, current path: {}", currentPath);
    }

    /**
     * List files in the current directory
     */
    public List<RemoteFile> listFiles() throws SftpException {
        return listFiles(currentPath);
    }

    /**
     * List files in a specific directory
     */
    @SuppressWarnings("unchecked")
    public List<RemoteFile> listFiles(String path) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        List<RemoteFile> files = new ArrayList<>();
        Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(path);

        for (ChannelSftp.LsEntry entry : entries) {
            String filename = entry.getFilename();

            // Skip . and ..
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }

            com.jcraft.jsch.SftpATTRS attrs = entry.getAttrs();

            RemoteFile file = new RemoteFile(
                filename,
                path,
                attrs.getSize(),
                attrs.getPermissions(),
                new java.util.Date((long) attrs.getMTime() * 1000),
                attrs.isDir()
            );

            files.add(file);
        }

        // Sort: directories first, then files, alphabetically
        files.sort((f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getFilename().compareToIgnoreCase(f2.getFilename());
        });

        return files;
    }

    /**
     * Change current directory
     */
    public void changeDirectory(String path) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.cd(path);
        currentPath = sftpChannel.pwd();
        log.info("Changed directory to: {}", currentPath);
    }

    /**
     * Go to parent directory
     */
    public void goToParentDirectory() throws SftpException {
        if (currentPath.equals("/")) {
            return;
        }

        String parent = currentPath.substring(0, currentPath.lastIndexOf('/'));
        if (parent.isEmpty()) {
            parent = "/";
        }

        changeDirectory(parent);
    }

    /**
     * Download a file
     */
    public void downloadFile(String remotePath, String localPath) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.get(remotePath, localPath);
        log.info("Downloaded: {} -> {}", remotePath, localPath);
    }

    /**
     * Download a file with progress monitoring
     */
    public void downloadFile(String remotePath, String localPath, ProgressMonitor monitor) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.get(remotePath, localPath, monitor);
        log.info("Downloaded: {} -> {}", remotePath, localPath);
    }

    /**
     * Upload a file
     */
    public void uploadFile(String localPath, String remotePath) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.put(localPath, remotePath);
        log.info("Uploaded: {} -> {}", localPath, remotePath);
    }

    /**
     * Upload a file with progress monitoring
     */
    public void uploadFile(String localPath, String remotePath, ProgressMonitor monitor) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.put(localPath, remotePath, monitor);
        log.info("Uploaded: {} -> {}", localPath, remotePath);
    }

    /**
     * Delete a file
     */
    public void deleteFile(String path) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.rm(path);
        log.info("Deleted file: {}", path);
    }

    /**
     * Delete a directory (must be empty)
     */
    public void deleteDirectory(String path) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.rmdir(path);
        log.info("Deleted directory: {}", path);
    }

    /**
     * Create a new directory
     */
    public void createDirectory(String path) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.mkdir(path);
        log.info("Created directory: {}", path);
    }

    /**
     * Rename a file or directory
     */
    public void rename(String oldPath, String newPath) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.rename(oldPath, newPath);
        log.info("Renamed: {} -> {}", oldPath, newPath);
    }

    /**
     * Change permissions
     */
    public void chmod(String path, int permissions) throws SftpException {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("SFTP channel is not connected");
        }

        sftpChannel.chmod(permissions, path);
        log.info("Changed permissions of {}: {}", path, Integer.toOctalString(permissions));
    }

    /**
     * Get current working directory
     */
    public String getCurrentPath() {
        return currentPath;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return sftpChannel != null && sftpChannel.isConnected();
    }

    /**
     * Disconnect
     */
    public void disconnect() {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
            log.info("SFTP disconnected");
        }
    }

    /**
     * Progress monitor interface for file transfers
     */
    public interface ProgressMonitor extends com.jcraft.jsch.SftpProgressMonitor {
        @Override
        void init(int op, String src, String dest, long max);

        @Override
        boolean count(long count);

        @Override
        void end();
    }
}
