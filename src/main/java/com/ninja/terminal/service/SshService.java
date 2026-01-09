package com.ninja.terminal.service;

import com.jcraft.jsch.*;
import com.ninja.terminal.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class SshService {
    
    private static final Logger log = LoggerFactory.getLogger(SshService.class);
    
    private final JSch jsch;
    private Session session;
    private ChannelShell channel;
    
    public SshService() {
        this.jsch = new JSch();
    }
    
    public void connect(HostInfo hostInfo) throws JSchException {
        log.info("Connecting to {}@{}:{}", hostInfo.getUsername(), hostInfo.getHostname(), hostInfo.getPort());
        
        // Set up authentication
        if (hostInfo.getAuthType() == HostInfo.AuthType.KEY) {
            if (hostInfo.getPassphrase() != null && !hostInfo.getPassphrase().isEmpty()) {
                jsch.addIdentity(hostInfo.getPrivateKeyPath(), hostInfo.getPassphrase());
            } else {
                jsch.addIdentity(hostInfo.getPrivateKeyPath());
            }
        }
        
        session = jsch.getSession(hostInfo.getUsername(), hostInfo.getHostname(), hostInfo.getPort());
        
        if (hostInfo.getAuthType() == HostInfo.AuthType.PASSWORD) {
            session.setPassword(hostInfo.getPassword());
        }
        
        // Skip host key checking (for simplicity - in production you'd want to handle this properly)
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        
        int timeout = ConfigService.getInstance().getSettings().getConnectionTimeout();
        session.connect(timeout);
        
        log.info("Connected to {}", hostInfo.getHostname());
    }
    
    public ChannelShell openShell() throws JSchException {
        if (session == null || !session.isConnected()) {
            throw new JSchException("Not connected");
        }
        
        channel = (ChannelShell) session.openChannel("shell");
        channel.setPtyType("xterm-256color");
        channel.setPtySize(120, 40, 1920, 1080);
        
        return channel;
    }
    
    public void resize(int cols, int rows) {
        if (channel != null && channel.isConnected()) {
            channel.setPtySize(cols, rows, cols * 8, rows * 16);
        }
    }
    
    public InputStream getInputStream() throws java.io.IOException {
        if (channel == null) return null;
        return channel.getInputStream();
    }
    
    public OutputStream getOutputStream() throws java.io.IOException {
        if (channel == null) return null;
        return channel.getOutputStream();
    }
    
    public void disconnect() {
        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
        log.info("Disconnected");
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
}
