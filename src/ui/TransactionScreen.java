package ui;

import model.Donation;
import model.Sale;
import model.Transaction;
import model.User;
import service.AgriSystem;
import components.FormDialog;
import components.StatCard;
import components.TransactionTable;
import util.Fmt;
import util.Input;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* 
 * - Wallet ballence
 * - Filterable transaction history (donor, type, amount or item, date for donations)
 * - Buttons to record donations, processing fees, and cash-outs, and to void a transaction
 * - readOnly is true (when a manager is looking at clerk's dashboard), false otherwise
 * 		- buttons and void column are hidden
 * 
 * Record Donation Button
 * - Donor name
 * - type (cash, item)
 * - amount (unwritable if type item)
 * - item name (unwritable if type cash)
 * - item quantity (unwritable if type cash)
 * 
 * Toll Processing Button
 * - shows a dialog:
 * 		- Farmer's cooperative name (input)
 * 		- Liter's processed (input)
 * 		- fee per liter (input)
 * 		- Fee (dynamically update)
 * 
 * Transaction History
 * Filter Selector:
 * - Sale
 * - Donation
 * - Processing Fee
 * - Cash out
 * - Write off
 * Search button
 * Show voided checkbox
 * 
 * Transaction History Table
 * Columns: number, date, type, details, amount, recorded by, status, button to void that transaction
 * */


public class TransactionScreen extends Screen {
 
    private static final String ALL_TYPES = "All types";
 
    private final AgriSystem system;
    private final String onlyUsername; // show only this person's transactions (null = everyone's)
    private final boolean readOnly;
 
    private final StatCard walletCard = new StatCard("Wallet balance");
    private final StatCard completedCard = new StatCard("Completed transactions");
    private final StatCard voidedCard = new StatCard("Voided");
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final TextField search = new TextField();
    private final CheckBox showVoided = new CheckBox("Show voided");
    private TransactionTable table;
    private List<Transaction> allTransactions = new ArrayList<>();
 
    public TransactionScreen(AgriSystem system, String onlyUsername, boolean readOnly) {
        this.system = system;
        this.onlyUsername = onlyUsername;
        this.readOnly = readOnly;
    }
 
    @Override
    public String getName() {
        if (onlyUsername == null) {
            return "Cash & Transactions";
        }
        return "Transactions";
    }
 
    @Override
    protected Parent build() {
        // a read-only screen gets a table without the Void button column
        if (readOnly) {
            table = new TransactionTable(system, null);
        } else {
            table = new TransactionTable(system, t -> voidTransaction(t));
        }
        table.setPrefHeight(440);
        VBox.setVgrow(table, Priority.ALWAYS);
 
        HBox stats = new HBox(14, walletCard, completedCard, voidedCard);
        VBox body = new VBox(14, stats);
 
        // buttons (hidden when readOnly)
        if (!readOnly) {
            Button donation = new Button("Record donation");
            donation.getStyleClass().add("btn-primary");
            donation.setOnAction(e -> recordDonation());
 
            Button fee = new Button("Toll processing fee");
            fee.getStyleClass().add("btn-primary");
            fee.setOnAction(e -> recordFee());
 
            HBox actions = new HBox(10, donation, fee);
 
            body.getChildren().add(actions);
        }
 
        // filters
        typeFilter.getItems().addAll(ALL_TYPES, "Sale", "Donation", "Processing fee", "Cash out", "Write-off");
        typeFilter.setValue(ALL_TYPES);
        typeFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        search.setPromptText("Search details, donor, person...");
        search.setPrefWidth(280);
        search.textProperty().addListener((o, a, b) -> applyFilter());
        showVoided.setSelected(true);
        showVoided.selectedProperty().addListener((o, a, b) -> applyFilter());
        HBox filters = new HBox(12, typeFilter, search, showVoided);
 
        body.getChildren().addAll(section("Transaction history"), filters, table);
 
        String subtitle = "Money in and out of the wallet. Voiding keeps the transaction in the history and reverses its effect.";
        if (readOnly) {
            subtitle = "Transactions recorded by this clerk (view only).";
        }
        return page("Cash & Transactions", subtitle, body);
    }
 
    @Override
    public void refresh() {
        if (onlyUsername == null) {
            allTransactions = system.getTransactions();
        } else {
            allTransactions = system.getTransactionsBy(onlyUsername);
        }
        applyFilter();
 
        // count completed and voided transactions for the cards
        int completed = 0;
        int voided = 0;
        for (Transaction t : allTransactions) {
            if (t.isVoided()) {
                voided++;
            } else {
                completed++;
            }
        }
        walletCard.setValue(Fmt.money(system.getWalletBalance()));
        completedCard.setValue(Integer.toString(completed));
        voidedCard.setValue(Integer.toString(voided));
    }
 
    // ======================= filter ========================
 
    // shows only the transactions that match the type, search text and "show voided" choice
    private void applyFilter() {
        if (table == null) {
            return;
        }
        String q = "";
        if (search.getText() != null) {
            q = search.getText().trim().toLowerCase();
        }
        String type = typeFilter.getValue();
        List<Transaction> visible = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (!showVoided.isSelected() && t.isVoided()) {
                continue;
            }
            if (type != null && !type.equals(ALL_TYPES) && !t.getType().equals(type)) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean found = t.getDetails().toLowerCase().contains(q)
                        || system.displayNameOf(t.getRecordedBy()).toLowerCase().contains(q)
                        || Integer.toString(t.getId()).equals(q);
                if (!found) {
                    continue;
                }
            }
            visible.add(t);
        }
        table.setData(visible);
    }
 
    // ======================= dialogs ========================
 
    private void recordDonation() {
        FormDialog dialog = new FormDialog("Record donation", null, "Record donation");
        TextField donor = dialog.addText("Donor name", "", "Who donated?");
        ComboBox<Donation.Kind> kind = dialog.addChoice("Type", Arrays.asList(Donation.Kind.values()), Donation.Kind.CASH);
        TextField amount = dialog.addText("Amount (PHP)", "", "Cash donations only");
        TextField item = dialog.addText("Item name", "", "Item donations only");
        TextField quantity = dialog.addText("Item quantity", "", "Item donations only");
 
        // cash: only the amount can be typed
        // item: only the item name and quantity
        Runnable sync = () -> {
            boolean cash = kind.getValue() == Donation.Kind.CASH;
            amount.setDisable(!cash);
            item.setDisable(cash);
            quantity.setDisable(cash);
        };
        kind.valueProperty().addListener((o, a, b) -> sync.run());
        sync.run();
 
        boolean done = dialog.show(() -> {
            if (kind.getValue() == Donation.Kind.CASH) {
                system.recordCashDonation(donor.getText(), Input.positiveNumber(amount.getText(), "Amount"));
            } else {
                system.recordItemDonation(donor.getText(), item.getText(),
                        Input.positiveInt(quantity.getText(), "Item quantity"));
            }
        });
        if (done) {
            refresh();
        }
    }
 
    private void recordFee() {
        FormDialog dialog = new FormDialog("Toll processing fee", null, "Record fee");
        TextField farmer = dialog.addText("Farmer / cooperative", "", "Name");
        TextField liters = dialog.addText("Liters processed", "", "e.g. 25");
        TextField rate = dialog.addText("Fee per liter (PHP)", "13.00", "");
        Label total = dialog.addInfo("Fee", Fmt.money(0));
 
        // the Fee line updates while the user types
        Runnable update = () -> {
            try {
                total.setText(Fmt.money(Fmt.round2(Input.number(liters.getText(), "Liters")
                        * Input.number(rate.getText(), "Rate"))));
            } catch (Exception e) {
                total.setText("-");
            }
        };
        liters.textProperty().addListener((o, a, b) -> update.run());
        rate.textProperty().addListener((o, a, b) -> update.run());
 
        boolean done = dialog.show(() -> system.recordProcessingFee(farmer.getText(),
                Input.positiveNumber(liters.getText(), "Liters processed"),
                Input.positiveNumber(rate.getText(), "Fee per liter")));
        if (done) {
            refresh();
        }
    }
 
    private void voidTransaction(Transaction t) {
        // tell the user what will happen before they confirm
        StringBuilder effects = new StringBuilder();
        if (t instanceof Sale) {
            effects.append("The sold units go back into stock. ");
        }
        double cash = t.getCashAmount();
        if (cash > 0) {
            effects.append(Fmt.money(cash)).append(" is taken out of the wallet.");
        } else if (cash < 0) {
            effects.append(Fmt.money(-cash)).append(" is put back into the wallet.");
        }
 
        FormDialog dialog = new FormDialog("Void transaction #" + t.getId(),
                t.getType() + ": " + t.getDetails(), "Void transaction");
        if (effects.length() > 0) {
            dialog.addInfo("What happens", effects.toString());
        }
        TextField reason = dialog.addText("Reason (optional)", "", "Why is it being voided?");
 
        if (dialog.show(() -> system.voidTransaction(t.getId(), reason.getText()))) {
            refresh();
        }
    }
}
