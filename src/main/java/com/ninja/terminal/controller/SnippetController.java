package com.ninja.terminal.controller;

import com.ninja.terminal.model.SnippetInfo;
import com.ninja.terminal.model.SnippetPackage;
import com.ninja.terminal.service.SnippetService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SnippetController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SnippetController.class);

    @FXML private TreeView<Object> snippetTree;
    @FXML private ListView<SnippetInfo> snippetListView;
    @FXML private TextArea scriptTextArea;
    @FXML private Button addSnippetBtn;
    @FXML private Button addPackageBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button runBtn;
    @FXML private Button copyBtn;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;

    private final SnippetService snippetService = SnippetService.getInstance();
    private SnippetInfo currentSnippet;
    private MainController mainController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSnippetTree();
        setupSnippetList();
        setupButtons();
        loadSnippets();

        // Find and inject MainController after scene is set up
        Platform.runLater(this::findMainController);
    }

    private void findMainController() {
        // Try to find MainController from parent hierarchy or properties
        if (snippetTree != null && snippetTree.getScene() != null) {
            var root = snippetTree.getScene().getRoot();
            if (root != null && root.getProperties().containsKey("mainController")) {
                mainController = (MainController) root.getProperties().get("mainController");
                log.info("MainController injected into SnippetController");
            } else if (root instanceof BorderPane borderPane) {
                // Try to find it from the BorderPane properties
                if (borderPane.getProperties().containsKey("mainController")) {
                    mainController = (MainController) borderPane.getProperties().get("mainController");
                    log.info("MainController found in BorderPane properties");
                }
            }
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void setupSnippetTree() {
        TreeItem<Object> root = new TreeItem<>("Packages");
        root.setExpanded(true);
        snippetTree.setRoot(root);
        snippetTree.setShowRoot(false);

        snippetTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof SnippetPackage pkg) {
                    setText(pkg.getIcon() + " " + pkg.getName());
                    setStyle("-fx-text-fill: " + pkg.getColor() + "; -fx-font-weight: bold;");
                }
            }
        });

        snippetTree.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof SnippetPackage pkg) {
                filterSnippetsByPackage(pkg.getId());
            } else {
                showAllSnippets();
            }
        });
    }

    private void setupSnippetList() {
        snippetListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SnippetInfo snippet, boolean empty) {
                super.updateItem(snippet, empty);
                if (empty || snippet == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox();
                    Label nameLabel = new Label(snippet.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
                    hbox.getChildren().add(nameLabel);

                    if (snippet.getTags() != null && !snippet.getTags().isEmpty()) {
                        Label tagsLabel = new Label(" " + String.join(", ", snippet.getTags()));
                        tagsLabel.setStyle("-fx-text-fill: #888;");
                        hbox.getChildren().add(tagsLabel);
                    }

                    setGraphic(hbox);
                }
            }
        });

        snippetListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                showSnippetDetails(newVal);
            } else {
                clearSnippetDetails();
            }
        });
    }

    private void setupButtons() {
        addSnippetBtn.setOnAction(e -> showSnippetDialog(null));
        addPackageBtn.setOnAction(e -> showPackageDialog(null));
        editBtn.setOnAction(e -> editCurrentSnippet());
        deleteBtn.setOnAction(e -> deleteCurrentSnippet());
        runBtn.setOnAction(e -> runCurrentSnippet());
        copyBtn.setOnAction(e -> copyCurrentSnippet());

        updateButtonStates();
    }

    private void loadSnippets() {
        TreeItem<Object> root = snippetTree.getRoot();
        root.getChildren().clear();

        for (SnippetPackage pkg : snippetService.getPackages()) {
            TreeItem<Object> pkgItem = new TreeItem<>(pkg);
            pkgItem.setExpanded(true);
            root.getChildren().add(pkgItem);
        }

        TreeItem<Object> allItem = new TreeItem<>("All Snippets");
        allItem.setExpanded(true);
        root.getChildren().add(0, allItem);

        showAllSnippets();
    }

    private void showAllSnippets() {
        snippetListView.setItems(FXCollections.observableArrayList(snippetService.getSnippets()));
    }

    private void filterSnippetsByPackage(String packageId) {
        if (packageId == null) {
            showAllSnippets();
        } else {
            snippetListView.setItems(FXCollections.observableArrayList(
                    snippetService.getSnippetsByPackage(packageId)
            ));
        }
    }

    private void showSnippetDetails(SnippetInfo snippet) {
        currentSnippet = snippet;
        titleLabel.setText(snippet.getName());
        descriptionLabel.setText(snippet.getDescription() != null ? snippet.getDescription() : "");
        scriptTextArea.setText(snippet.getScript());
        updateButtonStates();
    }

    private void clearSnippetDetails() {
        currentSnippet = null;
        titleLabel.setText("");
        descriptionLabel.setText("");
        scriptTextArea.clear();
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSnippet = currentSnippet != null;
        editBtn.setDisable(!hasSnippet);
        deleteBtn.setDisable(!hasSnippet);
        runBtn.setDisable(!hasSnippet);
        copyBtn.setDisable(!hasSnippet);
    }

    private void editCurrentSnippet() {
        if (currentSnippet != null) {
            showSnippetDialog(currentSnippet);
        }
    }

    private void deleteCurrentSnippet() {
        if (currentSnippet != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Snippet");
            alert.setHeaderText("Delete " + currentSnippet.getName() + "?");
            alert.setContentText("This action cannot be undone.");
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/dark-theme.css").toExternalForm()
            );

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    snippetService.deleteSnippet(currentSnippet.getId());
                    loadSnippets();
                    clearSnippetDetails();
                }
            });
        }
    }

    private void runCurrentSnippet() {
        if (currentSnippet == null) {
            return;
        }

        if (mainController == null) {
            showAlert("Error", "Main controller not initialized");
            return;
        }

        // Get the active terminal
        TerminalTabController activeTerminal = mainController.getActiveTerminalController();

        if (activeTerminal == null || !activeTerminal.isConnected()) {
            // No active terminal - ask user to select or connect to host first
            showRunOptions();
        } else {
            // Execute in active terminal
            executeInTerminal(activeTerminal);
        }
    }

    private void executeInTerminal(TerminalTabController terminal) {
        if (currentSnippet == null || currentSnippet.getScript() == null) {
            return;
        }

        terminal.executeCommand(currentSnippet.getScript());
        log.info("Executed snippet '{}' in terminal", currentSnippet.getName());

        // Switch to terminal tab to show execution
        if (mainController != null) {
            // Optionally switch to terminal tabs (you can comment this out if not desired)
            Platform.runLater(() -> {
                showAlert("Snippet Executed",
                        String.format("Snippet '%s' has been executed in the active terminal.", currentSnippet.getName()));
            });
        }
    }

    private void showRunOptions() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("No Active Terminal");
        alert.setHeaderText("No terminal is currently active");
        alert.setContentText("Would you like to run this snippet on all connected terminals, or connect to a host first?");

        ButtonType runOnAll = new ButtonType("Run on All");
        ButtonType selectHost = new ButtonType("Select Host");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(runOnAll, selectHost, cancel);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == runOnAll) {
                runOnAllTerminals();
            } else if (response == selectHost) {
                showHostSelectionDialog();
            }
        });
    }

    private void runOnAllTerminals() {
        if (mainController == null) return;

        java.util.List<TerminalTabController> terminals = mainController.getAllTerminalControllers();

        if (terminals.isEmpty()) {
            showAlert("No Terminals", "No terminals are currently connected. Please connect to a host first.");
            return;
        }

        int count = 0;
        for (TerminalTabController terminal : terminals) {
            if (terminal.isConnected()) {
                executeInTerminal(terminal);
                count++;
            }
        }

        showAlert("Snippet Executed",
                String.format("Snippet '%s' executed on %d terminal(s).", currentSnippet.getName(), count));
    }

    private void showHostSelectionDialog() {
        if (mainController == null) return;

        // Get list of hosts
        com.ninja.terminal.service.ConfigService configService = com.ninja.terminal.service.ConfigService.getInstance();
        java.util.List<com.ninja.terminal.model.HostInfo> hosts = configService.getHosts();

        if (hosts.isEmpty()) {
            showAlert("No Hosts", "No hosts configured. Please add a host first.");
            return;
        }

        // Create choice dialog
        ChoiceDialog<com.ninja.terminal.model.HostInfo> dialog = new ChoiceDialog<>(hosts.get(0), hosts);
        dialog.setTitle("Select Host");
        dialog.setHeaderText("Connect to host and run snippet");
        dialog.setContentText("Choose host:");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(host -> {
            // Connect to host
            mainController.connectToHost(host);

            // Wait a bit for connection, then execute
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds for connection
                    Platform.runLater(() -> {
                        TerminalTabController terminal = mainController.getActiveTerminalController();
                        if (terminal != null && terminal.isConnected()) {
                            executeInTerminal(terminal);
                        } else {
                            showAlert("Connection Failed", "Could not connect to host or connection not ready.");
                        }
                    });
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for connection", e);
                }
            }).start();
        });
    }

    private void copyCurrentSnippet() {
        if (currentSnippet != null && currentSnippet.getScript() != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(currentSnippet.getScript());
            clipboard.setContent(content);
            showAlert("Copied", "Snippet copied to clipboard");
        }
    }

    private void showSnippetDialog(SnippetInfo existingSnippet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SnippetDialog.fxml"));
            Parent root = loader.load();

            SnippetDialogController controller = loader.getController();
            controller.setSnippet(existingSnippet);

            Stage stage = new Stage();
            stage.setTitle(existingSnippet == null ? "Add Snippet" : "Edit Snippet");
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
            stage.setScene(scene);

            stage.showAndWait();

            if (controller.isSaved()) {
                SnippetInfo snippet = controller.getSnippet();
                if (existingSnippet == null) {
                    snippetService.addSnippet(snippet);
                } else {
                    snippetService.updateSnippet(snippet);
                }
                loadSnippets();
            }

        } catch (IOException e) {
            log.error("Failed to open snippet dialog", e);
        }
    }

    private void showPackageDialog(SnippetPackage existingPackage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(existingPackage == null ? "New Package" : "Edit Package");
        dialog.setHeaderText(existingPackage == null ? "Create a new package" : "Edit package name");
        dialog.setContentText("Package name:");

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        if (existingPackage != null) {
            dialog.getEditor().setText(existingPackage.getName());
        }

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                if (existingPackage == null) {
                    SnippetPackage newPackage = new SnippetPackage(name.trim());
                    snippetService.addPackage(newPackage);
                } else {
                    existingPackage.setName(name.trim());
                    snippetService.updatePackage(existingPackage);
                }
                loadSnippets();
            }
        });
    }

    private void showAlert(String title, String message) {
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
