package com.ninja.terminal.controller;

import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.model.RemoteFile;
import com.ninja.terminal.service.ConfigService;
import com.ninja.terminal.service.SftpService;
import com.ninja.terminal.service.SshService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SftpController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SftpController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private ComboBox<HostInfo> hostCombo;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;
    @FXML private Label connectionStatus;
    @FXML private Button backBtn;
    @FXML private TextField pathField;
    @FXML private Button refreshBtn;
    @FXML private Button uploadBtn;
    @FXML private Button downloadBtn;
    @FXML private Button newFolderBtn;
    @FXML private Button deleteBtn;
    @FXML private TableView<RemoteFile> fileTable;
    @FXML private TableColumn<RemoteFile, String> nameColumn;
    @FXML private TableColumn<RemoteFile, String> sizeColumn;
    @FXML private TableColumn<RemoteFile, String> permissionsColumn;
    @FXML private TableColumn<RemoteFile, String> modifiedColumn;
    @FXML private Label statusLabel;
    @FXML private Label fileCountLabel;

    private final ConfigService configService = ConfigService.getInstance();
    private final SshService sshService = new SshService();
    private final SftpService sftpService = new SftpService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHostCombo();
        setupFileTable();
        setupButtons();
        setupContextMenu();
    }

    private void setupHostCombo() {
        List<HostInfo> hosts = configService.getHosts();
        hostCombo.setItems(FXCollections.observableArrayList(hosts));

        // Display host name in combo box
        hostCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HostInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        hostCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(HostInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void setupFileTable() {
        // Name column with icon
        nameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFilename())
        );

        // Size column
        sizeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedSize())
        );

        // Permissions column
        permissionsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPermissionString())
        );

        // Modified time column
        modifiedColumn.setCellValueFactory(cellData -> {
            RemoteFile file = cellData.getValue();
            String formatted = file.getModifiedTime() != null ?
                file.getModifiedTime().format(DATE_FORMATTER) : "-";
            return new SimpleStringProperty(formatted);
        });

        // Double-click to open directory
        fileTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                RemoteFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
                if (selectedFile != null && selectedFile.isDirectory()) {
                    openDirectory(selectedFile);
                }
            }
        });

        // Selection listener for buttons
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            downloadBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });
    }

    private void setupButtons() {
        connectBtn.setOnAction(e -> onConnect());
        disconnectBtn.setOnAction(e -> onDisconnect());
        backBtn.setOnAction(e -> onBack());
        refreshBtn.setOnAction(e -> refreshFileList());
        uploadBtn.setOnAction(e -> onUpload());
        downloadBtn.setOnAction(e -> onDownload());
        newFolderBtn.setOnAction(e -> onNewFolder());
        deleteBtn.setOnAction(e -> onDelete());
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem downloadItem = new MenuItem("Download");
        downloadItem.setOnAction(e -> onDownload());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> onDelete());

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> onRename());

        MenuItem permissionsItem = new MenuItem("Change Permissions");
        permissionsItem.setOnAction(e -> onChangePermissions());

        contextMenu.getItems().addAll(downloadItem, new SeparatorMenuItem(),
                                      renameItem, permissionsItem, new SeparatorMenuItem(),
                                      deleteItem);

        fileTable.setContextMenu(contextMenu);
    }

    private void onConnect() {
        HostInfo selectedHost = hostCombo.getValue();
        if (selectedHost == null) {
            showError("No Host Selected", "Please select a host to connect to.");
            return;
        }

        connectBtn.setDisable(true);
        connectionStatus.setText("Connecting...");

        new Thread(() -> {
            try {
                // Connect SSH
                sshService.connect(selectedHost);

                // Connect SFTP
                sftpService.connect(sshService.getSession());

                Platform.runLater(() -> {
                    connectionStatus.setText("Connected to " + selectedHost.getName());
                    connectionStatus.setStyle("-fx-text-fill: #9ece6a;");
                    connectBtn.setDisable(true);
                    disconnectBtn.setDisable(false);
                    enableFileOperations(true);

                    refreshFileList();
                });

            } catch (Exception e) {
                log.error("Failed to connect SFTP", e);
                Platform.runLater(() -> {
                    connectionStatus.setText("Connection failed");
                    connectionStatus.setStyle("-fx-text-fill: #f7768e;");
                    connectBtn.setDisable(false);
                    showError("Connection Failed", "Could not connect to " + selectedHost.getName() + ": " + e.getMessage());
                });
            }
        }).start();
    }

    private void onDisconnect() {
        sftpService.disconnect();
        sshService.disconnect();

        connectionStatus.setText("Not connected");
        connectionStatus.setStyle("");
        connectBtn.setDisable(false);
        disconnectBtn.setDisable(true);
        enableFileOperations(false);

        fileTable.getItems().clear();
        pathField.setText("");
        statusLabel.setText("Disconnected");
    }

    private void enableFileOperations(boolean enable) {
        backBtn.setDisable(!enable);
        refreshBtn.setDisable(!enable);
        uploadBtn.setDisable(!enable);
        newFolderBtn.setDisable(!enable);
        // download and delete are controlled by selection
    }

    private void refreshFileList() {
        if (!sftpService.isConnected()) {
            return;
        }

        statusLabel.setText("Loading...");

        new Thread(() -> {
            try {
                List<RemoteFile> files = sftpService.listFiles();
                String currentPath = sftpService.getCurrentPath();

                Platform.runLater(() -> {
                    fileTable.setItems(FXCollections.observableArrayList(files));
                    pathField.setText(currentPath);
                    fileCountLabel.setText(files.size() + " items");
                    statusLabel.setText("Ready");
                });

            } catch (Exception e) {
                log.error("Failed to list files", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    showError("Failed to List Files", e.getMessage());
                });
            }
        }).start();
    }

    private void openDirectory(RemoteFile directory) {
        String newPath = directory.getFullPath();

        new Thread(() -> {
            try {
                sftpService.changeDirectory(newPath);
                Platform.runLater(this::refreshFileList);
            } catch (Exception e) {
                log.error("Failed to change directory", e);
                Platform.runLater(() -> showError("Failed to Open Directory", e.getMessage()));
            }
        }).start();
    }

    private void onBack() {
        if (!sftpService.isConnected()) {
            return;
        }

        new Thread(() -> {
            try {
                sftpService.goToParentDirectory();
                Platform.runLater(this::refreshFileList);
            } catch (Exception e) {
                log.error("Failed to go to parent directory", e);
                Platform.runLater(() -> showError("Failed to Go Back", e.getMessage()));
            }
        }).start();
    }

    private void onUpload() {
        if (!sftpService.isConnected()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(uploadBtn.getScene().getWindow());

        if (file == null) {
            return;
        }

        statusLabel.setText("Uploading " + file.getName() + "...");

        new Thread(() -> {
            try {
                String remotePath = sftpService.getCurrentPath();
                if (!remotePath.endsWith("/")) {
                    remotePath += "/";
                }
                remotePath += file.getName();

                sftpService.uploadFile(file.getAbsolutePath(), remotePath);

                Platform.runLater(() -> {
                    statusLabel.setText("Uploaded " + file.getName());
                    refreshFileList();
                });

            } catch (Exception e) {
                log.error("Failed to upload file", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Upload failed");
                    showError("Upload Failed", e.getMessage());
                });
            }
        }).start();
    }

    private void onDownload() {
        RemoteFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null || selectedFile.isDirectory()) {
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Download Location");
        File directory = dirChooser.showDialog(downloadBtn.getScene().getWindow());

        if (directory == null) {
            return;
        }

        statusLabel.setText("Downloading " + selectedFile.getFilename() + "...");

        new Thread(() -> {
            try {
                String localPath = directory.getAbsolutePath() + File.separator + selectedFile.getFilename();
                sftpService.downloadFile(selectedFile.getFullPath(), localPath);

                Platform.runLater(() -> {
                    statusLabel.setText("Downloaded " + selectedFile.getFilename());
                    showInfo("Download Complete", "File saved to: " + localPath);
                });

            } catch (Exception e) {
                log.error("Failed to download file", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Download failed");
                    showError("Download Failed", e.getMessage());
                });
            }
        }).start();
    }

    private void onNewFolder() {
        if (!sftpService.isConnected()) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder");
        dialog.setContentText("Folder name:");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(folderName -> {
            if (folderName.trim().isEmpty()) {
                return;
            }

            new Thread(() -> {
                try {
                    String newPath = sftpService.getCurrentPath();
                    if (!newPath.endsWith("/")) {
                        newPath += "/";
                    }
                    newPath += folderName;

                    sftpService.createDirectory(newPath);

                    Platform.runLater(() -> {
                        statusLabel.setText("Created folder: " + folderName);
                        refreshFileList();
                    });

                } catch (Exception e) {
                    log.error("Failed to create folder", e);
                    Platform.runLater(() -> showError("Failed to Create Folder", e.getMessage()));
                }
            }).start();
        });
    }

    private void onDelete() {
        RemoteFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete " + (selectedFile.isDirectory() ? "Folder" : "File"));
        alert.setHeaderText("Delete " + selectedFile.getFilename() + "?");
        alert.setContentText("This action cannot be undone.");
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        if (selectedFile.isDirectory()) {
                            sftpService.deleteDirectory(selectedFile.getFullPath());
                        } else {
                            sftpService.deleteFile(selectedFile.getFullPath());
                        }

                        Platform.runLater(() -> {
                            statusLabel.setText("Deleted: " + selectedFile.getFilename());
                            refreshFileList();
                        });

                    } catch (Exception e) {
                        log.error("Failed to delete", e);
                        Platform.runLater(() -> showError("Delete Failed", e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void onRename() {
        RemoteFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedFile.getFilename());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + selectedFile.getFilename());
        dialog.setContentText("New name:");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty() || newName.equals(selectedFile.getFilename())) {
                return;
            }

            new Thread(() -> {
                try {
                    String currentPath = selectedFile.getPath();
                    if (!currentPath.endsWith("/")) {
                        currentPath += "/";
                    }

                    String oldPath = selectedFile.getFullPath();
                    String newPath = currentPath + newName;

                    sftpService.rename(oldPath, newPath);

                    Platform.runLater(() -> {
                        statusLabel.setText("Renamed to: " + newName);
                        refreshFileList();
                    });

                } catch (Exception e) {
                    log.error("Failed to rename", e);
                    Platform.runLater(() -> showError("Rename Failed", e.getMessage()));
                }
            }).start();
        });
    }

    private void onChangePermissions() {
        RemoteFile selectedFile = fileTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return;
        }

        // Simple permission dialog with octal input
        TextInputDialog dialog = new TextInputDialog(Integer.toOctalString(selectedFile.getPermissions()));
        dialog.setTitle("Change Permissions");
        dialog.setHeaderText("Change permissions for " + selectedFile.getFilename());
        dialog.setContentText("Permissions (octal, e.g., 755):");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(permStr -> {
            try {
                int permissions = Integer.parseInt(permStr, 8);

                new Thread(() -> {
                    try {
                        sftpService.chmod(selectedFile.getFullPath(), permissions);

                        Platform.runLater(() -> {
                            statusLabel.setText("Changed permissions: " + permStr);
                            refreshFileList();
                        });

                    } catch (Exception e) {
                        log.error("Failed to change permissions", e);
                        Platform.runLater(() -> showError("Failed to Change Permissions", e.getMessage()));
                    }
                }).start();

            } catch (NumberFormatException e) {
                showError("Invalid Permissions", "Please enter a valid octal number (e.g., 755).");
            }
        });
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
