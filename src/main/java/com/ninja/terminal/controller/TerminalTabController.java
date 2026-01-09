package com.ninja.terminal.controller;

import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import com.ninja.terminal.service.SshService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class TerminalTabController implements Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(TerminalTabController.class);
    
    @FXML private VBox terminalContainer;
    @FXML private TextArea terminalArea;
    @FXML private Label connectionInfo;
    
    private SshService sshService;
    private OutputStream outputStream;
    private Thread readerThread;
    private volatile boolean running = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sshService = new SshService();
        
        // Configure terminal area
        terminalArea.setWrapText(true);
        terminalArea.setEditable(true);
        
        // Style for terminal
        String fontFamily = ConfigService.getInstance().getSettings().getFontFamily();
        int fontSize = ConfigService.getInstance().getSettings().getFontSize();
        terminalArea.setStyle(String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; " +
            "-fx-control-inner-background: #1a1a2e; -fx-text-fill: #e0e0e0;",
            fontFamily, fontSize
        ));
        
        // Handle key input
        terminalArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        terminalArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
    }
    
    public void connect(HostInfo host) {
        connectionInfo.setText(String.format("Connecting to %s@%s:%d...", 
            host.getUsername(), host.getHostname(), host.getPort()));
        
        new Thread(() -> {
            try {
                sshService.connect(host);
                var channel = sshService.openShell();
                
                outputStream = channel.getOutputStream();
                InputStream inputStream = channel.getInputStream();
                
                channel.connect();
                running = true;
                
                // Update last connected time
                host.setLastConnectedAt(LocalDateTime.now());
                ConfigService.getInstance().updateHost(host);
                
                Platform.runLater(() -> {
                    connectionInfo.setText(String.format("Connected: %s@%s", 
                        host.getUsername(), host.getHostname()));
                });
                
                // Read output from SSH
                readerThread = new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while (running && (len = inputStream.read(buffer)) != -1) {
                            String text = new String(buffer, 0, len);
                            // Simple ANSI escape sequence removal (basic)
                            String cleanText = removeAnsiCodes(text);
                            Platform.runLater(() -> {
                                terminalArea.appendText(cleanText);
                                terminalArea.positionCaret(terminalArea.getLength());
                            });
                        }
                    } catch (Exception e) {
                        if (running) {
                            log.error("Error reading from SSH", e);
                        }
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();
                
            } catch (Exception e) {
                log.error("Connection failed", e);
                Platform.runLater(() -> {
                    connectionInfo.setText("Connection failed: " + e.getMessage());
                    terminalArea.appendText("\r\n*** Connection failed: " + e.getMessage() + " ***\r\n");
                });
            }
        }).start();
    }
    
    private void handleKeyPress(KeyEvent event) {
        if (outputStream == null) return;
        
        try {
            String toSend = null;
            
            // Handle special keys
            if (event.getCode() == KeyCode.ENTER) {
                toSend = "\r";
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                toSend = "\u007F";
            } else if (event.getCode() == KeyCode.TAB) {
                toSend = "\t";
            } else if (event.getCode() == KeyCode.UP) {
                toSend = "\u001B[A";
            } else if (event.getCode() == KeyCode.DOWN) {
                toSend = "\u001B[B";
            } else if (event.getCode() == KeyCode.RIGHT) {
                toSend = "\u001B[C";
            } else if (event.getCode() == KeyCode.LEFT) {
                toSend = "\u001B[D";
            } else if (event.isControlDown()) {
                // Ctrl+C, Ctrl+D, etc.
                if (event.getCode().isLetterKey()) {
                    char c = event.getCode().getChar().charAt(0);
                    toSend = String.valueOf((char) (c - 64)); // Ctrl+A = 1, Ctrl+C = 3, etc.
                }
            }
            
            if (toSend != null) {
                outputStream.write(toSend.getBytes());
                outputStream.flush();
                event.consume();
            }
        } catch (Exception e) {
            log.error("Error sending key", e);
        }
    }
    
    private void handleKeyTyped(KeyEvent event) {
        if (outputStream == null) return;
        
        String character = event.getCharacter();
        if (character.isEmpty() || character.charAt(0) < 32) {
            return; // Ignore control characters
        }
        
        try {
            outputStream.write(character.getBytes());
            outputStream.flush();
            event.consume();
        } catch (Exception e) {
            log.error("Error sending character", e);
        }
    }
    
    private String removeAnsiCodes(String text) {
        // Basic ANSI escape sequence removal
        return text.replaceAll("\u001B\\[[0-9;]*[a-zA-Z]", "")
                   .replaceAll("\u001B\\][^\u0007]*\u0007", ""); // OSC sequences
    }
    
    public void disconnect() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (sshService != null) {
            sshService.disconnect();
        }
    }
}
