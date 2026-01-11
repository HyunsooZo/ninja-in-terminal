package com.ninja.terminal.controller;

import com.ninja.terminal.model.HostGroup;
import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;
    @FXML private TabPane mainTabs;
    @FXML private Label statusLabel;

    private TreeView<Object> hostTree;
    private TabPane terminalTabs;
    private Button addHostBtn;
    private Button addGroupBtn;
    private TextField searchField;

    private final ConfigService configService = ConfigService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Store MainController reference in rootPane properties for child controllers
        if (rootPane != null) {
            rootPane.getProperties().put("mainController", this);
        }

        findHostsViewComponents();
        setupHostTree();
        setupContextMenu();
        setupCommandPalette();
        loadHosts();

        // Double-click to connect
        if (hostTree != null) {
            hostTree.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    TreeItem<Object> selected = hostTree.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.getValue() instanceof HostInfo host) {
                        connectToHost(host);
                    }
                }
            });
        }

        // Search filter
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newVal) -> filterHosts(newVal));
        }

        // [수정됨] FXML에서 onAction을 제거했으므로 여기서 핸들러 연결
        if (addHostBtn != null) {
            addHostBtn.setOnAction(e -> onAddHost());
        }
        if (addGroupBtn != null) {
            addGroupBtn.setOnAction(e -> onAddGroup());
        }
    }

    private void findHostsViewComponents() {
        if (mainTabs.getTabs().isEmpty()) return;

        Tab hostsTab = mainTabs.getTabs().get(0);
        if (hostsTab != null && hostsTab.getContent() instanceof SplitPane hostsSplitPane) {
            // SplitPane의 아이템 가져오기
            if (hostsSplitPane.getItems().size() < 2) return;

            var leftItem = hostsSplitPane.getItems().get(0);
            var rightItem = hostsSplitPane.getItems().get(1);

            if (!(leftItem instanceof VBox leftPane) || !(rightItem instanceof VBox rightPane)) {
                log.warn("Unexpected pane types in SplitPane");
                return;
            }

            // [중요 수정] FXML 구조에 맞게 인덱스 조정 및 안전한 타입 체크
            // 0: Label, 1: TextField, 2: HBox(Buttons), 3: TreeView
            if (leftPane.getChildren().size() > 3) {
                if (leftPane.getChildren().get(1) instanceof TextField field) {
                    searchField = field;
                }

                if (leftPane.getChildren().get(2) instanceof HBox buttonBox) {
                    if (buttonBox.getChildren().size() >= 2) {
                        if (buttonBox.getChildren().get(0) instanceof Button btn) {
                            addHostBtn = btn;
                        }
                        if (buttonBox.getChildren().get(1) instanceof Button btn) {
                            addGroupBtn = btn;
                        }
                    }
                }

                if (leftPane.getChildren().get(3) instanceof TreeView<?> tree) {
                    @SuppressWarnings("unchecked")
                    TreeView<Object> objTree = (TreeView<Object>) tree;
                    hostTree = objTree;
                }
            }

            // 오른쪽 패널의 첫 번째 자식이 Terminal Tabs
            if (!rightPane.getChildren().isEmpty()) {
                if (rightPane.getChildren().getFirst() instanceof TabPane tabs) {
                    terminalTabs = tabs;
                }
            }
        }
    }

    private void setupHostTree() {
        if (hostTree == null) return;

        TreeItem<Object> root = new TreeItem<>("Hosts");
        root.setExpanded(true);
        hostTree.setRoot(root);
        hostTree.setShowRoot(false);

        hostTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof HostInfo host) {
                    setText(host.getName() != null ? host.getName() : host.getHostname());
                    setStyle("-fx-text-fill: #e0e0e0;");
                } else if (item instanceof HostGroup group) {
                    setText(group.getName());
                    setStyle("-fx-text-fill: #80cbc4; -fx-font-weight: bold;");
                } else {
                    setText(item.toString());
                }
            }
        });
    }

    private void setupContextMenu() {
        if (hostTree == null) return;

        // Dynamic context menu based on selection
        hostTree.setOnContextMenuRequested(event -> {
            TreeItem<Object> selected = hostTree.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;

            ContextMenu contextMenu = new ContextMenu();
            Object value = selected.getValue();

            if (value instanceof HostInfo host) {
                // Host context menu
                MenuItem connectItem = new MenuItem("Connect");
                connectItem.setOnAction(e -> connectToHost(host));

                MenuItem editItem = new MenuItem("Edit");
                editItem.setOnAction(e -> editHost(host));

                MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setOnAction(e -> deleteHost(host));

                contextMenu.getItems().addAll(connectItem, new SeparatorMenuItem(), editItem, deleteItem);
            } else if (value instanceof HostGroup group) {
                // Group context menu
                MenuItem renameItem = new MenuItem("Rename");
                renameItem.setOnAction(e -> renameGroup(group));

                MenuItem deleteItem = new MenuItem("Delete Group");
                deleteItem.setOnAction(e -> deleteGroup(group));

                contextMenu.getItems().addAll(renameItem, deleteItem);
            }

            contextMenu.show(hostTree, event.getScreenX(), event.getScreenY());
        });
    }

    private void setupCommandPalette() {
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.J && event.isControlDown()) {
                showCommandPalette();
                event.consume();
            }
        });
    }

    private void showCommandPalette() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CommandPaletteView.fxml"));
            VBox content = loader.load();

            CommandPaletteController controller = loader.getController();
            controller.setMainController(this);
            controller.setOnHostSelected(hostName -> {
                configService.getHosts().stream()
                        .filter(h -> hostName.equals(h.getName()) || hostName.equals(h.getHostname()))
                        .findFirst()
                        .ifPresent(this::connectToHost);
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());

            Scene scene = new Scene(content);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dark-theme.css")).toExternalForm());
            stage.setScene(scene);

            controller.show();
            stage.show();

        } catch (IOException e) {
            log.error("Failed to open command palette", e);
        }
    }

    public void loadHosts() {
        if (hostTree == null) return;

        TreeItem<Object> root = hostTree.getRoot();
        root.getChildren().clear();

        // Add groups
        for (HostGroup group : configService.getGroups()) {
            TreeItem<Object> groupItem = new TreeItem<>(group);
            groupItem.setExpanded(true);

            // Add hosts in this group
            configService.getHosts().stream()
                    .filter(h -> group.getId().equals(h.getGroupId()))
                    .forEach(h -> groupItem.getChildren().add(new TreeItem<>(h)));

            root.getChildren().add(groupItem);
        }

        // Add ungrouped hosts
        configService.getHosts().stream()
                .filter(h -> h.getGroupId() == null)
                .forEach(h -> root.getChildren().add(new TreeItem<>(h)));
    }

    private void filterHosts(String filter) {
        if (hostTree == null) return;

        if (filter == null || filter.isEmpty()) {
            loadHosts();
            return;
        }

        String lowerFilter = filter.toLowerCase();
        TreeItem<Object> root = hostTree.getRoot();
        root.getChildren().clear();

        configService.getHosts().stream()
                .filter(h -> {
                    String name = h.getName() != null ? h.getName().toLowerCase() : "";
                    String hostname = h.getHostname() != null ? h.getHostname().toLowerCase() : "";
                    return name.contains(lowerFilter) || hostname.contains(lowerFilter);
                })
                .forEach(h -> root.getChildren().add(new TreeItem<>(h)));
    }

    private void onAddHost() {
        showHostDialog(null);
    }

    private void onAddGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Group");
        dialog.setHeaderText("Create a new group");
        dialog.setContentText("Group name:");

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                HostGroup group = new HostGroup(name.trim());
                configService.addGroup(group);
                loadHosts();
            }
        });
    }

    private void showHostDialog(HostInfo existingHost) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HostDialog.fxml"));
            Parent root = loader.load();

            HostDialogController controller = loader.getController();
            controller.setMainController(this);

            if (existingHost != null) {
                controller.setHost(existingHost);
            }

            Stage stage = new Stage();
            stage.setTitle(existingHost == null ? "Add Host" : "Edit Host");
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dark-theme.css")).toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            log.error("Failed to open host dialog", e);
        }
    }

    public void connectToHost(HostInfo host) {
        if (terminalTabs == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TerminalTab.fxml"));
            VBox terminalContent = loader.load();

            TerminalTabController controller = loader.getController();

            Tab tab = new Tab(host.getName() != null ? host.getName() : host.getHostname());
            tab.setContent(terminalContent);
            tab.setOnClosed(e -> controller.disconnect());

            // Store controller reference in tab properties for later access
            tab.setUserData(controller);

            // Set callback to close tab on connection failure
            controller.setOnConnectionFailed(() -> {
                Platform.runLater(() -> {
                    // Wait a bit to show error message before closing
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // Show error for 3 seconds
                            Platform.runLater(() -> terminalTabs.getTabs().remove(tab));
                        } catch (InterruptedException ex) {
                            log.warn("Interrupted while waiting to close failed tab", ex);
                        }
                    }).start();
                });
            });

            terminalTabs.getTabs().add(tab);
            terminalTabs.getSelectionModel().select(tab);

            // Start connection in background
            controller.connect(host);

            statusLabel.setText("Connecting to " + host.getHostname());

        } catch (IOException e) {
            log.error("Failed to create terminal tab", e);
            showError("Connection Failed", "Could not create terminal: " + e.getMessage());
        }
    }

    /**
     * Get the currently active terminal tab controller
     * @return TerminalTabController or null if no terminal is active
     */
    public TerminalTabController getActiveTerminalController() {
        if (terminalTabs == null) return null;

        Tab selectedTab = terminalTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getUserData() == null) {
            return null;
        }

        return (TerminalTabController) selectedTab.getUserData();
    }

    /**
     * Get all terminal tab controllers
     * @return List of all terminal controllers
     */
    public java.util.List<TerminalTabController> getAllTerminalControllers() {
        if (terminalTabs == null) return java.util.Collections.emptyList();

        return terminalTabs.getTabs().stream()
                .map(Tab::getUserData)
                .filter(data -> data instanceof TerminalTabController)
                .map(data -> (TerminalTabController) data)
                .toList();
    }

    private void editHost(HostInfo host) {
        showHostDialog(host);
    }

    private void deleteHost(HostInfo host) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Host");
        alert.setHeaderText("Delete " + host.getName() + "?");
        alert.setContentText("This action cannot be undone.");
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                configService.deleteHost(host.getId());
                loadHosts();
            }
        });
    }

    private void renameGroup(HostGroup group) {
        TextInputDialog dialog = new TextInputDialog(group.getName());
        dialog.setTitle("Rename Group");
        dialog.setHeaderText("Rename group");
        dialog.setContentText("New name:");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(group.getName())) {
                group.setName(newName.trim());
                configService.save();
                loadHosts();
            }
        });
    }

    private void deleteGroup(HostGroup group) {
        // Count hosts in this group
        long hostCount = configService.getHosts().stream()
                .filter(h -> group.getId().equals(h.getGroupId()))
                .count();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Group");
        alert.setHeaderText("Delete group '" + group.getName() + "'?");

        if (hostCount > 0) {
            alert.setContentText(
                    "This group contains " + hostCount + " host(s).\n" +
                    "The hosts will be moved to ungrouped.\n\n" +
                    "This action cannot be undone."
            );
        } else {
            alert.setContentText("This action cannot be undone.");
        }

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                configService.deleteGroup(group.getId());
                loadHosts();
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/dark-theme.css")).toExternalForm()
        );
        alert.showAndWait();
    }

    public Button getAddHostBtn() {
        return addHostBtn;
    }

    public void setAddHostBtn(Button addHostBtn) {
        this.addHostBtn = addHostBtn;
    }

    public Button getAddGroupBtn() {
        return addGroupBtn;
    }

    public void setAddGroupBtn(Button addGroupBtn) {
        this.addGroupBtn = addGroupBtn;
    }
}