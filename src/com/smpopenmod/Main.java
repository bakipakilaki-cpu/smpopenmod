package com.smpopenmod;

import com.smpopenmod.storage.ConfigManager;
import com.smpopenmod.system.DesktopShortcutManager;
import com.smpopenmod.system.SshInfoManager;
import com.smpopenmod.tunnel.PlayitManager;
import com.smpopenmod.ui.DesktopGUI;
import com.smpopenmod.ui.TerminalTUI;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * SMPOpenMod Main Entrypoint.
 * Automatically chooses modern Dark Mode GUI or interactive Arrow-key Terminal TUI.
 */
public class Main {
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        boolean forceCli = false;
        boolean forceGui = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--cli") || arg.equalsIgnoreCase("--tui") || arg.equalsIgnoreCase("-c")) {
                forceCli = true;
            } else if (arg.equalsIgnoreCase("--gui") || arg.equalsIgnoreCase("-g")) {
                forceGui = true;
            } else if (arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("-h")) {
                printHelp();
                return;
            }
        }

        // On Linux: auto-add .desktop shortcut to ~/Desktop and app menu
        DesktopShortcutManager.autoInstallDesktopShortcut();

        ConfigManager configManager = new ConfigManager();
        SshInfoManager sshInfoManager = new SshInfoManager();
        PlayitManager playitManager = new PlayitManager();

        // Determine if GUI can be launched
        boolean isHeadless = GraphicsEnvironment.isHeadless();
        String os = System.getProperty("os.name").toLowerCase();
        boolean hasDisplay = true;

        if (os.contains("linux")) {
            String display = System.getenv("DISPLAY");
            String wayland = System.getenv("WAYLAND_DISPLAY");
            if (display == null && wayland == null) {
                hasDisplay = false;
            }
        }

        if (forceCli || isHeadless || !hasDisplay) {
            // Launch interactive arrow-key terminal TUI
            TerminalTUI tui = new TerminalTUI(configManager, sshInfoManager, playitManager);
            tui.start();
        } else {
            // Launch GUI on desktop
            try {
                SwingUtilities.invokeLater(() -> {
                    try {
                        DesktopGUI gui = new DesktopGUI(configManager, sshInfoManager, playitManager);
                        gui.setVisible(true);
                    } catch (Throwable t) {
                        System.err.println("[SMPOpenMod] GUI failed to open, falling back to Terminal TUI: " + t.getMessage());
                        TerminalTUI tui = new TerminalTUI(configManager, sshInfoManager, playitManager);
                        tui.start();
                    }
                });
            } catch (Throwable t) {
                TerminalTUI tui = new TerminalTUI(configManager, sshInfoManager, playitManager);
                tui.start();
            }
        }
    }

    private static void printHelp() {
        System.out.println("⚡ SMPOPENMOD v" + VERSION + " - Open Source Minecraft Server Hosting");
        System.out.println("Usage: java -jar smpopenmod.jar [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --gui, -g      Launch graphical desktop dashboard");
        System.out.println("  --cli, -c      Launch interactive arrow-key terminal TUI");
        System.out.println("  --help, -h     Show this help screen");
    }
}
