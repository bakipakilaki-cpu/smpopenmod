package com.smpopenmod.storage;

import com.smpopenmod.model.ServerProfile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistent configuration and server storage in ~/.smpopenmod/
 */
public class ConfigManager {
    private static final String APP_DIR_NAME = ".smpopenmod";
    private static final String SERVERS_FILE_NAME = "servers.json";
    private static final File APP_DIR = new File(System.getProperty("user.home"), APP_DIR_NAME);
    private static final File SERVERS_FILE = new File(APP_DIR, SERVERS_FILE_NAME);

    private final List<ServerProfile> servers = new ArrayList<>();
    private String selectedServerId = null;

    public ConfigManager() {
        initAppDir();
        loadServers();
    }

    public static File getAppDir() {
        return APP_DIR;
    }

    private void initAppDir() {
        if (!APP_DIR.exists()) {
            APP_DIR.mkdirs();
        }
        File binDir = new File(APP_DIR, "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
    }

    public synchronized void loadServers() {
        servers.clear();
        if (!SERVERS_FILE.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(SERVERS_FILE), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line.trim());
            }

            parseServersJson(sb.toString());
        } catch (Exception e) {
            System.err.println("[SMPOpenMod] Warning: Could not read servers.json: " + e.getMessage());
        }
    }

    public synchronized void saveServers() {
        initAppDir();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < servers.size(); i++) {
            ServerProfile s = servers.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escape(s.getId())).append("\",\n");
            sb.append("    \"name\": \"").append(escape(s.getName())).append("\",\n");
            sb.append("    \"directory\": \"").append(escape(s.getDirectory())).append("\",\n");
            sb.append("    \"jarName\": \"").append(escape(s.getJarName())).append("\",\n");
            sb.append("    \"ramGb\": ").append(s.getRamGb()).append(",\n");
            sb.append("    \"port\": ").append(s.getPort()).append(",\n");
            sb.append("    \"playitEnabled\": ").append(s.isPlayitEnabled()).append(",\n");
            sb.append("    \"autoRestart\": ").append(s.isAutoRestart()).append(",\n");
            sb.append("    \"createdAt\": ").append(s.getCreatedAt()).append("\n");
            sb.append("  }").append(i < servers.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]\n");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(SERVERS_FILE), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            System.err.println("[SMPOpenMod] Error: Failed to save servers.json: " + e.getMessage());
        }
    }

    private void parseServersJson(String json) {
        if (json == null || json.isEmpty()) return;
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return;

        // Split into objects
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objStr = json.substring(objStart + 1, i);
                    ServerProfile s = parseProfileObject(objStr);
                    if (s != null) {
                        servers.add(s);
                    }
                    objStart = -1;
                }
            }
        }
    }

    private ServerProfile parseProfileObject(String objStr) {
        ServerProfile profile = new ServerProfile();
        String[] pairs = objStr.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length != 2) continue;
            String key = unquote(kv[0].trim());
            String val = kv[1].trim();

            switch (key) {
                case "id":
                    profile.setId(unquote(val));
                    break;
                case "name":
                    profile.setName(unquote(val));
                    break;
                case "directory":
                    profile.setDirectory(unquote(val));
                    break;
                case "jarName":
                    profile.setJarName(unquote(val));
                    break;
                case "ramGb":
                    try { profile.setRamGb(Integer.parseInt(val)); } catch (Exception ignored) {}
                    break;
                case "port":
                    try { profile.setPort(Integer.parseInt(val)); } catch (Exception ignored) {}
                    break;
                case "playitEnabled":
                    profile.setPlayitEnabled(Boolean.parseBoolean(val));
                    break;
                case "autoRestart":
                    profile.setAutoRestart(Boolean.parseBoolean(val));
                    break;
                case "createdAt":
                    try { profile.setCreatedAt(Long.parseLong(val)); } catch (Exception ignored) {}
                    break;
            }
        }
        return profile;
    }

    private String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public synchronized List<ServerProfile> getServers() {
        return new ArrayList<>(servers);
    }

    public synchronized void addServer(ServerProfile profile) {
        servers.removeIf(s -> s.getId().equals(profile.getId()) || s.getDirectory().equals(profile.getDirectory()));
        servers.add(profile);
        saveServers();
    }

    public synchronized void removeServer(String id) {
        servers.removeIf(s -> s.getId().equals(id));
        saveServers();
    }

    public synchronized ServerProfile getServerById(String id) {
        for (ServerProfile s : servers) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public String getSelectedServerId() {
        return selectedServerId;
    }

    public void setSelectedServerId(String selectedServerId) {
        this.selectedServerId = selectedServerId;
    }
}
