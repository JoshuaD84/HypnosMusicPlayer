package net.joshuad.hypnos.fxui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class EditOnClickCell<S, T> extends TableCell<S, T> {

	private final TextField textField = new TextField();
	private final StringConverter<T> converter;

	public EditOnClickCell(StringConverter<T> converter) {
		textField.setMaxHeight(5);
		textField.setPrefHeight(5);
		this.converter = converter;
		itemProperty().addListener((obx, oldItem, newItem) -> {
			setText(converter.toString(newItem));
		});
		setGraphic(textField);
		setContentDisplay(ContentDisplay.TEXT_ONLY);
		
		Platform.runLater(() -> {
			getTableView().setOnKeyPressed(event -> {
				TablePosition<T, ?> pos = getTableView().getFocusModel().getFocusedCell();
				if (pos != null && event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
					beginEditing();
				}
			});
			getTableView().getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
				if (newSelection != null) {
					beginEditing();
				}
			});
		});
		textField.setOnAction(evt -> {
			commitEdit(this.converter.fromString(textField.getText()));
		});
		textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				commitEdit((T)textField.getText());
				setText(textField.getText());
				cancelEdit();
			}
		});
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				textField.setText(converter.toString(getItem()));
				cancelEdit();
				event.consume();
			} else if (event.getCode() == KeyCode.UP || (event.getCode() == KeyCode.TAB && event.isShiftDown())) {
				commitEdit((T)textField.getText());
				cancelEdit();
				getTableView().getSelectionModel().selectAboveCell();
				event.consume();
			} else if (event.getCode() == KeyCode.DOWN || (event.getCode() == KeyCode.TAB && !event.isShiftDown())) {
				commitEdit((T)textField.getText());
				this.cancelEdit();
				getTableView().getSelectionModel().selectBelowCell();
				event.consume();
			}
		});
	}

	private void beginEditing() {
		int row = getTableView().getSelectionModel().getSelectedCells().get(0).getRow();
		TableColumn<S, ?> column = getTableView().getColumns().get(1);
		Platform.runLater(() -> {
			getTableView().edit(row, column);
			textField.selectAll();
		});
	}

	public static final StringConverter<String> IDENTITY_CONVERTER = new StringConverter<String>() {
		@Override
		public String toString(String object) {
			if ( object == null ) return "";
			else return object;
		}

		@Override
		public String fromString(String string) {
			if ( string == null ) return "";
			else return string;
		}
	};

	public static <S> EditOnClickCell<S, String> createStringEditCell() {
		return new EditOnClickCell<S, String>(IDENTITY_CONVERTER);
	}

	@Override
	public void startEdit() {
		super.startEdit();
		textField.setText(converter.toString(getItem()));
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		textField.requestFocus();
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}
	@Override
	public void commitEdit(T item) {

		// This block is necessary to support commit on losing focus, because the
		// baked-in mechanism
		// sets our editing state to false before we can intercept the loss of focus.
		// The default commitEdit(...) method simply bails if we are not editing...
		if (!isEditing() && !item.equals(getItem())) {
			TableView<S> table = getTableView();
			if (table != null) {
				TableColumn<S, T> column = getTableColumn();
				CellEditEvent<S, T> event = new CellEditEvent<>(table, new TablePosition<S, T>(table, getIndex(), column),
						TableColumn.editCommitEvent(), item);
				Event.fireEvent(column, event);
			}
		}
		super.commitEdit(item);
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}
}