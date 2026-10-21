package com.smpopenmod.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Gathers SSH connection and networking details for 24/7 server management.
 */
public class SshInfoManager {
    private String username;
    private String hostname;
    private String localIp;
    private String publicIp;
    private boolean sshServiceRunning;

    public SshInfoManager() {
        refresh();
    }

    public void refresh() {
        this.username = System.getProperty("user.name");
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.hostname = "localhost";
        }

        this.localIp = detectLocalIp();
        this.publicIp = detectPublicIp();
        this.sshServiceRunning = checkSshService();
    }

    public String getUsername() { return username; }
    public String getHostname() { return hostname; }
    public String getLocalIp() { return localIp; }
    public String getPublicIp() { return publicIp; }
    public boolean isSshServiceRunning() { return sshServiceRunning; }

    public String getLocalSshCommand() {
        return "ssh " + username + "@" + localIp;
    }

    public String getPublicSshCommand() {
        return "ssh " + username + "@" + (publicIp != null ? publicIp : localIp);
    }

    private String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String detectPublicIp() {
        String[] providers = {"https://api.ipify.org", "https://icanhazip.com", "https://checkip.amazonaws.com"};
        for (String urlStr : providers) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String ip = reader.readLine();
                    if (ip != null && !ip.trim().isEmpty()) {
                        return ip.trim();
                    }
                }
            } catch (Exception ignored) {}
        }
        return "Unavailable";
    }

    private boolean checkSshService() {
        // Quick socket check to port 22 on localhost
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 22), 500);
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public List<String> get24HourGuide() {
        List<String> lines = new ArrayList<>();
        lines.add("================================================================================");
        lines.add("                  ✦ SMPOPENMOD 24/7 SSH MANAGEMENT GUIDE ✦");
        lines.add("================================================================================");
        lines.add("• Current Host: " + hostname + " (" + System.getProperty("os.name") + ")");
        lines.add("• SSH Username: " + username);
        lines.add("• Local IP:     " + localIp);
        lines.add("• Public WAN IP:" + publicIp);
        lines.add("• SSH Status:   " + (sshServiceRunning ? "RUNNING (Port 22 Active)" : "PORT 22 INACTIVE / BLOCKED"));
        lines.add("");
        lines.add("1. HOW TO CONNECT VIA SSH (24-Hour Remote Control):");
        lines.add("   From same network:   " + getLocalSshCommand());
        lines.add("   From Internet (WAN): " + getPublicSshCommand());
        lines.add("");
        lines.add("2. PASSWORD AUTHENTICATION SETUP:");
        lines.add("   Set or change your SSH password in terminal:");
        lines.add("   $ passwd");
        lines.add("");
        lines.add("3. KEEPING YOUR SERVER RUNNING 24/7 (Prevent sleep / auto-shutdown):");
        lines.add("   Linux (systemd keepawake):");
        lines.add("   $ sudo systemctl mask sleep.target suspend.target hibernate.target");
        lines.add("   Mac (caffeinate):");
        lines.add("   $ caffeinate -s -i -d java -jar smpopenmod.jar");
        lines.add("");
        lines.add("4. RUNNING IN BACKGROUND (Detached / 24h Screen):");
        lines.add("   $ screen -S mcserver java -jar smpopenmod.jar --cli");
        lines.add("   Detach: Press Ctrl+A then D | Reattach: screen -r mcserver");
        lines.add("================================================================================");
        return lines;
    }
}
