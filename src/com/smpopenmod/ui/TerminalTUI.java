package com.smpopenmod.ui;

import com.smpopenmod.manager.ServerProcessManager;
import com.smpopenmod.model.ServerProfile;
import com.smpopenmod.storage.ConfigManager;
import com.smpopenmod.system.SshInfoManager;
import com.smpopenmod.tunnel.PlayitManager;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * Interactive Terminal User Interface with Arrow Key Navigation.
 */
public class TerminalTUI {
    private final ConfigManager configManager;
    private final SshInfoManager sshInfoManager;
    private final PlayitManager playitManager;
    private final Map<String, ServerProcessManager> activeManagers = new HashMap<>();

    private boolean rawModeEnabled = false;

    public TerminalTUI(ConfigManager configManager, SshInfoManager sshInfoManager, PlayitManager playitManager) {
        this.configManager = configManager;
        this.sshInfoManager = sshInfoManager;
        this.playitManager = playitManager;
    }

    public void start() {
        enableRawMode();
        try {
            mainLoop();
        } finally {
            disableRawMode();
        }
    }

    private void mainLoop() {
        int selectedIndex = 0;

        while (true) {
            clearScreen();
            List<ServerProfile> servers = configManager.getServers();

            printHeader();
            printServersSummary(servers);

            List<String> menuItems = new ArrayList<>();
            for (ServerProfile s : servers) {
                ServerProcessManager m = getManager(s);
                String status = (m != null && m.isRunning()) ? " \u001B[32m[RUNNING ●]\u001B[0m" : " \u001B[31m[STOPPED ○]\u001B[0m";
                menuItems.add("Manage Server: \u001B[1m" + s.getName() + "\u001B[0m" + status);
            }
            menuItems.add("\u001B[36m[+] Create / Add New Server (Folder Picker & Creator)\u001B[0m");
            menuItems.add("\u001B[33m[ℹ] 24/7 SSH & Network Info (User, Password, IP)\u001B[0m");
            menuItems.add("\u001B[35m[🌐] Playit Tunnel Manager (Public IP: " + playitManager.getTunnelAddress() + ")\u001B[0m");
            menuItems.add("\u001B[31m[✕] Exit SMPOpenMod\u001B[0m");

            if (selectedIndex >= menuItems.size()) {
                selectedIndex = Math.max(0, menuItems.size() - 1);
            }

            System.out.println("  \u001B[1mUse [↑/↓] Arrow Keys to navigate, [Enter] to select:\u001B[0m");
            System.out.println();

            for (int i = 0; i < menuItems.size(); i++) {
                if (i == selectedIndex) {
                    System.out.println("   \u001B[1;36m▶ \u001B[7m " + stripAnsi(menuItems.get(i)) + " \u001B[0m");
                } else {
                    System.out.println("     " + menuItems.get(i));
                }
            }

            System.out.println();
            int key = readKey();

            if (key == KEY_UP) {
                selectedIndex = (selectedIndex - 1 + menuItems.size()) % menuItems.size();
            } else if (key == KEY_DOWN) {
                selectedIndex = (selectedIndex + 1) % menuItems.size();
            } else if (key == KEY_ENTER) {
                int serverCount = servers.size();
                if (selectedIndex < serverCount) {
                    manageServerMenu(servers.get(selectedIndex));
                } else {
                    int actionIdx = selectedIndex - serverCount;
                    if (actionIdx == 0) {
                        createNewServerWizard();
                    } else if (actionIdx == 1) {
                        showSshInfo();
                    } else if (actionIdx == 2) {
                        managePlayitTunnel();
                    } else if (actionIdx == 3) {
                        System.out.println("\nExiting SMPOpenMod. Goodbye!");
                        break;
                    }
                }
            } else if (key == 'q' || key == 'Q') {
                break;
            }
        }
    }

    private void manageServerMenu(ServerProfile server) {
        int selectedIndex = 0;
        ServerProcessManager manager = getManager(server);

        while (true) {
            clearScreen();
            boolean running = manager.isRunning();

            System.out.println("================================================================================");
            System.out.println("  ⚡ SMPOPENMOD - SERVER CONTROL PANEL: \u001B[1;33m" + server.getName() + "\u001B[0m");
            System.out.println("================================================================================");
            System.out.println(" • Status:        " + (running ? "\u001B[1;32mONLINE (RUNNING) ●\u001B[0m" : "\u001B[1;31mOFFLINE (STOPPED) ○\u001B[0m"));
            System.out.println(" • Directory:     " + server.getDirectory());
            System.out.println(" • Server JAR:    " + server.getJarName());
            System.out.println(" • RAM Allocated: " + server.getRamGb() + " GB");
            System.out.println(" • Local Port:    " + server.getPort());
            System.out.println(" • Playit Tunnel: " + (playitManager.isRunning() ? "\u001B[1;32m" + playitManager.getTunnelAddress() + "\u001B[0m" : "\u001B[31mOFF\u001B[0m"));
            System.out.println("================================================================================");
            System.out.println();

            List<String> options = new ArrayList<>();
            if (running) {
                options.add("\u001B[31m[■] Stop Server (Turn OFF)\u001B[0m");
                options.add("\u001B[33m[↻] Restart Server\u001B[0m");
                options.add("\u001B[36m[💻] Open Live Streaming Console & Command Input\u001B[0m");
            } else {
                options.add("\u001B[32m[▶] Start Server (Turn ON)\u001B[0m");
                options.add("\u001B[36m[💻] View Console Log History\u001B[0m");
            }
            options.add("\u001B[35m[🌐] Toggle Playit Public Tunnel\u001B[0m");
            options.add("\u001B[34m[⚙] Change RAM Allocation\u001B[0m");
            options.add("\u001B[31m[🗑] Delete Server from SMPOpenMod\u001B[0m");
            options.add("\u001B[37m[←] Back to Server List\u001B[0m");

            if (selectedIndex >= options.size()) selectedIndex = 0;

            for (int i = 0; i < options.size(); i++) {
                if (i == selectedIndex) {
                    System.out.println("   \u001B[1;36m▶ \u001B[7m " + stripAnsi(options.get(i)) + " \u001B[0m");
                } else {
                    System.out.println("     " + options.get(i));
                }
            }

            int key = readKey();
            if (key == KEY_UP) {
                selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
            } else if (key == KEY_DOWN) {
                selectedIndex = (selectedIndex + 1) % options.size();
            } else if (key == KEY_ENTER) {
                String plain = stripAnsi(options.get(selectedIndex));
                if (plain.contains("Start Server")) {
                    try {
                        manager.start();
                        if (server.isPlayitEnabled() && !playitManager.isRunning()) {
                            try { playitManager.start(); } catch (Exception ignored) {}
                        }
                    } catch (Exception e) {
                        System.out.println("\u001B[31mFailed to start: " + e.getMessage() + "\u001B[0m");
                        pressAnyKey();
                    }
                } else if (plain.contains("Stop Server")) {
                    manager.stop();
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                } else if (plain.contains("Restart Server")) {
                    manager.restart();
                } else if (plain.contains("Live Streaming Console") || plain.contains("View Console Log")) {
                    openConsoleInteractive(manager);
                } else if (plain.contains("Toggle Playit")) {
                    if (playitManager.isRunning()) {
                        playitManager.stop();
                    } else {
                        try { playitManager.start(); } catch (Exception e) {
                            System.out.println("\u001B[31mPlayit error: " + e.getMessage() + "\u001B[0m");
                            pressAnyKey();
                        }
                    }
                } else if (plain.contains("Change RAM")) {
                    changeRamPrompt(server);
                } else if (plain.contains("Delete Server")) {
                    configManager.removeServer(server.getId());
                    break;
                } else if (plain.contains("Back")) {
                    break;
                }
            } else if (key == KEY_ESC || key == 'b' || key == 'B') {
                break;
            }
        }
    }

    private void openConsoleInteractive(ServerProcessManager manager) {
        disableRawMode();
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  💻 LIVE SERVER CONSOLE - Type commands below (e.g. op, tps, say, stop)");
        System.out.println("  Type \u001B[1;33mexit\u001B[0m or \u001B[1;33m/back\u001B[0m to return to the control menu.");
        System.out.println("================================================================================");

        List<String> history = manager.getConsoleBuffer();
        int start = Math.max(0, history.size() - 25);
        for (int i = start; i < history.size(); i++) {
            System.out.println(history.get(i));
        }

        manager.addLogListener(System.out::println);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) {
                String input = br.readLine();
                if (input == null) break;
                input = input.trim();
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("/back") || input.equalsIgnoreCase(":q")) {
                    break;
                }
                if (!input.isEmpty()) {
                    manager.sendCommand(input);
                }
            }
        } catch (IOException ignored) {}

        manager.removeLogListener(System.out::println);
        enableRawMode();
    }

    private void createNewServerWizard() {
        disableRawMode();
        clearScreen();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("================================================================================");
        System.out.println("  ✨ SMPOPENMOD - SERVER CREATION & FOLDER SETUP");
        System.out.println("================================================================================");

        try {
            System.out.print("Enter a friendly Server Name (e.g. FriendSMP): ");
            String name = reader.readLine();
            if (name == null || name.trim().isEmpty()) name = "Minecraft Server";

            System.out.println("\nWhere should this server live?");
            System.out.println("Default: " + new File(System.getProperty("user.home"), "minecraft-server").getAbsolutePath());
            System.out.print("Folder path (press Enter for default, or type new folder path): ");
            String folderStr = reader.readLine();
            File folder;
            if (folderStr == null || folderStr.trim().isEmpty()) {
                folder = new File(System.getProperty("user.home"), "minecraft-server");
            } else {
                folder = new File(folderStr.trim());
            }

            if (!folder.exists()) {
                folder.mkdirs();
                System.out.println("✔ Created folder: " + folder.getAbsolutePath());
            }

            // Check for jar
            File[] jars = folder.listFiles((d, f) -> f.endsWith(".jar"));
            String selectedJar = "server.jar";
            if (jars != null && jars.length > 0) {
                selectedJar = jars[0].getName();
                System.out.println("✔ Detected existing JAR in folder: " + selectedJar);
            } else {
                System.out.println("\nNo JAR found in folder. What would you like to do?");
                System.out.println(" [1] Auto-download Purpur 1.21.4 (High performance, recommended)");
                System.out.println(" [2] I will place my own server.jar into the folder later");
                System.out.print("Choice [1]: ");
                String jarChoice = reader.readLine();
                if (jarChoice == null || jarChoice.trim().isEmpty() || jarChoice.trim().equals("1")) {
                    downloadPurpurJar(folder);
                    selectedJar = "purpur.jar";
                }
            }

            System.out.print("\nAllocated RAM in GB (e.g. 4): ");
            String ramStr = reader.readLine();
            int ram = 4;
            try { ram = Integer.parseInt(ramStr.trim()); } catch (Exception ignored) {}

            ServerProfile profile = new ServerProfile(name.trim(), folder.getAbsolutePath(), selectedJar, ram, 25565);
            configManager.addServer(profile);

            System.out.println("\n\u001B[32m✔ Server successfully added to SMPOpenMod!\u001B[0m");
            System.out.println("You can now manage, turn ON/OFF, and view console anytime.");
            pressAnyKey();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            pressAnyKey();
        } finally {
            enableRawMode();
        }
    }

    private void downloadPurpurJar(File folder) {
        System.out.println("Downloading Purpur 1.21 server jar...");
        File target = new File(folder, "purpur.jar");
        try {
            URL url = new URL("https://api.purpurmc.org/v2/purpur/1.21/latest/download");
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            System.out.println("✔ Downloaded purpur.jar (" + (target.length() / (1024 * 1024)) + "MB)");
        } catch (Exception e) {
            System.out.println("Could not auto-download: " + e.getMessage());
        }
    }

    private void showSshInfo() {
        clearScreen();
        sshInfoManager.refresh();
        for (String line : sshInfoManager.get24HourGuide()) {
            System.out.println(line);
        }
        pressAnyKey();
    }

    private void managePlayitTunnel() {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  🌐 SMPOPENMOD - PLAYIT ZERO-PORT-FORWARDING TUNNEL");
        System.out.println("================================================================================");
        System.out.println("• Status:         " + (playitManager.isRunning() ? "\u001B[32mACTIVE ●\u001B[0m" : "\u001B[31mSTOPPED ○\u001B[0m"));
        System.out.println("• Public Address: \u001B[1;36m" + playitManager.getTunnelAddress() + "\u001B[0m");
        if (playitManager.getClaimUrl() != null) {
            System.out.println("• Claim URL:      \u001B[1;33m" + playitManager.getClaimUrl() + "\u001B[0m");
        }
        System.out.println("================================================================================");
        System.out.println(" [1] " + (playitManager.isRunning() ? "Stop Tunnel" : "Start Tunnel"));
        System.out.println(" [2] Back");
        System.out.println();

        int key = readKey();
        if (key == '1') {
            if (playitManager.isRunning()) {
                playitManager.stop();
            } else {
                try {
                    playitManager.start();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    pressAnyKey();
                }
            }
        }
    }

    private void changeRamPrompt(ServerProfile server) {
        disableRawMode();
        System.out.print("\nEnter new RAM allocation in GB (current: " + server.getRamGb() + " GB): ");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        try {
            String val = r.readLine();
            int newRam = Integer.parseInt(val.trim());
            if (newRam > 0 && newRam <= 128) {
                server.setRamGb(newRam);
                configManager.saveServers();
                System.out.println("✔ RAM updated to " + newRam + " GB.");
            }
        } catch (Exception ignored) {}
        enableRawMode();
    }

    private synchronized ServerProcessManager getManager(ServerProfile profile) {
        return activeManagers.computeIfAbsent(profile.getId(), k -> new ServerProcessManager(profile));
    }

    private void printHeader() {
        System.out.println("\u001B[1;36m┌──────────────────────────────────────────────────────────────────────────────┐\u001B[0m");
        System.out.println("\u001B[1;36m│\u001B[0m  \u001B[1;33m⚡ SMPOPENMOD v1.0.0\u001B[0m - Open Source Minecraft Hosting & 24/7 Management  \u001B[1;36m│\u001B[0m");
        System.out.println("\u001B[1;36m│\u001B[0m  SSH: \u001B[1;32m" + sshInfoManager.getLocalSshCommand() + "\u001B[0m | Playit: \u001B[1;35m" + playitManager.getTunnelAddress() + "\u001B[0m \u001B[1;36m│\u001B[0m");
        System.out.println("\u001B[1;36m└──────────────────────────────────────────────────────────────────────────────┘\u001B[0m");
        System.out.println();
    }

    private void printServersSummary(List<ServerProfile> servers) {
        if (servers.isEmpty()) {
            System.out.println("  \u001B[33mNo servers created yet. Choose '[+] Create / Add New Server' below.\u001B[0m\n");
        } else {
            System.out.println("  \u001B[1mRegistered Servers (" + servers.size() + "):\u001B[0m");
        }
    }

    // Terminal raw mode & Key handling
    private static final int KEY_UP = 1001;
    private static final int KEY_DOWN = 1002;
    private static final int KEY_ENTER = 10;
    private static final int KEY_ESC = 27;

    private int readKey() {
        try {
            int c = System.in.read();
            if (c == 27) { // Escape sequence
                if (System.in.available() > 0) {
                    int c2 = System.in.read();
                    if (c2 == '[') {
                        int c3 = System.in.read();
                        if (c3 == 'A') return KEY_UP;
                        if (c3 == 'B') return KEY_DOWN;
                    }
                }
                return KEY_ESC;
            }
            if (c == '\r') return KEY_ENTER;
            if (c == 'w' || c == 'k') return KEY_UP;
            if (c == 's' || c == 'j') return KEY_DOWN;
            return c;
        } catch (IOException e) {
            return -1;
        }
    }

    private void pressAnyKey() {
        System.out.println("\nPress [Enter] to continue...");
        try {
            while (System.in.read() != '\n');
        } catch (Exception ignored) {}
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void enableRawMode() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux") || os.contains("mac") || os.contains("darwin")) {
            try {
                new ProcessBuilder("sh", "-c", "stty -echo -icanon < /dev/tty").inheritIO().start().waitFor();
                rawModeEnabled = true;
            } catch (Exception ignored) {}
        }
    }

    private void disableRawMode() {
        if (!rawModeEnabled) return;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux") || os.contains("mac") || os.contains("darwin")) {
            try {
                new ProcessBuilder("sh", "-c", "stty echo icanon < /dev/tty").inheritIO().start().waitFor();
                rawModeEnabled = false;
            } catch (Exception ignored) {}
        }
    }

    private String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
