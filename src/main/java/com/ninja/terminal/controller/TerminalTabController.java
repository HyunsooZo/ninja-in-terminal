package com.ninja.terminal.controller;

import com.jcraft.jsch.ChannelShell;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import com.ninja.terminal.service.SshService;
import com.ninja.terminal.util.JSchTtyConnector;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class TerminalTabController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(TerminalTabController.class);

    @FXML private StackPane terminalPane;
    @FXML private Label connectionInfo;

    private SshService sshService;
    private JediTermWidget terminalWidget;
    private TtyConnector ttyConnector;
    private Runnable onConnectionFailed;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sshService = new SshService();
    }

    public void connect(HostInfo host) {
        connectionInfo.setText(String.format("Connecting to %s@%s:%d...",
                host.getUsername(), host.getHostname(), host.getPort()));

        new Thread(() -> {
            try {
                sshService.connect(host);
                ChannelShell channel = sshService.openShell();

                ttyConnector = new JSchTtyConnector(sshService.getSession(), channel);

                Platform.runLater(() -> createJediTermWidget(ttyConnector, host));

            } catch (Exception e) {
                log.error("Connection failed to {}@{}:{}", host.getUsername(), host.getHostname(), host.getPort(), e);
                Platform.runLater(() -> {
                    String detailedError = getDetailedErrorMessage(e);
                    connectionInfo.setText("Connection failed: " + detailedError);
                    connectionInfo.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");

                    // Call callback to close tab if configured
                    if (onConnectionFailed != null) {
                        onConnectionFailed.run();
                    }
                });
            }
        }).start();
    }

    private String getDetailedErrorMessage(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

        // Provide more helpful error messages based on exception type
        if (message.contains("Auth fail") || message.contains("Authentication")) {
            return "Authentication failed. Please check your username and password/key.";
        } else if (message.contains("UnknownHostException") || message.contains("resolve")) {
            return "Cannot resolve hostname. Please check the hostname/IP address.";
        } else if (message.contains("timeout") || message.contains("timed out")) {
            return "Connection timeout. Please check the host is reachable and port is correct.";
        } else if (message.contains("Connection refused")) {
            return "Connection refused. Please check SSH service is running on the host.";
        } else if (message.contains("Network is unreachable")) {
            return "Network unreachable. Please check your internet connection.";
        }

        return message;
    }

    private void createJediTermWidget(TtyConnector connector, HostInfo host) {
        DefaultSettingsProvider settings = new DefaultSettingsProvider() {
            @Override
            public Font getTerminalFont() {
                String fontFamily = ConfigService.getInstance().getSettings().getFontFamily();
                int fontSize = ConfigService.getInstance().getSettings().getFontSize();
                try {
                    return new Font(fontFamily, Font.PLAIN, fontSize);
                } catch (Exception e) {
                    log.warn("Font {} not found, using default", fontFamily);
                    return new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
                }
            }

            @Override
            public float getTerminalFontSize() {
                return ConfigService.getInstance().getSettings().getFontSize();
            }

            @Override
            public ColorPalette getTerminalColorPalette() {
                return super.getTerminalColorPalette();
            }

            @NotNull
            @Override
            public TextStyle getDefaultStyle() {
                return new TextStyle(
                        TerminalColor.rgb(0xe0, 0xe0, 0xe0),
                        TerminalColor.rgb(0x1a, 0x1a, 0x2e)
                );
            }
        };

        terminalWidget = new JediTermWidget(settings);
        terminalWidget.setTtyConnector(connector);
        terminalWidget.start();

        SwingNode swingNode = new SwingNode();
        swingNode.setContent(terminalWidget);

        // 포커스 문제 해결
        swingNode.setOnMouseClicked(event -> swingNode.requestFocus());

        terminalPane.getChildren().add(swingNode);

        // 연결 시간 업데이트 및 저장
        host.setLastConnectedAt(LocalDateTime.now());
        ConfigService.getInstance().updateHost(host);

        connectionInfo.setText("Connected: " + host.getName());

        // Execute startup command if configured
        if (host.getStartupCommand() != null && !host.getStartupCommand().trim().isEmpty()) {
            // Wait a bit for terminal to fully initialize, then execute startup command
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Wait 500ms
                    executeCommand(host.getStartupCommand());
                    log.info("Executed startup command for host: {}", host.getName());
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting to execute startup command", e);
                }
            }).start();
        }
    }

    public void disconnect() {
        if (terminalWidget != null) {
            try {
                terminalWidget.close();
            } catch (Exception e) {
                log.warn("Error closing terminal widget", e);
            } finally {
                terminalWidget = null;
            }
        }
        if (sshService != null) {
            try {
                sshService.disconnect();
            } catch (Exception e) {
                log.warn("Error disconnecting SSH service", e);
            }
        }
    }

    /**
     * Execute a command in the terminal
     * @param command The command to execute (will automatically add newline)
     */
    public void executeCommand(String command) {
        if (ttyConnector == null || !ttyConnector.isConnected()) {
            log.warn("Cannot execute command: terminal not connected");
            return;
        }

        try {
            // Send command with newline to execute it
            String commandWithNewline = command + "\n";
            ttyConnector.write(commandWithNewline);
            log.info("Executed command: {}", command);
        } catch (Exception e) {
            log.error("Failed to execute command: {}", command, e);
        }
    }

    /**
     * Check if the terminal is connected and ready
     */
    public boolean isConnected() {
        return ttyConnector != null && ttyConnector.isConnected();
    }

    /**
     * Set callback to be called when connection fails
     * @param callback Runnable to execute on connection failure
     */
    public void setOnConnectionFailed(Runnable callback) {
        this.onConnectionFailed = callback;
    }
}