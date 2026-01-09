package com.ninja.terminal.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SnippetInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("script")
    private String script;

    @JsonProperty("packageId")
    private String packageId;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public SnippetInfo() {
        this.id = UUID.randomUUID().toString();
        this.tags = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public SnippetInfo(String name, String description, String script, String packageId) {
        this();
        this.name = name;
        this.description = description;
        this.script = script;
        this.packageId = packageId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
