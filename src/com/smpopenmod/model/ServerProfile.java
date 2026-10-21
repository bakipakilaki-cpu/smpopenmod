package com.smpopenmod.model;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a managed Minecraft server instance.
 */
public class ServerProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String directory;
    private String jarName;
    private int ramGb;
    private int port;
    private boolean playitEnabled;
    private boolean autoRestart;
    private long createdAt;

    public ServerProfile() {
        this.id = UUID.randomUUID().toString();
        this.name = "My Minecraft Server";
        this.directory = "";
        this.jarName = "server.jar";
        this.ramGb = 4;
        this.port = 25565;
        this.playitEnabled = true;
        this.autoRestart = false;
        this.createdAt = System.currentTimeMillis();
    }

    public ServerProfile(String name, String directory, String jarName, int ramGb, int port) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.directory = directory;
        this.jarName = jarName;
        this.ramGb = ramGb;
        this.port = port;
        this.playitEnabled = true;
        this.autoRestart = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }

    public String getJarName() { return jarName; }
    public void setJarName(String jarName) { this.jarName = jarName; }

    public int getRamGb() { return ramGb; }
    public void setRamGb(int ramGb) { this.ramGb = ramGb; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isPlayitEnabled() { return playitEnabled; }
    public void setPlayitEnabled(boolean playitEnabled) { this.playitEnabled = playitEnabled; }

    public boolean isAutoRestart() { return autoRestart; }
    public void setAutoRestart(boolean autoRestart) { this.autoRestart = autoRestart; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public File getDirectoryFile() {
        return new File(directory);
    }

    public File getJarFile() {
        return new File(directory, jarName);
    }

    public boolean existsOnDisk() {
        return directory != null && new File(directory).isDirectory();
    }

    public boolean hasJar() {
        return getJarFile().isFile();
    }

    @Override
    public String toString() {
        return name + " (" + directory + ")";
    }
}
