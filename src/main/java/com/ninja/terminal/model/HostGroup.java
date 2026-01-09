package com.ninja.terminal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroup {
    
    private String id;
    private String name;
    private String parentId;
    private String icon;
    
    public HostGroup() {
        this.id = UUID.randomUUID().toString();
    }
    
    public HostGroup(String name) {
        this();
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public String toString() {
        return name;
    }
}
