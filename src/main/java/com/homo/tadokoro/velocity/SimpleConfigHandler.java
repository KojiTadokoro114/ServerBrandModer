package com.homo.tadokoro.velocity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class SimpleConfigHandler {

    private final File configFile;
    private final Map<String, String> configMap = new HashMap<>();

    public SimpleConfigHandler(File configFile) {
        this.configFile = new File(configFile, "config.yml");
        saveDefaultConfig();
        reloadConfig();
    }

    public void saveDefaultConfig() {
        if (configFile.exists()) {
            return;
        }
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream in = getClass().getResourceAsStream("/config.yml");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Files.write(configFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        configMap.clear();
        try {
            Files.readAllLines(configFile.toPath()).forEach(str -> {
                String[] line = str.split(": ");
                configMap.put(line[0], cite(line[1], false));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String get(String key) {
        return configMap.get(key);
    }

    public void set(String key, String value) {
        configMap.put(key, value);
    }

    public void saveConfig() {
        try {
            Files.newBufferedWriter(configFile.toPath() , StandardOpenOption.TRUNCATE_EXISTING);
            configMap.forEach((key, value) -> {
                try {
                    Files.write(configFile.toPath(), (key + ": " + cite(value, true)).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String cite(String origin, boolean shouldCite) {
        if (shouldCite) {
            return "'" + origin + "'";
        } else {
            return origin.substring(1, origin.length() - 1);
        }
    }
}
