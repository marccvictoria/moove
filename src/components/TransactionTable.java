package components;


import model.Transaction;
import service.AgriSystem;
import util.Fmt;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;

/* 
 * The transaction history table. 
 * - It is reused by the Manager overview (clerk sales history), the clerk summary, and the wallet and transaction screen, 
 * - Voided rows are greyed out, and the "Void" button column only appears when a void handler is supplied.
 * 
 * References: https://www.geeksforgeeks.org/java/lambda-expressions-java-8/
 */

public final class TransactionTable extends TableView<Transaction> {
 
    // What to do when the Void button of a row is pressed.
    public interface VoidHandler {
        void voidTransaction(Transaction t);
    }
 
    public TransactionTable(AgriSystem system, VoidHandler voidHandler) {
        setPlaceholder(new Label("No transactions to show."));
 
        // one text column per detail of a transaction
        getColumns().add(Tables.text("#", 45, t -> Integer.toString(t.getId())));
        getColumns().add(Tables.text("When", 170, t -> Fmt.dateTime(t.getTimestamp())));
        getColumns().add(Tables.text("Type", 125, t -> t.getType()));
        getColumns().add(Tables.text("Details", 220, t -> t.getDetails()));
        getColumns().add(Tables.number("Amount", 105, t -> amountText(t)));
        getColumns().add(Tables.text("Recorded by", 105, t -> system.displayNameOf(t.getRecordedBy())));
        getColumns().add(Tables.text("Status", 110, t -> t.getStatus().getLabel()));
        if (voidHandler != null) {
            getColumns().add(makeVoidColumn(voidHandler));
        }
 
        // grey out voided rows
        setRowFactory(table -> new TableRow<Transaction>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().remove("row-voided");
                if (!empty && t != null && t.isVoided()) {
                    getStyleClass().add("row-voided");
                }
            }
        });
 
        // do not let a column shrink below its starting width (so text is not cut off)
        for (TableColumn<Transaction, ?> column : getColumns()) {
            column.setMinWidth(column.getPrefWidth());
        }
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
    }
 
    // shows these transactions (replaces the rows that are there now).
    public void setData(List<Transaction> transactions) {
        setItems(FXCollections.observableArrayList(transactions));
    }
 
    // no cash moved (for example an item donation) is shown as "-"
    private static String amountText(Transaction t) {
        if (t.getCashAmount() == 0) {
            return "-";
        }
        return Fmt.money(t.getCashAmount());
    }
 
    // the column that holds the Void button of each row
    private static TableColumn<Transaction, Transaction> makeVoidColumn(VoidHandler voidHandler) {
        TableColumn<Transaction, Transaction> column = new TableColumn<>("");
        column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        column.setSortable(false);
        column.setPrefWidth(80);
        column.setCellFactory(col -> new TableCell<Transaction, Transaction>() {
            private final Button button = new Button("Void");
 
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                // no button for empty rows, voided transactions, or ones that cannot be voided
                if (empty || t == null || t.isVoided() || !t.isVoidable()) {
                    setGraphic(null);
                    return;
                }
                if (!button.getStyleClass().contains("btn-small")) {
                    button.getStyleClass().add("btn-small");
                }
                button.setOnAction(e -> voidHandler.voidTransaction(t));
                setGraphic(button);
                setAlignment(Pos.CENTER);
            }
        });
        return column;
    }
}
 