package com.dailymark.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dailymark")
public class StorageProperties {
    private String apiKey;
    private String dataDir;

    @PostConstruct
    void validate() throws Exception {
        if (apiKey == null || apiKey.length() < 32) {
            throw new IllegalStateException("DAILYMARK_API_KEY must be set and contain at least 32 characters");
        }
        Files.createDirectories(dataPath());
        Files.createDirectories(docsPath());
        Files.createDirectories(assetsPath());
        Files.createDirectories(dataPath().resolve("db"));
    }

    public Path dataPath() { return Paths.get(dataDir).toAbsolutePath().normalize(); }
    public Path docsPath() { return dataPath().resolve("docs"); }
    public Path assetsPath() { return dataPath().resolve("assets"); }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
}
