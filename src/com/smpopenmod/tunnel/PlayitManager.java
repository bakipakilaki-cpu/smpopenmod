package com.smpopenmod.tunnel;

import com.smpopenmod.storage.ConfigManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the playit.gg zero-port-forwarding public tunnel agent.
 */
public class PlayitManager {
    private static final String PLAYIT_VERSION = "v0.15.26";
    private Process playitProcess;
    private String tunnelAddress = "Waiting for tunnel...";
    private String claimUrl = null;
    private boolean downloading = false;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("([a-zA-Z0-9.-]+\\.ply\\.gg:\\d+)");
    private static final Pattern CLAIM_PATTERN = Pattern.compile("(https://playit\\.gg/claim/[a-zA-Z0-9]+)");

    public PlayitManager() {}

    public synchronized boolean isRunning() {
        return playitProcess != null && playitProcess.isAlive();
    }

    public synchronized String getTunnelAddress() {
        return tunnelAddress;
    }

    public synchronized String getClaimUrl() {
        return claimUrl;
    }

    public synchronized boolean isDownloading() {
        return downloading;
    }

    public File getPlayitBinary() {
        File binDir = new File(ConfigManager.getAppDir(), "bin");
        String os = System.getProperty("os.name").toLowerCase();
        String name = os.contains("win") ? "playit.exe" : "playit";
        return new File(binDir, name);
    }

    public synchronized void start() throws Exception {
        if (isRunning()) return;

        File binary = getPlayitBinary();
        if (!binary.exists() || binary.length() == 0) {
            downloadBinary();
        }

        if (!binary.canExecute()) {
            binary.setExecutable(true, false);
        }

        ProcessBuilder pb = new ProcessBuilder(binary.getAbsolutePath(), "run");
        pb.directory(ConfigManager.getAppDir());
        pb.redirectErrorStream(true);

        playitProcess = pb.start();

        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(playitProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parsePlayitOutput(line);
                }
            } catch (IOException ignored) {}

            synchronized (PlayitManager.this) {
                playitProcess = null;
            }
        });
    }

    public synchronized void stop() {
        if (playitProcess != null) {
            playitProcess.destroy();
            try {
                if (!playitProcess.waitFor(3, TimeUnit.SECONDS)) {
                    playitProcess.destroyForcibly();
                }
            } catch (InterruptedException ignored) {}
            playitProcess = null;
            tunnelAddress = "Stopped";
        }
    }

    private synchronized void parsePlayitOutput(String line) {
        Matcher claimMatcher = CLAIM_PATTERN.matcher(line);
        if (claimMatcher.find()) {
            claimUrl = claimMatcher.group(1);
        }

        Matcher addrMatcher = ADDRESS_PATTERN.matcher(line);
        if (addrMatcher.find()) {
            tunnelAddress = addrMatcher.group(1);
        }
    }

    public void downloadBinary() throws IOException {
        downloading = true;
        File binary = getPlayitBinary();
        binary.getParentFile().mkdirs();

        String url = resolveDownloadUrl();
        System.out.println("[PlayitManager] Downloading Playit agent from: " + url);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (SMPOpenMod Agent)");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (SMPOpenMod Agent)");
            conn.connect();
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(binary)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } finally {
            downloading = false;
        }

        binary.setExecutable(true, false);
        System.out.println("[PlayitManager] Playit downloaded successfully to: " + binary.getAbsolutePath());
    }

    private String resolveDownloadUrl() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        boolean isAarch64 = arch.contains("aarch64") || arch.contains("arm64");

        if (os.contains("mac") || os.contains("darwin")) {
            return isAarch64
                    ? "https://github.com/playit-cloud/playit-agent/releases/download/" + PLAYIT_VERSION + "/playit-darwin-arm64"
                    : "https://github.com/playit-cloud/playit-agent/releases/download/" + PLAYIT_VERSION + "/playit-darwin-amd64";
        } else if (os.contains("win")) {
            return "https://github.com/playit-cloud/playit-agent/releases/download/" + PLAYIT_VERSION + "/playit-windows-x86_64.exe";
        } else {
            // Linux default
            return isAarch64
                    ? "https://github.com/playit-cloud/playit-agent/releases/download/" + PLAYIT_VERSION + "/playit-linux-aarch64"
                    : "https://github.com/playit-cloud/playit-agent/releases/download/" + PLAYIT_VERSION + "/playit-linux-amd64";
        }
    }
}
