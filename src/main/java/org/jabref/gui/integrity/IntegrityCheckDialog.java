package org.jabref.gui.integrity;

import java.util.List;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.controlsfx.control.table.TableFilter;

public class IntegrityCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<IntegrityMessage> messagesTable;
    @FXML private TableColumn<IntegrityMessage, String> keyColumn;
    @FXML private TableColumn<IntegrityMessage, String> fieldColumn;
    @FXML private TableColumn<IntegrityMessage, String> messageColumn;
    @FXML private MenuButton keyFilterButton;
    @FXML private MenuButton fieldFilterButton;
    @FXML private MenuButton messageFilterButton;

    @Inject private EntryEditor entryEditor;
    @Inject private ThemeManager themeManager;
    @Inject private StateManager stateManager;

    private final List<IntegrityMessage> messages;
    private final LibraryTab libraryTab;
    private IntegrityCheckDialogViewModel viewModel;
    private TableFilter<IntegrityMessage> tableFilter;

    public IntegrityCheckDialog(List<IntegrityMessage> messages, LibraryTab libraryTab) {
        this.messages = messages;
        this.libraryTab = libraryTab;
        this.setTitle(Localization.lang("Check integrity"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    private void handleRowClick(IntegrityMessage message, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            libraryTab.clearAndSelect(message.entry());

            stateManager.getEditorShowing().setValue(true);

            // Focus field async to give entry editor time to load
            Platform.runLater(() -> entryEditor.setFocusToField(message.field()));
            if (event.getClickCount() == 2) {
                this.close();
            }
        }
    }

    public IntegrityCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new IntegrityCheckDialogViewModel(messages);

        new ViewModelTableRowFactory<IntegrityMessage>()
                .withOnMouseClickedEvent(this::handleRowClick)
                .install(messagesTable);
        messagesTable.setItems(viewModel.getMessages());
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().entry().getCitationKey().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().field().getDisplayName()));
        messageColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().message()));

        new ValueTableCellFactory<IntegrityMessage, String>()
                .withText(Function.identity())
                .withTooltip(Function.identity())
                .install(messageColumn);

        tableFilter = TableFilter.forTableView(messagesTable)
                                 .apply();

        addMessageColumnFilter(keyColumn, keyFilterButton);
        addMessageColumnFilter(fieldColumn, fieldFilterButton);
        addMessageColumnFilter(messageColumn, messageFilterButton);
    }

    private void addMessageColumnFilter(TableColumn<IntegrityMessage, String> messageColumn, MenuButton messageFilterButton) {
        tableFilter.getColumnFilter(messageColumn).ifPresent(columnFilter -> {
            ContextMenu messageContextMenu = messageColumn.getContextMenu();
            if (messageContextMenu != null) {
                messageFilterButton.setContextMenu(messageContextMenu);
                messageFilterButton.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (messageContextMenu.isShowing()) {
                            messageContextMenu.setX(event.getScreenX());
                            messageContextMenu.setY(event.getScreenY());
                        } else {
                            messageContextMenu.show(messageFilterButton, event.getScreenX(), event.getScreenY());
                        }
                    }
                });
            }
        });
    }

    public void clearFilters() {
        if (tableFilter != null) {
            tableFilter.resetFilter();
            messagesTable.getColumns().forEach(column -> {
                tableFilter.selectAllValues(column);
                column.setGraphic(null);
            });
        }
    }
}
