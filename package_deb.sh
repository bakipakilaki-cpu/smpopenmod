#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/dist"
PKG_DIR="${SCRIPT_DIR}/deb_package"

rm -rf "${PKG_DIR}"
mkdir -p "${PKG_DIR}/DEBIAN"
mkdir -p "${PKG_DIR}/opt/smpopenmod"
mkdir -p "${PKG_DIR}/usr/local/bin"
mkdir -p "${PKG_DIR}/usr/share/applications"
mkdir -p "${PKG_DIR}/usr/share/icons/hicolor/128x128/apps"

# 1. Control file
cat << 'EOF' > "${PKG_DIR}/DEBIAN/control"
Package: smpopenmod
Version: 1.0.0
Section: games
Priority: optional
Architecture: all
Maintainer: Antigravity & Branko <brankothebest@friendsmp.net>
Depends: default-jre (>= 2:1.17) | openjdk-17-jre | openjdk-21-jre
Description: SMPOpenMod - Open Source Minecraft Server Hosting & Management
 Powerful cross-platform server hosting app for Linux and macOS.
 Built-in 24/7 SSH support, automatic Playit tunnel with zero port-forwarding,
 arrow-key terminal TUI, modern desktop GUI, server persistence, and auto-desktop shortcut.
EOF

# 2. Post-installation script (auto-adds to desktop on Linux)
cat << 'EOF' > "${PKG_DIR}/DEBIAN/postinst"
#!/bin/bash
set -e

chmod +x /usr/local/bin/smpopenmod
chmod 644 /usr/share/applications/smpopenmod.desktop

for user_home in /home/*; do
    if [ -d "$user_home/Desktop" ]; then
        cp /usr/share/applications/smpopenmod.desktop "$user_home/Desktop/smpopenmod.desktop" 2>/dev/null || true
        chmod +x "$user_home/Desktop/smpopenmod.desktop" 2>/dev/null || true
        user_name=$(basename "$user_home")
        chown "$user_name:$user_name" "$user_home/Desktop/smpopenmod.desktop" 2>/dev/null || true
        if command -v gio > /dev/null 2>&1; then
            su - "$user_name" -c "gio set '$user_home/Desktop/smpopenmod.desktop' metadata::trusted true" 2>/dev/null || true
        fi
    fi
done

if command -v update-desktop-database > /dev/null 2>&1; then
    update-desktop-database 2>/dev/null || true
fi

exit 0
EOF
chmod 755 "${PKG_DIR}/DEBIAN/postinst"

# 3. Payload binaries and icons
cp "${DIST_DIR}/smpopenmod.jar" "${PKG_DIR}/opt/smpopenmod/smpopenmod.jar"
cp "${SCRIPT_DIR}/resources/icon.png" "${PKG_DIR}/opt/smpopenmod/icon.png"
cp "${SCRIPT_DIR}/resources/icon.png" "${PKG_DIR}/usr/share/icons/hicolor/128x128/apps/smpopenmod.png"

# 4. Binary wrapper
cat << 'EOF' > "${PKG_DIR}/usr/local/bin/smpopenmod"
#!/bin/bash
exec java -jar /opt/smpopenmod/smpopenmod.jar "$@"
EOF
chmod 755 "${PKG_DIR}/usr/local/bin/smpopenmod"

# 5. Desktop launcher
cat << 'EOF' > "${PKG_DIR}/usr/share/applications/smpopenmod.desktop"
[Desktop Entry]
Version=1.0
Type=Application
Name=SMPOpenMod
GenericName=Minecraft Server Host
Comment=Open Source Minecraft Server Host with Built-in Playit & SSH
Exec=/usr/local/bin/smpopenmod
Icon=/opt/smpopenmod/icon.png
Terminal=false
Categories=Game;Network;Server;Java;
StartupNotify=true
EOF
chmod 644 "${PKG_DIR}/usr/share/applications/smpopenmod.desktop"

# 6. Build .deb package
dpkg-deb --build --root-owner-group "${PKG_DIR}" "${DIST_DIR}/smpopenmod.deb"
cp "${DIST_DIR}/smpopenmod.deb" "${DIST_DIR}/smpopenmod_1.0.0_all.deb"

echo "=== Debian Package built successfully! ==="
ls -lh "${DIST_DIR}/smpopenmod.deb"
