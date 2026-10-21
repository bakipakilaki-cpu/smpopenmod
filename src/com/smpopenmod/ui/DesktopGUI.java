package com.smpopenmod.ui;

import com.smpopenmod.manager.ServerProcessManager;
import com.smpopenmod.model.ServerProfile;
import com.smpopenmod.storage.ConfigManager;
import com.smpopenmod.system.SshInfoManager;
import com.smpopenmod.tunnel.PlayitManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern Dark Mode Swing Desktop GUI for SMPOpenMod.
 */
public class DesktopGUI extends JFrame {
    private final ConfigManager configManager;
    private final SshInfoManager sshInfoManager;
    private final PlayitManager playitManager;
    private final Map<String, ServerProcessManager> managers = new HashMap<>();

    private DefaultListModel<ServerProfile> serverListModel;
    private JList<ServerProfile> serverJList;

    private JLabel statusLabel;
    private JButton powerButton;
    private JButton restartButton;
    private JLabel folderLabel;
    private JLabel ramLabel;
    private JLabel playitAddrLabel;
    private JButton playitToggleBtn;
    private JLabel sshCommandLabel;

    private JTextArea consoleTextArea;
    private JTextField commandInputField;

    private ServerProfile currentServer;

    public DesktopGUI(ConfigManager configManager, SshInfoManager sshInfoManager, PlayitManager playitManager) {
        this.configManager = configManager;
        this.sshInfoManager = sshInfoManager;
        this.playitManager = playitManager;

        setTitle("SMPOpenMod - Open Source Minecraft Hosting & 24/7 Management");
        setSize(1050, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initTheme();
        initUI();
        loadServersIntoUI();

        // Start periodic status refresher
        Timer timer = new Timer(1500, e -> updateStatusUI());
        timer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // If servers are running, inform user or shutdown cleanly
            }
        });
    }

    private void initTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private void initUI() {
        Color bgDark = new Color(24, 24, 37);
        Color sidebarBg = new Color(30, 30, 46);

        getContentPane().setBackground(bgDark);
        setLayout(new BorderLayout());

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(sidebarBg);
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("⚡ SMPOPENMOD");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 210, 255));

        JLabel subtitleLabel = new JLabel("Open-Source Minecraft Server Host • 24/7 SSH & Playit Built-in");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(160, 160, 180));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Quick SSH info pill in header
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton copySshBtn = createStyledButton("Copy SSH: " + sshInfoManager.getUsername() + "@" + sshInfoManager.getLocalIp(), new Color(60, 60, 90));
        copySshBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sshInfoManager.getLocalSshCommand()), null);
            JOptionPane.showMessageDialog(this, "Copied to clipboard:\n" + sshInfoManager.getLocalSshCommand(), "SSH Command Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        headerRight.add(copySshBtn);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Split Pane: Sidebar + Content
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(260);
        splitPane.setDividerSize(3);
        splitPane.setBackground(bgDark);
        splitPane.setBorder(null);

        // Left Sidebar: Server List
        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(sidebarBg);
        sidebarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel serversHeading = new JLabel("YOUR SERVERS");
        serversHeading.setFont(new Font("SansSerif", Font.BOLD, 12));
        serversHeading.setForeground(new Color(120, 120, 150));
        serversHeading.setBorder(new EmptyBorder(5, 5, 10, 5));
        sidebarPanel.add(serversHeading, BorderLayout.NORTH);

        serverListModel = new DefaultListModel<>();
        serverJList = new JList<>(serverListModel);
        serverJList.setBackground(new Color(36, 36, 54));
        serverJList.setForeground(Color.WHITE);
        serverJList.setFont(new Font("SansSerif", Font.BOLD, 14));
        serverJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverJList.setCellRenderer(new ServerListCellRenderer());
        serverJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectServer(serverJList.getSelectedValue());
            }
        });

        JScrollPane serverScroll = new JScrollPane(serverJList);
        serverScroll.setBorder(null);
        sidebarPanel.add(serverScroll, BorderLayout.CENTER);

        JButton newServerBtn = createStyledButton("+ New Server / Folder", new Color(0, 180, 136));
        newServerBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        newServerBtn.addActionListener(e -> openNewServerDialog());
        sidebarPanel.add(newServerBtn, BorderLayout.SOUTH);

        splitPane.setLeftComponent(sidebarPanel);

        // Right Main Content: Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(bgDark);
        tabbedPane.setForeground(Color.WHITE);

        tabbedPane.addTab("Control Dashboard", buildDashboardTab());
        tabbedPane.addTab("Live Console", buildConsoleTab());
        tabbedPane.addTab("24/7 SSH & Network Info", buildSshTab());

        splitPane.setRightComponent(tabbedPane);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildDashboardTab() {
        Color cardBg = new Color(36, 36, 54);
        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(new Color(24, 24, 37));
        main.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Status & Power Bar
        JPanel topCard = new JPanel(new BorderLayout(15, 15));
        topCard.setBackground(cardBg);
        topCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel statusBox = new JPanel(new GridLayout(2, 1, 4, 4));
        statusBox.setOpaque(false);
        statusLabel = new JLabel("SELECT OR CREATE A SERVER");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusLabel.setForeground(new Color(200, 200, 220));

        folderLabel = new JLabel("Directory: -");
        folderLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        folderLabel.setForeground(new Color(150, 150, 170));
        statusBox.add(statusLabel);
        statusBox.add(folderLabel);

        JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonBox.setOpaque(false);

        powerButton = createStyledButton("START SERVER", new Color(0, 200, 83));
        powerButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        powerButton.setPreferredSize(new Dimension(160, 42));
        powerButton.addActionListener(e -> toggleServerPower());

        restartButton = createStyledButton("Restart", new Color(255, 152, 0));
        restartButton.setPreferredSize(new Dimension(100, 42));
        restartButton.addActionListener(e -> {
            if (currentServer != null) {
                getManager(currentServer).restart();
            }
        });

        buttonBox.add(powerButton);
        buttonBox.add(restartButton);

        topCard.add(statusBox, BorderLayout.CENTER);
        topCard.add(buttonBox, BorderLayout.EAST);
        main.add(topCard, BorderLayout.NORTH);

        // Center Grid Cards: Playit Tunnel & SSH Info
        JPanel grid = new JPanel(new GridLayout(2, 1, 15, 15));
        grid.setOpaque(false);

        // Card 1: Playit Tunnel
        JPanel playitCard = new JPanel(new BorderLayout(10, 10));
        playitCard.setBackground(cardBg);
        playitCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel playitTitle = new JLabel("🌐 PLAYIT.GG ZERO-PORT-FORWARDING TUNNEL");
        playitTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        playitTitle.setForeground(new Color(180, 120, 255));

        playitAddrLabel = new JLabel("Public Minecraft Address: " + playitManager.getTunnelAddress());
        playitAddrLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        playitAddrLabel.setForeground(new Color(0, 210, 255));

        playitToggleBtn = createStyledButton("Toggle Tunnel", new Color(130, 80, 220));
        playitToggleBtn.addActionListener(e -> {
            if (playitManager.isRunning()) {
                playitManager.stop();
            } else {
                try {
                    playitManager.start();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not start Playit: " + ex.getMessage(), "Playit Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            updateStatusUI();
        });

        playitCard.add(playitTitle, BorderLayout.NORTH);
        playitCard.add(playitAddrLabel, BorderLayout.CENTER);
        playitCard.add(playitToggleBtn, BorderLayout.EAST);
        grid.add(playitCard);

        // Card 2: Configuration & Details
        JPanel configCard = new JPanel(new GridLayout(3, 2, 10, 10));
        configCard.setBackground(cardBg);
        configCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        ramLabel = new JLabel("RAM Allocation: -");
        ramLabel.setForeground(Color.WHITE);
        ramLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        sshCommandLabel = new JLabel("SSH Command: " + sshInfoManager.getLocalSshCommand());
        sshCommandLabel.setForeground(Color.WHITE);
        sshCommandLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton openFolderBtn = createStyledButton("Open Server Folder in File Manager", new Color(60, 60, 90));
        openFolderBtn.addActionListener(e -> {
            if (currentServer != null && currentServer.existsOnDisk()) {
                try {
                    Desktop.getDesktop().open(currentServer.getDirectoryFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening directory: " + ex.getMessage());
                }
            }
        });

        JButton deleteServerBtn = createStyledButton("Delete Server From App", new Color(180, 40, 40));
        deleteServerBtn.addActionListener(e -> {
            if (currentServer != null) {
                int res = JOptionPane.showConfirmDialog(this, "Remove server '" + currentServer.getName() + "' from SMPOpenMod?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    configManager.removeServer(currentServer.getId());
                    loadServersIntoUI();
                }
            }
        });

        configCard.add(ramLabel);
        configCard.add(openFolderBtn);
        configCard.add(sshCommandLabel);
        configCard.add(deleteServerBtn);
        grid.add(configCard);

        main.add(grid, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildConsoleTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(24, 24, 37));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        consoleTextArea = new JTextArea();
        consoleTextArea.setBackground(new Color(18, 18, 28));
        consoleTextArea.setForeground(new Color(220, 220, 240));
        consoleTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        consoleTextArea.setEditable(false);
        consoleTextArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(consoleTextArea);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Input Bar
        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setOpaque(false);

        commandInputField = new JTextField();
        commandInputField.setBackground(new Color(36, 36, 54));
        commandInputField.setForeground(Color.WHITE);
        commandInputField.setCaretColor(Color.WHITE);
        commandInputField.setFont(new Font("Monospaced", Font.BOLD, 14));
        commandInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 90)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        commandInputField.addActionListener(e -> sendConsoleCommand());

        JButton sendBtn = createStyledButton("Send", new Color(0, 180, 255));
        sendBtn.addActionListener(e -> sendConsoleCommand());

        inputBar.add(commandInputField, BorderLayout.CENTER);
        inputBar.add(sendBtn, BorderLayout.EAST);
        panel.add(inputBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSshTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 24, 37));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextArea guideArea = new JTextArea();
        guideArea.setBackground(new Color(18, 18, 28));
        guideArea.setForeground(new Color(200, 240, 200));
        guideArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        guideArea.setEditable(false);

        StringBuilder sb = new StringBuilder();
        for (String line : sshInfoManager.get24HourGuide()) {
            sb.append(line).append("\n");
        }
        guideArea.setText(sb.toString());

        panel.add(new JScrollPane(guideArea), BorderLayout.CENTER);
        return panel;
    }

    private void sendConsoleCommand() {
        if (currentServer == null) return;
        String cmd = commandInputField.getText().trim();
        if (cmd.isEmpty()) return;

        ServerProcessManager mgr = getManager(currentServer);
        if (mgr.isRunning()) {
            mgr.sendCommand(cmd);
            commandInputField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Server is currently offline. Start the server first!");
        }
    }

    private void toggleServerPower() {
        if (currentServer == null) return;
        ServerProcessManager mgr = getManager(currentServer);

        if (mgr.isRunning()) {
            mgr.stop();
        } else {
            try {
                mgr.start();
                if (currentServer.isPlayitEnabled() && !playitManager.isRunning()) {
                    try { playitManager.start(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to start server:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        updateStatusUI();
    }

    private void updateStatusUI() {
        if (currentServer == null) {
            statusLabel.setText("NO SERVER SELECTED");
            statusLabel.setForeground(Color.GRAY);
            powerButton.setEnabled(false);
            restartButton.setEnabled(false);
            return;
        }

        ServerProcessManager mgr = getManager(currentServer);
        boolean running = mgr.isRunning();

        if (running) {
            statusLabel.setText("● RUNNING - " + currentServer.getName());
            statusLabel.setForeground(new Color(0, 230, 118));
            powerButton.setText("STOP SERVER");
            powerButton.setBackground(new Color(255, 61, 0));
            restartButton.setEnabled(true);
        } else {
            statusLabel.setText("○ STOPPED - " + currentServer.getName());
            statusLabel.setForeground(new Color(255, 82, 82));
            powerButton.setText("START SERVER");
            powerButton.setBackground(new Color(0, 200, 83));
            restartButton.setEnabled(false);
        }
        powerButton.setEnabled(true);

        folderLabel.setText("Directory: " + currentServer.getDirectory());
        ramLabel.setText("RAM Allocation: " + currentServer.getRamGb() + " GB (" + currentServer.getJarName() + ")");
        playitAddrLabel.setText("Public Minecraft Address: " + playitManager.getTunnelAddress());
        sshCommandLabel.setText("SSH Command: " + sshInfoManager.getLocalSshCommand());

        serverJList.repaint();
    }

    private void selectServer(ServerProfile profile) {
        if (profile == null) return;
        this.currentServer = profile;
        consoleTextArea.setText("");

        ServerProcessManager mgr = getManager(profile);
        mgr.addLogListener(line -> SwingUtilities.invokeLater(() -> {
            consoleTextArea.append(line + "\n");
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        }));

        for (String line : mgr.getConsoleBuffer()) {
            consoleTextArea.append(line + "\n");
        }

        updateStatusUI();
    }

    private void loadServersIntoUI() {
        serverListModel.clear();
        List<ServerProfile> servers = configManager.getServers();
        for (ServerProfile s : servers) {
            serverListModel.addElement(s);
        }
        if (!servers.isEmpty()) {
            serverJList.setSelectedIndex(0);
            selectServer(servers.get(0));
        } else {
            currentServer = null;
            updateStatusUI();
        }
    }

    private void openNewServerDialog() {
        JTextField nameField = new JTextField("FriendSMP");
        JTextField folderField = new JTextField(new File(System.getProperty("user.home"), "minecraft-server").getAbsolutePath());
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                folderField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JTextField ramField = new JTextField("4");
        JTextField jarField = new JTextField("server.jar");

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.add(new JLabel("Server Name:"));
        form.add(nameField);
        form.add(new JLabel("Folder Location:"));

        JPanel folderBox = new JPanel(new BorderLayout(5, 0));
        folderBox.add(folderField, BorderLayout.CENTER);
        folderBox.add(browseBtn, BorderLayout.EAST);
        form.add(folderBox);

        form.add(new JLabel("Allocated RAM (GB):"));
        form.add(ramField);
        form.add(new JLabel("Server JAR Name:"));
        form.add(jarField);

        int result = JOptionPane.showConfirmDialog(this, form, "Create / Add Minecraft Server", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String folder = folderField.getText().trim();
            int ram = 4;
            try { ram = Integer.parseInt(ramField.getText().trim()); } catch (Exception ignored) {}
            String jar = jarField.getText().trim();

            new File(folder).mkdirs();

            ServerProfile profile = new ServerProfile(name, folder, jar, ram, 25565);
            configManager.addServer(profile);
            loadServersIntoUI();
            serverJList.setSelectedValue(profile, true);
        }
    }

    private synchronized ServerProcessManager getManager(ServerProfile profile) {
        return managers.computeIfAbsent(profile.getId(), k -> new ServerProcessManager(profile));
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private class ServerListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ServerProfile profile) {
                ServerProcessManager m = managers.get(profile.getId());
                boolean running = (m != null && m.isRunning());
                String icon = running ? "● " : "○ ";
                setText(icon + profile.getName());
                setForeground(running ? new Color(0, 230, 118) : (isSelected ? Color.WHITE : new Color(180, 180, 200)));
                setBackground(isSelected ? new Color(60, 60, 90) : new Color(36, 36, 54));
                setBorder(new EmptyBorder(8, 10, 8, 10));
            }
            return this;
        }
    }
}
