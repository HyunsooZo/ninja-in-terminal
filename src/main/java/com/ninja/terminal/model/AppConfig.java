package com.ninja.terminal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    
    private List<HostInfo> hosts;
    private List<HostGroup> groups;
    private Settings settings;
    
    public AppConfig() {
        this.hosts = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.settings = new Settings();
    }

    public List<HostInfo> getHosts() { return hosts; }
    public void setHosts(List<HostInfo> hosts) { this.hosts = hosts; }

    public List<HostGroup> getGroups() { return groups; }
    public void setGroups(List<HostGroup> groups) { this.groups = groups; }

    public Settings getSettings() { return settings; }
    public void setSettings(Settings settings) { this.settings = settings; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Settings {
        private String fontFamily = "JetBrains Mono";
        private int fontSize = 14;
        private String theme = "dark";
        private int defaultPort = 22;
        private int connectionTimeout = 30000;
        private int scrollBufferSize = 10000;
        
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public int getDefaultPort() { return defaultPort; }
        public void setDefaultPort(int defaultPort) { this.defaultPort = defaultPort; }

        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

        public int getScrollBufferSize() { return scrollBufferSize; }
        public void setScrollBufferSize(int scrollBufferSize) { this.scrollBufferSize = scrollBufferSize; }
    }
}
