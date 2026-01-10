package com.ninja.terminal.controller;

import com.ninja.terminal.model.AppConfig;
import com.ninja.terminal.service.ConfigService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML private ComboBox<String> fontFamilyCombo;
    @FXML private Spinner<Integer> fontSizeSpinner;
    @FXML private Spinner<Integer> scrollBufferSpinner;
    @FXML private Spinner<Integer> defaultPortSpinner;
    @FXML private Spinner<Integer> connectionTimeoutSpinner;
    @FXML private ComboBox<String> themeCombo;
    @FXML private Button resetBtn;
    @FXML private Button saveBtn;

    private final ConfigService configService = ConfigService.getInstance();
    private AppConfig.Settings settings;

    // Common monospace fonts
    private static final List<String> FONT_FAMILIES = Arrays.asList(
        "JetBrains Mono",
        "Consolas",
        "Monaco",
        "Courier New",
        "Menlo",
        "Fira Code",
        "Source Code Pro",
        "Ubuntu Mono",
        "Inconsolata",
        "DejaVu Sans Mono"
    );

    private static final List<String> THEMES = Arrays.asList(
        "Dark",
        "Light" // Future implementation
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settings = configService.getSettings();

        setupFontFamilyCombo();
        setupSpinners();
        setupThemeCombo();
        setupButtons();

        loadSettings();
    }

    private void setupFontFamilyCombo() {
        fontFamilyCombo.setItems(FXCollections.observableArrayList(FONT_FAMILIES));
    }

    private void setupSpinners() {
        // Font Size Spinner (8-24)
        SpinnerValueFactory<Integer> fontSizeFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 24, 14);
        fontSizeSpinner.setValueFactory(fontSizeFactory);
        fontSizeSpinner.setEditable(true);

        // Scroll Buffer Size Spinner (1000-50000)
        SpinnerValueFactory<Integer> scrollBufferFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 50000, 10000, 1000);
        scrollBufferSpinner.setValueFactory(scrollBufferFactory);
        scrollBufferSpinner.setEditable(true);

        // Default Port Spinner (1-65535)
        SpinnerValueFactory<Integer> defaultPortFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 65535, 22);
        defaultPortSpinner.setValueFactory(defaultPortFactory);
        defaultPortSpinner.setEditable(true);

        // Connection Timeout Spinner (5000-60000)
        SpinnerValueFactory<Integer> timeoutFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5000, 60000, 30000, 1000);
        connectionTimeoutSpinner.setValueFactory(timeoutFactory);
        connectionTimeoutSpinner.setEditable(true);

        // Add text formatter for spinners to handle manual input
        addSpinnerTextFormatter(fontSizeSpinner);
        addSpinnerTextFormatter(scrollBufferSpinner);
        addSpinnerTextFormatter(defaultPortSpinner);
        addSpinnerTextFormatter(connectionTimeoutSpinner);
    }

    private void addSpinnerTextFormatter(Spinner<Integer> spinner) {
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) {
                try {
                    int value = Integer.parseInt(newValue);
                    SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
                    if (value >= ((SpinnerValueFactory.IntegerSpinnerValueFactory) factory).getMin() &&
                        value <= ((SpinnerValueFactory.IntegerSpinnerValueFactory) factory).getMax()) {
                        spinner.getValueFactory().setValue(value);
                    }
                } catch (NumberFormatException e) {
                    // Ignore empty string
                }
            }
        });
    }

    private void setupThemeCombo() {
        themeCombo.setItems(FXCollections.observableArrayList(THEMES));
    }

    private void setupButtons() {
        saveBtn.setOnAction(e -> onSave());
        resetBtn.setOnAction(e -> onReset());
    }

    private void loadSettings() {
        // Load current settings
        fontFamilyCombo.setValue(settings.getFontFamily());
        fontSizeSpinner.getValueFactory().setValue(settings.getFontSize());
        scrollBufferSpinner.getValueFactory().setValue(settings.getScrollBufferSize());
        defaultPortSpinner.getValueFactory().setValue(settings.getDefaultPort());
        connectionTimeoutSpinner.getValueFactory().setValue(settings.getConnectionTimeout());

        // Capitalize theme name for display
        String themeName = capitalizeFirst(settings.getTheme());
        themeCombo.setValue(themeName);
    }

    private void onSave() {
        try {
            // Get values from UI
            String fontFamily = fontFamilyCombo.getValue();
            int fontSize = fontSizeSpinner.getValue();
            int scrollBuffer = scrollBufferSpinner.getValue();
            int defaultPort = defaultPortSpinner.getValue();
            int timeout = connectionTimeoutSpinner.getValue();
            String theme = themeCombo.getValue() != null ? themeCombo.getValue().toLowerCase() : "dark";

            // Validate
            if (fontFamily == null || fontFamily.trim().isEmpty()) {
                showError("Validation Error", "Please select a font family.");
                return;
            }

            // Update settings
            settings.setFontFamily(fontFamily);
            settings.setFontSize(fontSize);
            settings.setScrollBufferSize(scrollBuffer);
            settings.setDefaultPort(defaultPort);
            settings.setConnectionTimeout(timeout);
            settings.setTheme(theme);

            // Save to file
            configService.save();

            log.info("Settings saved successfully");
            showSuccess("Settings Saved",
                "Your settings have been saved successfully.\n\n" +
                "Note: Some settings may require restarting terminals to take effect.");

        } catch (Exception e) {
            log.error("Failed to save settings", e);
            showError("Save Failed", "Could not save settings: " + e.getMessage());
        }
    }

    private void onReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset all settings to defaults?");
        alert.setContentText("This will restore all settings to their default values.");
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                resetToDefaults();
            }
        });
    }

    private void resetToDefaults() {
        // Reset to default values
        fontFamilyCombo.setValue("JetBrains Mono");
        fontSizeSpinner.getValueFactory().setValue(14);
        scrollBufferSpinner.getValueFactory().setValue(10000);
        defaultPortSpinner.getValueFactory().setValue(22);
        connectionTimeoutSpinner.getValueFactory().setValue(30000);
        themeCombo.setValue("Dark");

        log.info("Settings reset to defaults");
        showInfo("Settings Reset", "All settings have been reset to their default values.\n\nClick 'Save Changes' to apply.");
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );
        alert.showAndWait();
    }
}
