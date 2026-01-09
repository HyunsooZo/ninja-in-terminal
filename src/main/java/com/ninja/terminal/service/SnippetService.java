package com.ninja.terminal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ninja.terminal.model.SnippetInfo;
import com.ninja.terminal.model.SnippetPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SnippetService {

    private static final Logger log = LoggerFactory.getLogger(SnippetService.class);
    private static SnippetService instance;

    private final ObjectMapper objectMapper;
    private final Path configDir;
    private final Path snippetsFile;
    private final Path packagesFile;

    private List<SnippetInfo> snippets;
    private List<SnippetPackage> packages;

    private SnippetService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        String homeDir = System.getProperty("user.home");
        configDir = Paths.get(homeDir, ".ninja-terminal");
        snippetsFile = configDir.resolve("snippets.json");
        packagesFile = configDir.resolve("snippet-packages.json");

        loadSnippets();
        loadPackages();
    }

    public static synchronized SnippetService getInstance() {
        if (instance == null) {
            instance = new SnippetService();
        }
        return instance;
    }

    private void loadSnippets() {
        try {
            if (Files.exists(snippetsFile)) {
                snippets = objectMapper.readValue(snippetsFile.toFile(), new TypeReference<List<SnippetInfo>>() {});
            } else {
                snippets = new ArrayList<>();
                saveSnippets();
            }
        } catch (IOException e) {
            log.error("Failed to load snippets", e);
            snippets = new ArrayList<>();
        }
    }

    private void saveSnippets() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(snippetsFile.toFile(), snippets);
        } catch (IOException e) {
            log.error("Failed to save snippets", e);
        }
    }

    private void loadPackages() {
        try {
            if (Files.exists(packagesFile)) {
                packages = objectMapper.readValue(packagesFile.toFile(), new TypeReference<List<SnippetPackage>>() {});
            } else {
                packages = new ArrayList<>();
                savePackages();
            }
        } catch (IOException e) {
            log.error("Failed to load snippet packages", e);
            packages = new ArrayList<>();
        }
    }

    private void savePackages() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(packagesFile.toFile(), packages);
        } catch (IOException e) {
            log.error("Failed to save snippet packages", e);
        }
    }

    public List<SnippetInfo> getSnippets() {
        return new ArrayList<>(snippets);
    }

    public List<SnippetPackage> getPackages() {
        return new ArrayList<>(packages);
    }

    public Optional<SnippetInfo> getSnippetById(String id) {
        return snippets.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public Optional<SnippetPackage> getPackageById(String id) {
        return packages.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public List<SnippetInfo> getSnippetsByPackage(String packageId) {
        return snippets.stream().filter(s -> packageId.equals(s.getPackageId())).toList();
    }

    public void addSnippet(SnippetInfo snippet) {
        snippets.add(snippet);
        saveSnippets();
    }

    public void updateSnippet(SnippetInfo snippet) {
        for (int i = 0; i < snippets.size(); i++) {
            if (snippets.get(i).getId().equals(snippet.getId())) {
                snippets.set(i, snippet);
                break;
            }
        }
        saveSnippets();
    }

    public void deleteSnippet(String id) {
        snippets.removeIf(s -> s.getId().equals(id));
        saveSnippets();
    }

    public void addPackage(SnippetPackage pkg) {
        packages.add(pkg);
        savePackages();
    }

    public void updatePackage(SnippetPackage pkg) {
        for (int i = 0; i < packages.size(); i++) {
            if (packages.get(i).getId().equals(pkg.getId())) {
                packages.set(i, pkg);
                break;
            }
        }
        savePackages();
    }

    public void deletePackage(String id) {
        packages.removeIf(p -> p.getId().equals(id));
        snippets.forEach(s -> {
            if (id.equals(s.getPackageId())) {
                s.setPackageId(null);
            }
        });
        savePackages();
        saveSnippets();
    }
}
