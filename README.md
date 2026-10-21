# ⚡ SMPOpenMod

**SMPOpenMod** is an open-source, cross-platform Minecraft server hosting, automation, and management suite built specifically for Linux and macOS.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-emerald.svg)](https://github.com/bakipakilaki-cpu/smpopenmod/releases)
[![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20macOS-cyan.svg)]()

---

## ✨ Features

- 🌐 **Built-in Playit.gg Tunnel**: Zero port-forwarding required. Friends can join your server from anywhere on Earth via automatic `*.ply.gg` tunnel addresses.
- 🔑 **24/7 SSH Remote Management**: Automatic detection and display of system username, password instructions, LAN IP, public WAN IP, and background execution guide (screen, tmux, and systemd keepawake).
- 🎮 **Dual Interface (GUI & Terminal TUI)**:
  - **Desktop GUI**: Sleek modern Dark Mode dashboard built with Java Swing.
  - **Interactive Arrow-Key TUI**: Works seamlessly in headless servers, SSH sessions, and terminals with `[↑/↓]` arrow-key navigation.
- 📂 **Server & Folder Manager**: Interactive folder selector & creator. Pick an existing folder or create a new one anywhere on your system.
- 💾 **Server State Persistence**: When you open SMPOpenMod again, all your created servers are remembered with their statuses, paths, and configurations (`~/.smpopenmod/servers.json`).
- ⚡ **Power Controls & Live Console**: Turn servers ON or OFF with a single click/enter. Stream live console logs and send commands directly into the server in real-time.
- 🐧 **Linux Auto-Desktop Integration**: Automatically creates desktop shortcuts (`~/Desktop/smpopenmod.desktop`) and application menu entries.
- 📦 **Debian Package (`.deb`)**: Native installation for Ubuntu, Debian, Linux Mint, and derivatives via `sudo apt install ./smpopenmod.deb`.

---

## 🚀 Installation & Downloads

### Download Releases
Download the latest binaries from [GitHub Releases](https://github.com/bakipakilaki-cpu/smpopenmod/releases/tag/v1.0.0):
- **Debian / Ubuntu / Mint**: [`smpopenmod.deb`](https://github.com/bakipakilaki-cpu/smpopenmod/releases/download/v1.0.0/smpopenmod.deb)
- **Standalone Cross-Platform JAR**: [`smpopenmod.jar`](https://github.com/bakipakilaki-cpu/smpopenmod/releases/download/v1.0.0/smpopenmod.jar)

---

### Linux (.deb Package)
```bash
# 1. Install package
sudo dpkg -i smpopenmod.deb || sudo apt-get install -f -y

# 2. Launch from Desktop or Terminal
smpopenmod
```
*(The package automatically installs a launcher directly onto your Desktop).*

---

### Standalone JAR (macOS & Linux)
Requirements: Java 17 or higher (`default-jre` or `openjdk-17-jre`).

```bash
# Run graphical dashboard (on desktop):
java -jar smpopenmod.jar

# Run interactive terminal arrow-key TUI (in terminal / SSH):
java -jar smpopenmod.jar --cli
```

---

## 🛠️ Building from Source

```bash
git clone https://github.com/bakipakilaki-cpu/smpopenmod.git
cd smpopenmod
chmod +x build.sh
./build.sh
```

---

## 📜 License
Released under the [MIT License](LICENSE). Made with ❤️ for the Minecraft community.
