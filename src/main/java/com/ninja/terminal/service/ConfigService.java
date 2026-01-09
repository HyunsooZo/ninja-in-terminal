package com.ninja.terminal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ninja.terminal.model.AppConfig;
import com.ninja.terminal.model.HostGroup;
import com.ninja.terminal.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class ConfigService {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_DIR = ".ninja-in-terminal";
    private static final String CONFIG_FILE = "config.json";
    
    private final ObjectMapper objectMapper;
    private final Path configPath;
    private AppConfig config;
    
    private static ConfigService instance;
    
    public static ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }
    
    private ConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, CONFIG_DIR);
        this.configPath = configDir.resolve(CONFIG_FILE);
        
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            log.error("Failed to create config directory", e);
        }
        
        load();
    }
    
    public void load() {
        if (Files.exists(configPath)) {
            try {
                config = objectMapper.readValue(configPath.toFile(), AppConfig.class);
                log.info("Config loaded from {}", configPath);
            } catch (IOException e) {
                log.error("Failed to load config", e);
                config = new AppConfig();
            }
        } else {
            config = new AppConfig();
            save();
        }
    }
    
    public void save() {
        try {
            objectMapper.writeValue(configPath.toFile(), config);
            log.info("Config saved to {}", configPath);
        } catch (IOException e) {
            log.error("Failed to save config", e);
        }
    }
    
    public AppConfig getConfig() {
        return config;
    }
    
    // Host operations
    public List<HostInfo> getHosts() {
        return config.getHosts();
    }
    
    public void addHost(HostInfo host) {
        config.getHosts().add(host);
        save();
    }
    
    public void updateHost(HostInfo host) {
        config.getHosts().removeIf(h -> h.getId().equals(host.getId()));
        config.getHosts().add(host);
        save();
    }
    
    public void deleteHost(String hostId) {
        config.getHosts().removeIf(h -> h.getId().equals(hostId));
        save();
    }
    
    public Optional<HostInfo> getHostById(String id) {
        return config.getHosts().stream()
                .filter(h -> h.getId().equals(id))
                .findFirst();
    }
    
    // Group operations
    public List<HostGroup> getGroups() {
        return config.getGroups();
    }
    
    public void addGroup(HostGroup group) {
        config.getGroups().add(group);
        save();
    }
    
    public void deleteGroup(String groupId) {
        config.getGroups().removeIf(g -> g.getId().equals(groupId));
        // Move hosts in this group to ungrouped
        config.getHosts().stream()
                .filter(h -> groupId.equals(h.getGroupId()))
                .forEach(h -> h.setGroupId(null));
        save();
    }
    
    // Settings
    public AppConfig.Settings getSettings() {
        return config.getSettings();
    }
    
    public void updateSettings(AppConfig.Settings settings) {
        config.setSettings(settings);
        save();
    }
}
