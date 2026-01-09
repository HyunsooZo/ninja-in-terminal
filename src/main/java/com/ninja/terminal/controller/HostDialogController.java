package com.ninja.terminal.controller;

import com.ninja.terminal.model.HostGroup;
import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class HostDialogController implements Initializable {
    
    @FXML private TextField nameField;
    @FXML private TextField hostnameField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton passwordAuth;
    @FXML private RadioButton keyAuth;
    @FXML private TextField keyPathField;
    @FXML private PasswordField passphraseField;
    @FXML private Button browseKeyBtn;
    @FXML private ComboBox<HostGroup> groupCombo;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    
    private MainController mainController;
    private HostInfo existingHost;
    private final ConfigService configService = ConfigService.getInstance();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up auth type toggle
        ToggleGroup authGroup = new ToggleGroup();
        passwordAuth.setToggleGroup(authGroup);
        keyAuth.setToggleGroup(authGroup);
        passwordAuth.setSelected(true);
        
        // Enable/disable key fields based on auth type
        keyPathField.disableProperty().bind(passwordAuth.selectedProperty());
        passphraseField.disableProperty().bind(passwordAuth.selectedProperty());
        browseKeyBtn.disableProperty().bind(passwordAuth.selectedProperty());
        passwordField.disableProperty().bind(keyAuth.selectedProperty());
        
        // Load groups into combo box
        groupCombo.getItems().add(null); // No group option
        groupCombo.getItems().addAll(configService.getGroups());
        
        // Default port
        portField.setText("22");
        
        // Numeric validation for port
        portField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                portField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }
    
    public void setHost(HostInfo host) {
        this.existingHost = host;
        
        nameField.setText(host.getName());
        hostnameField.setText(host.getHostname());
        portField.setText(String.valueOf(host.getPort()));
        usernameField.setText(host.getUsername());
        
        if (host.getAuthType() == HostInfo.AuthType.KEY) {
            keyAuth.setSelected(true);
            keyPathField.setText(host.getPrivateKeyPath());
            passphraseField.setText(host.getPassphrase());
        } else {
            passwordAuth.setSelected(true);
            passwordField.setText(host.getPassword());
        }
        
        // Select group
        if (host.getGroupId() != null) {
            configService.getGroups().stream()
                    .filter(g -> g.getId().equals(host.getGroupId()))
                    .findFirst()
                    .ifPresent(g -> groupCombo.setValue(g));
        }
    }
    
    @FXML
    private void onBrowseKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Private Key");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/.ssh"));
        
        File file = fileChooser.showOpenDialog(keyPathField.getScene().getWindow());
        if (file != null) {
            keyPathField.setText(file.getAbsolutePath());
        }
    }
    
    @FXML
    private void onSave() {
        // Validation
        if (hostnameField.getText().trim().isEmpty()) {
            showError("Hostname is required");
            return;
        }
        if (usernameField.getText().trim().isEmpty()) {
            showError("Username is required");
            return;
        }
        
        HostInfo host = existingHost != null ? existingHost : new HostInfo();
        
        host.setName(nameField.getText().trim().isEmpty() ? 
                     hostnameField.getText().trim() : nameField.getText().trim());
        host.setHostname(hostnameField.getText().trim());
        host.setPort(Integer.parseInt(portField.getText().isEmpty() ? "22" : portField.getText()));
        host.setUsername(usernameField.getText().trim());
        
        if (keyAuth.isSelected()) {
            host.setAuthType(HostInfo.AuthType.KEY);
            host.setPrivateKeyPath(keyPathField.getText().trim());
            host.setPassphrase(passphraseField.getText());
            host.setPassword(null);
        } else {
            host.setAuthType(HostInfo.AuthType.PASSWORD);
            host.setPassword(passwordField.getText());
            host.setPrivateKeyPath(null);
            host.setPassphrase(null);
        }
        
        HostGroup selectedGroup = groupCombo.getValue();
        host.setGroupId(selectedGroup != null ? selectedGroup.getId() : null);
        
        if (existingHost != null) {
            configService.updateHost(host);
        } else {
            configService.addHost(host);
        }
        
        mainController.loadHosts();
        closeDialog();
    }
    
    @FXML
    private void onCancel() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );
        alert.showAndWait();
    }
}
