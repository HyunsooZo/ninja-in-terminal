package com.ninja.terminal.controller;

import com.ninja.terminal.model.HostInfo;
import com.ninja.terminal.service.ConfigService;
import com.ninja.terminal.util.FuzzySearch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CommandPaletteController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CommandPaletteController.class);

    @FXML private VBox rootPane;
    @FXML private TextField searchField;
    @FXML private ListView<CommandItem> resultListView;

    private final ConfigService configService = ConfigService.getInstance();
    private MainController mainController;
    private Consumer<String> onHostSelected;
    private Consumer<Integer> onTabSelected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchField();
        setupListView();
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((obs, old, newVal) -> performSearch(newVal));

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.DOWN) {
                if (!resultListView.getItems().isEmpty()) {
                    resultListView.getSelectionModel().select(0);
                    resultListView.requestFocus();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                if (!resultListView.getItems().isEmpty()) {
                    executeSelected();
                }
            }
        });

        searchField.requestFocus();
    }

    private void setupListView() {
        resultListView.setCellFactory(listView -> new CommandItemCell());

        resultListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
            } else if (event.getCode() == KeyCode.UP) {
                int selectedIndex = resultListView.getSelectionModel().getSelectedIndex();
                if (selectedIndex == 0) {
                    searchField.requestFocus();
                    event.consume();
                }
            }
        });

        resultListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                executeSelected();
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            showQuickActions();
            return;
        }

        List<String> hostNames = configService.getHosts().stream()
                .map(host -> host.getName() != null ? host.getName() : host.getHostname())
                .toList();

        List<FuzzySearch.MatchResult> hostResults = FuzzySearch.search(query, hostNames);

        ObservableList<CommandItem> items = FXCollections.observableArrayList();
        for (FuzzySearch.MatchResult result : hostResults) {
            items.add(new CommandItem(
                    CommandItemType.HOST,
                    result.getItem(),
                    "Connect to host",
                    result.getMatchedIndices()
            ));
        }

        resultListView.setItems(items);
    }

    private void showQuickActions() {
        ObservableList<CommandItem> items = FXCollections.observableArrayList();
        items.add(new CommandItem(CommandItemType.ACTION, "Show all hosts", "View all hosts", List.of()));
        items.add(new CommandItem(CommandItemType.ACTION, "New host", "Create a new host", List.of()));
        resultListView.setItems(items);
    }

    private void executeSelected() {
        CommandItem selected = resultListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        switch (selected.getType()) {
            case HOST:
                if (onHostSelected != null) {
                    onHostSelected.accept(selected.getTitle());
                }
                break;
            case ACTION:
                if ("Show all hosts".equals(selected.getTitle())) {
                    close();
                }
                break;
        }

        close();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setOnHostSelected(Consumer<String> onHostSelected) {
        this.onHostSelected = onHostSelected;
    }

    public void setOnTabSelected(Consumer<Integer> onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    public void show() {
        searchField.clear();
        showQuickActions();
        searchField.requestFocus();
    }

    private void close() {
        if (rootPane.getParent() != null) {
            rootPane.getParent().getScene().getWindow().hide();
        }
    }

    public enum CommandItemType {
        HOST,
        TAB,
        ACTION
    }

    public static class CommandItem {
        private final CommandItemType type;
        private final String title;
        private final String subtitle;
        private final List<Integer> matchedIndices;

        public CommandItem(CommandItemType type, String title, String subtitle, List<Integer> matchedIndices) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.matchedIndices = matchedIndices;
        }

        public CommandItemType getType() { return type; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public List<Integer> getMatchedIndices() { return matchedIndices; }
    }

    private class CommandItemCell extends javafx.scene.control.ListCell<CommandItem> {
        @Override
        protected void updateItem(CommandItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                String highlightedTitle = FuzzySearch.highlightMatch(item.getTitle(), item.getMatchedIndices());
                String style = """
                        -fx-padding: 8 12;
                        -fx-background-color: #2d2d2d;
                        -fx-background-radius: 4;
                        -fx-border-color: #3d3d3d;
                        -fx-border-radius: 4;
                    """;
                
                if (isSelected()) {
                    style += """
                        -fx-background-color: #45a049;
                    """;
                }

                setStyle(style);
                setText(item.getTitle() + "\n" + item.getSubtitle());
            }
        }
    }
}
