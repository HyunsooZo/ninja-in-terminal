package com.ninja.terminal.controller;

import com.ninja.terminal.model.SnippetInfo;
import com.ninja.terminal.model.SnippetPackage;
import com.ninja.terminal.service.SnippetService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SnippetDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SnippetDialogController.class);

    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private TextArea scriptTextArea;
    @FXML private ComboBox<String> packageComboBox;
    @FXML private TextField tagsField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private final SnippetService snippetService = SnippetService.getInstance();
    private SnippetInfo snippet;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPackages();
        setupButtons();
    }

    private void loadPackages() {
        packageComboBox.getItems().clear();
        packageComboBox.getItems().add("None");
        packageComboBox.getItems().addAll(
                snippetService.getPackages().stream()
                        .map(SnippetPackage::getName)
                        .collect(Collectors.toList())
        );
        packageComboBox.getSelectionModel().selectFirst();
    }

    private void setupButtons() {
        saveBtn.setOnAction(e -> saveSnippet());
        cancelBtn.setOnAction(e -> closeDialog());
    }

    public void setSnippet(SnippetInfo snippet) {
        this.snippet = snippet != null ? snippet : new SnippetInfo();

        if (snippet != null) {
            nameField.setText(snippet.getName());
            descriptionField.setText(snippet.getDescription());
            scriptTextArea.setText(snippet.getScript());

            if (snippet.getPackageId() != null) {
                snippetService.getPackageById(snippet.getPackageId()).ifPresent(pkg -> {
                    packageComboBox.getSelectionModel().select(pkg.getName());
                });
            }

            if (snippet.getTags() != null) {
                tagsField.setText(String.join(", ", snippet.getTags()));
            }
        }
    }

    private void saveSnippet() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String script = scriptTextArea.getText();
        String packageName = packageComboBox.getValue();
        String tagsText = tagsField.getText().trim();

        if (name.isEmpty()) {
            showError("Validation Error", "Snippet name is required");
            return;
        }

        if (script.isEmpty()) {
            showError("Validation Error", "Script is required");
            return;
        }

        snippet.setName(name);
        snippet.setDescription(description.isEmpty() ? null : description);
        snippet.setScript(script);

        if ("None".equals(packageName) || packageName == null) {
            snippet.setPackageId(null);
        } else {
            snippetService.getPackages().stream()
                    .filter(p -> p.getName().equals(packageName))
                    .findFirst()
                    .ifPresent(pkg -> snippet.setPackageId(pkg.getId()));
        }

        if (!tagsText.isEmpty()) {
            snippet.getTags().clear();
            for (String tag : tagsText.split(",")) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    snippet.getTags().add(trimmedTag);
                }
            }
        } else {
            snippet.getTags().clear();
        }

        saved = true;
        closeDialog();
    }

    private void closeDialog() {
        nameField.getScene().getWindow().hide();
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

    public SnippetInfo getSnippet() {
        return snippet;
    }

    public boolean isSaved() {
        return saved;
    }
}
