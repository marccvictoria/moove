package ui;

import model.Product;
import model.ProductBatch;
import model.Sale;
import model.Transaction;
import model.User;
import service.AgriSystem;
import components.StatCard;
import components.Tables;
import components.TransactionTable;
import util.Fmt;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager Dashboard's main screen
 * + shop stats: sales today, wallet balance, stock alerts
 * + a list of clerks (sales today, all-time sales), each with a button that opens
 *   that clerk's dashboard in a new window
 * + the sales history of every clerk, with a filter to pick one clerk
 */
public class OverviewScreen extends Screen {

    private static final String ALL_CLERKS = "All clerks";

    private final SceneManager sceneManager;
    private final AgriSystem system;

    private final StatCard todayCard = new StatCard("Shop sales today");
    private final StatCard walletCard = new StatCard("Wallet balance");
    private final StatCard nearCard = new StatCard("Batches near expiry");
    private final StatCard outCard = new StatCard("Products out of stock");
    private final Label stockAlert = new Label();
    private final TableView<User> clerkTable = new TableView<>();
    private final ComboBox<String> clerkFilter = new ComboBox<>();
    private TransactionTable salesTable;

    public OverviewScreen(SceneManager sceneManager, AgriSystem system) {
        this.sceneManager = sceneManager;
        this.system = system;
    }

    @Override
    public String getName() {
        return "Overview";
    }

    @Override
    protected Parent build() {
        FlowPane stats = new FlowPane(14, 14, todayCard, walletCard, nearCard, outCard);

        stockAlert.getStyleClass().add("alert-warn");
        stockAlert.setWrapText(true);
        stockAlert.setMaxWidth(Double.MAX_VALUE);

        buildClerkTable();
        clerkTable.setPrefHeight(150);

        clerkFilter.setPrefWidth(200);
        clerkFilter.valueProperty().addListener((o, a, b) -> filterSales());
        salesTable = new TransactionTable(system, null);
        salesTable.setPrefHeight(340);
        VBox.setVgrow(salesTable, Priority.ALWAYS);

        HBox historyHeader = new HBox(12, section("Clerk sales history"), clerkFilter);
        historyHeader.setAlignment(Pos.CENTER_LEFT);

        return page("Manager dashboard",
                "Monitor sales, stock alerts and each clerk's activity.",
                stats, stockAlert, section("Clerks"), clerkTable, historyHeader, salesTable);
    }

    private void buildClerkTable() {
        clerkTable.getColumns().add(Tables.text("Clerk", 200, User::getDisplayName));
        clerkTable.getColumns().add(Tables.number("Sales today", 140,
                u -> Fmt.money(system.salesTotalOn(system.today(), u.getUsername()))));
        clerkTable.getColumns().add(Tables.number("All-time sales", 140,
                u -> Fmt.money(system.salesTotalAllTime(u.getUsername()))));
        clerkTable.getColumns().add(Tables.number("Transactions", 110,
                u -> Integer.toString(system.getTransactionsBy(u.getUsername()).size())));

        TableColumn<User, User> open = new TableColumn<>("");
        open.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        open.setPrefWidth(180);
        open.setSortable(false);
        open.setCellFactory(col -> new TableCell<User, User>() {
            private final Button button = new Button("Open dashboard");

            {
                button.getStyleClass().add("btn-small");
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                button.setOnAction(e -> sceneManager.openClerkWindow(item));
                setGraphic(button);
            }
        });
        clerkTable.getColumns().add(open);
        clerkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        clerkTable.setPlaceholder(new Label("No clerk accounts."));
    }

    @Override
    public void refresh() {
        LocalDate today = system.today();
        todayCard.setValue(Fmt.money(system.salesTotalOn(today, null)));
        walletCard.setValue(Fmt.money(system.getWalletBalance()));

        List<ProductBatch> near = system.getInventory().getNearExpiryBatches(today);
        List<ProductBatch> expired = system.getInventory().getExpiredBatchesWithStock(today);
        nearCard.setValue(Integer.toString(near.size())).setWarning(!near.isEmpty());
        int out = 0;
        for (Product p : system.getActiveProducts()) {
            if (system.availableUnits(p) == 0) {
                out++;
            }
        }
        outCard.setValue(Integer.toString(out)).setWarning(out > 0);

        StringBuilder alert = new StringBuilder();
        if (!expired.isEmpty()) {
            int units = 0;
            for (ProductBatch b : expired) {
                units += b.getUnitsRemaining();
            }
            alert.append(units).append(" expired units are still on the shelf. Use Inventory > Discard expired. ");
        }
        if (!near.isEmpty()) {
            alert.append(near.size()).append(" batch(es) expire within ")
                    .append(ProductBatch.NEAR_EXPIRY_DAYS).append(" days; they will be sold first.");
        }
        boolean showAlert = alert.length() > 0;
        stockAlert.setText(alert.toString().trim());
        stockAlert.setVisible(showAlert);
        stockAlert.setManaged(showAlert);

        clerkTable.setItems(FXCollections.observableArrayList(system.getClerks()));
        clerkTable.refresh();

        String selected = clerkFilter.getValue();
        List<String> choices = new ArrayList<>();
        choices.add(ALL_CLERKS);
        for (User u : system.getClerks()) {
            choices.add(u.getDisplayName());
        }
        clerkFilter.getItems().setAll(choices);
        clerkFilter.setValue(selected != null && choices.contains(selected) ? selected : ALL_CLERKS);
        filterSales();
    }

    // shows only the sales of the chosen clerk
    private void filterSales() {
        if (salesTable == null) {
            return;
        }
        String name = clerkFilter.getValue();
        List<Transaction> visible = new ArrayList<>();
        for (Sale sale : system.getSales()) {
            if (name == null || name.equals(ALL_CLERKS)
                    || system.displayNameOf(sale.getRecordedBy()).equals(name)) {
                visible.add(sale);
            }
        }
        salesTable.setData(visible);
    }
}
