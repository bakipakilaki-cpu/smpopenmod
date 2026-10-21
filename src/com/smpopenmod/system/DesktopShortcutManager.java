package com.smpopenmod.system;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages desktop launcher creation and system integration on Linux.
 */
public class DesktopShortcutManager {

    public static void autoInstallDesktopShortcut() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("linux")) {
            return;
        }

        String userHome = System.getProperty("user.home");
        File desktopDir = new File(userHome, "Desktop");
        File appsDir = new File(userHome, ".local/share/applications");

        // Determine executable path
        String jarPath = getJarPath();
        String execCommand;
        if (new File("/opt/smpopenmod/smpopenmod.jar").exists()) {
            execCommand = "java -jar /opt/smpopenmod/smpopenmod.jar";
        } else if (new File("/usr/local/bin/smpopenmod").exists()) {
            execCommand = "/usr/local/bin/smpopenmod";
        } else {
            execCommand = "java -jar \"" + jarPath + "\"";
        }

        String desktopContent = "[Desktop Entry]\n" +
                "Version=1.0\n" +
                "Type=Application\n" +
                "Name=SMPOpenMod\n" +
                "GenericName=Minecraft Server Hosting\n" +
                "Comment=Open Source Minecraft Server Host with Built-in Playit & SSH\n" +
                "Exec=" + execCommand + "\n" +
                "Icon=smpopenmod\n" +
                "Terminal=false\n" +
                "Categories=Game;Network;Server;Java;\n" +
                "StartupNotify=true\n";

        // 1. Install to application menu
        if (appsDir.exists() || appsDir.mkdirs()) {
            File appFile = new File(appsDir, "smpopenmod.desktop");
            writeFile(appFile, desktopContent);
        }

        // 2. Install to Desktop if directory exists
        if (desktopDir.exists() && desktopDir.isDirectory()) {
            File desktopFile = new File(desktopDir, "smpopenmod.desktop");
            writeFile(desktopFile, desktopContent);
            desktopFile.setExecutable(true, false);

            try {
                // Mark trusted in GNOME / XFCE
                new ProcessBuilder("gio", "set", desktopFile.getAbsolutePath(), "metadata::trusted", "true").start();
            } catch (Exception ignored) {}
        }
    }

    private static void writeFile(File file, String content) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        } catch (IOException ignored) {}
    }

    private static String getJarPath() {
        try {
            return new File(DesktopShortcutManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return "/opt/smpopenmod/smpopenmod.jar";
        }
    }
}
