package com.smpopenmod.manager;

import com.smpopenmod.model.ServerProfile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Manages the Minecraft server OS process, live console I/O, and health monitoring.
 */
public class ServerProcessManager {
    private final ServerProfile profile;
    private Process process;
    private BufferedWriter processWriter;
    private final List<String> consoleBuffer = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private static final int MAX_BUFFER_LINES = 1000;
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private boolean starting = false;
    private boolean stopping = false;

    public ServerProcessManager(ServerProfile profile) {
        this.profile = profile;
    }

    public ServerProfile getProfile() {
        return profile;
    }

    public synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    public synchronized boolean isStarting() {
        return starting;
    }

    public synchronized boolean isStopping() {
        return stopping;
    }

    public synchronized boolean start() throws IOException {
        if (isRunning() || starting) {
            return false;
        }

        starting = true;
        appendLog("[SMPOpenMod] Initializing server start: " + profile.getName() + "...");

        File dir = profile.getDirectoryFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Auto-accept EULA if missing or false
        ensureEulaAccepted(dir);

        File jarFile = profile.getJarFile();
        if (!jarFile.exists()) {
            starting = false;
            appendLog("[SMPOpenMod] ERROR: Server JAR not found at: " + jarFile.getAbsolutePath());
            return false;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-Xms1G");
        cmd.add("-Xmx" + profile.getRamGb() + "G");
        cmd.add("-XX:+UseG1GC");
        cmd.add("-jar");
        cmd.add(profile.getJarName());
        cmd.add("--nogui");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        pb.redirectErrorStream(true);

        try {
            process = pb.start();
            processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            starting = false;
            appendLog("[SMPOpenMod] Server process launched (PID: " + process.pid() + "). Allocation: " + profile.getRamGb() + "GB RAM.");

            // Read output stream
            ioExecutor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(line);
                    }
                } catch (IOException ignored) {}

                try {
                    int exitCode = process.waitFor();
                    appendLog("[SMPOpenMod] Server stopped with exit code: " + exitCode);
                } catch (InterruptedException ignored) {}

                synchronized (ServerProcessManager.this) {
                    stopping = false;
                    process = null;
                    processWriter = null;
                }
            });

            return true;
        } catch (IOException e) {
            starting = false;
            appendLog("[SMPOpenMod] ERROR launching server: " + e.getMessage());
            throw e;
        }
    }

    public synchronized void stop() {
        if (!isRunning() || stopping) {
            return;
        }
        stopping = true;
        appendLog("[SMPOpenMod] Gracefully stopping server...");
        sendCommand("stop");

        // Schedule forceful termination watchdog
        ioExecutor.submit(() -> {
            try {
                if (process != null && !process.waitFor(20, TimeUnit.SECONDS)) {
                    appendLog("[SMPOpenMod] Server did not stop within 20s. Forcing shutdown...");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                if (process != null) process.destroyForcibly();
            } finally {
                synchronized (ServerProcessManager.this) {
                    stopping = false;
                }
            }
        });
    }

    public synchronized void restart() {
        ioExecutor.submit(() -> {
            if (isRunning()) {
                stop();
                while (isRunning()) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
            try {
                Thread.sleep(1000);
                start();
            } catch (Exception e) {
                appendLog("[SMPOpenMod] Restart failed: " + e.getMessage());
            }
        });
    }

    public synchronized boolean sendCommand(String command) {
        if (!isRunning() || processWriter == null) {
            return false;
        }
        try {
            processWriter.write(command);
            processWriter.newLine();
            processWriter.flush();
            appendLog("> " + command);
            return true;
        } catch (IOException e) {
            appendLog("[SMPOpenMod] Failed to send command: " + e.getMessage());
            return false;
        }
    }

    private void appendLog(String line) {
        consoleBuffer.add(line);
        if (consoleBuffer.size() > MAX_BUFFER_LINES) {
            consoleBuffer.remove(0);
        }
        for (Consumer<String> listener : logListeners) {
            try {
                listener.accept(line);
            } catch (Exception ignored) {}
        }
    }

    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public void removeLogListener(Consumer<String> listener) {
        logListeners.remove(listener);
    }

    public List<String> getConsoleBuffer() {
        return new ArrayList<>(consoleBuffer);
    }

    private void ensureEulaAccepted(File dir) {
        File eulaFile = new File(dir, "eula.txt");
        if (!eulaFile.exists()) {
            try (FileWriter fw = new FileWriter(eulaFile)) {
                fw.write("#By changing the setting below to TRUE you are indicating your agreement to the EULA (https://aka.ms/MinecraftEULA).\n");
                fw.write("eula=true\n");
            } catch (IOException ignored) {}
        } else {
            try {
                List<String> lines = new ArrayList<>();
                boolean found = false;
                try (BufferedReader br = new BufferedReader(new FileReader(eulaFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().startsWith("eula=")) {
                            lines.add("eula=true");
                            found = true;
                        } else {
                            lines.add(line);
                        }
                    }
                }
                if (!found) lines.add("eula=true");
                try (FileWriter fw = new FileWriter(eulaFile)) {
                    for (String l : lines) fw.write(l + "\n");
                }
            } catch (IOException ignored) {}
        }
    }
}
