package com.ninja.terminal.controller;

import com.jcraft.jsch.ChannelShell;
import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import com.ninja.terminal.service.SshService;
import com.ninja.terminal.util.JSchTtyConnector;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.jetbrains.jediterm.terminal.TtyConnector;
import org.jetbrains.jediterm.terminal.ui.JediTermWidget;
import org.jetbrains.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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

                TtyConnector connector = new JSchTtyConnector(sshService.getSession(), channel);

                Platform.runLater(() -> createJediTermWidget(connector, host));

            } catch (Exception e) {
                log.error("Connection failed", e);
                Platform.runLater(() -> {
                    connectionInfo.setText("Connection failed: " + e.getMessage());
                });
            }
        }).start();
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
            public Color getTerminalBackground() {
                return new Color(0x1a, 0x1a, 0x2e);
            }

            @Override
            public Color getTerminalForeground() {
                return new Color(0xe0, 0xe0, 0xe0);
            }

            @Override
            public int getTerminalRows() {
                return 40;
            }

            @Override
            public int getTerminalColumns() {
                return 120;
            }

            @Override
            public int getScrollBufferSize() {
                return ConfigService.getInstance().getSettings().getScrollBufferSize();
            }
        };

        terminalWidget = new JediTermWidget(settings);
        terminalWidget.setTtyConnector(connector);
        terminalWidget.start();

        SwingNode swingNode = new SwingNode();
        swingNode.setContent(terminalWidget);

        swingNode.setOnMouseClicked(event -> swingNode.requestFocus());

        terminalPane.getChildren().add(swingNode);

        host.setLastConnectedAt(LocalDateTime.now());
        ConfigService.getInstance().updateHost(host);

        connectionInfo.setText("Connected: " + host.getName());
    }

    public void disconnect() {
        if (terminalWidget != null) {
            terminalWidget.close();
            terminalWidget = null;
        }
        if (sshService != null) {
            sshService.disconnect();
        }
    }
}
