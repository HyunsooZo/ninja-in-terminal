package com.ninja.terminal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostInfo {
    
    private String id;
    private String name;
    private String hostname;
    private int port;
    private String username;
    private String password;
    private String privateKeyPath;
    private String passphrase;
    private String groupId;
    private AuthType authType;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnectedAt;
    
    public enum AuthType {
        PASSWORD, KEY
    }
    
    public HostInfo() {
        this.id = UUID.randomUUID().toString();
        this.port = 22;
        this.authType = AuthType.PASSWORD;
        this.createdAt = LocalDateTime.now();
    }
    
    public HostInfo(String name, String hostname, String username) {
        this();
        this.name = name;
        this.hostname = hostname;
        this.username = username;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public String getPassphrase() { return passphrase; }
    public void setPassphrase(String passphrase) { this.passphrase = passphrase; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastConnectedAt() { return lastConnectedAt; }
    public void setLastConnectedAt(LocalDateTime lastConnectedAt) { this.lastConnectedAt = lastConnectedAt; }

    @Override
    public String toString() {
        return name != null ? name : hostname;
    }
}
