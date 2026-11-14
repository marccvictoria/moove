package ui;

import model.Product;
import model.ProductBatch;
import service.AgriSystem;
import components.Dialogs;
import components.ProductCard;
import util.Fmt;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
 
import java.util.LinkedHashMap;
import java.util.Map;
 

/* 
 * LHS panel
 * - product list in cards (selectable)
 * - a product card contains category, product name, price, how much in stock
 * - search field to search for a product
 * 
 * RHS panel
 * - this is the cart 
 * - placeholder message: "The card is empty. Tap a product to add.
 * - tapping a product n times, adds it to cart n times
 * - a product in cart should be less than or equal to available stock, once you click on the maximum number of
 * product you can buy, it will throw a error (displayed via the dialog)
 * - Total: PHP X.XX at the bottom and below that is the checkout button. As you tap the product, the total should also
 * update.
 * - Once you press the checkout, it will be recorded in history and displayed in the TransactionScreen
 * 
 * */

public class SellScreen extends Screen {
	
    private final AgriSystem system;
    private final boolean readOnly; // true when a manager is looking at a clerk's dashboard
 
    // what is in the cart: product -> quantity (kept in the order the products were added)
    private final Map<Product, Integer> cart = new LinkedHashMap<>();
 
    private final TextField search = new TextField();
    private final FlowPane cards = new FlowPane(12, 12);
    private final VBox cartLines = new VBox(8);
    private final Label totalLabel = new Label();
    private final Button checkout = new Button("Checkout");
 
    public SellScreen(AgriSystem system, boolean readOnly) {
        this.system = system;
        this.readOnly = readOnly;
    }
 
    @Override
    public String getName() {
        return "Sell";
    }
 
    @Override
    protected Parent build() {
        // left panel: search + product cards
        search.setPromptText("Search a product or category");
        search.textProperty().addListener((o, a, b) -> showCards());
 
        cards.setPadding(new Insets(4));
        cards.setAlignment(Pos.TOP_CENTER);
        ScrollPane cardScroll = new ScrollPane(cards);
        cardScroll.setFitToWidth(true);
        cardScroll.setPrefHeight(520);
 
        VBox left = new VBox(10, search, cardScroll);
        HBox.setHgrow(left, Priority.ALWAYS);
 
        // right panel: cart, total and checkout
        Label cartTitle = new Label("Cart");
        cartTitle.getStyleClass().add("section-title");
 
        ScrollPane lineScroll = new ScrollPane(cartLines);
        lineScroll.setFitToWidth(true);
        lineScroll.setPrefHeight(380);
        VBox.setVgrow(lineScroll, Priority.ALWAYS);
 
        totalLabel.getStyleClass().add("stat-value");
 
        checkout.getStyleClass().add("btn-primary");
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setOnAction(e -> checkout());
 
        VBox right = new VBox(12, cartTitle, lineScroll, totalLabel, checkout);
        right.getStyleClass().add("sell-right-panel");
        right.setPrefWidth(350); 
 
        String subtitle = readOnly
                ? "View only. Clerks record sales from their own dashboard."
                : "Tap a product to add it to the cart, then check out.";
        return page("Sell", subtitle, new HBox(18, left, right));
    }
 
    // ========================= product cards =========================
 
    // rebuilds the cards from the products that are on sale and match the search text
    private void showCards() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        cards.getChildren().clear();
        for (Product p : system.getActiveProducts()) {
            boolean matches = q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getCategory().getLabel().toLowerCase().contains(q);
            if (!matches) {
                continue;
            }
            ProductBatch next = system.getInventory().nextBatchToSell(p.getId(), system.today());
            boolean nearExpiry = next != null
                    && next.getFreshness(system.today()) == ProductBatch.Freshness.NEAR_EXPIRY;
            cards.getChildren().add(new ProductCard(p, system.availableUnits(p), nearExpiry, !readOnly,
                    () -> addToCart(p)));
        }
        if (cards.getChildren().isEmpty()) {
            Label none = new Label("No products match your search.");
            none.getStyleClass().add("page-subtitle");
            cards.getChildren().add(none);
        }
    }
 
    // ========================= cart =========================
 
    // adds one unit; the cart can never hold more than the stock that can be sold
    private void addToCart(Product product) {
        int stock = system.availableUnits(product);
        int inCart = cart.getOrDefault(product, 0);
        if (inCart + 1 > stock) {
            Dialogs.error("Only " + stock + " " + product.getName() + " in stock, and all of it is already in the cart.");
            return;
        }
        cart.put(product, inCart + 1);
        showCart();
    }
 
    // takes one unit out, and removes the line when it reaches zero
    private void removeFromCart(Product product) {
        int left = cart.getOrDefault(product, 0) - 1;
        if (left <= 0) {
            cart.remove(product);
        } else {
            cart.put(product, left);
        }
        showCart();
    }
 
    // redraws the cart lines and the total
    private void showCart() {
        cartLines.getChildren().clear();
        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            double subtotal = Fmt.round2(p.getPrice() * qty);
            total += subtotal;
 
            Label name = new Label(p.getName());
            Label detail = new Label(qty + " x " + Fmt.money(p.getPrice()));
            detail.getStyleClass().add("stat-note");
            VBox text = new VBox(2, name, detail);
 
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label sub = new Label(Fmt.money(subtotal));
 
            HBox line = new HBox(8, text, spacer, sub);
            line.setAlignment(Pos.CENTER_LEFT);
            if (!readOnly) {
                Button minus = new Button("-");
                minus.getStyleClass().add("btn-small");
                minus.setOnAction(e -> removeFromCart(p));
                line.getChildren().add(minus);
            }
            cartLines.getChildren().add(line);
        }
        if (cart.isEmpty()) {
            Label empty = new Label("The cart is empty. Tap a product to add.");
            empty.getStyleClass().add("page-subtitle");
            empty.setWrapText(true);
            cartLines.getChildren().add(empty);
        }
        totalLabel.setText("Total: " + Fmt.money(Fmt.round2(total)));
        totalLabel.getStyleClass().add("total-price");
        checkout.setDisable(readOnly || cart.isEmpty());
    }
 
    // records the sale, the customer pays the exact total
    private void checkout() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            quantities.put(entry.getKey().getId(), entry.getValue());
            total += entry.getKey().getPrice() * entry.getValue();
        }
        double paid = Fmt.round2(total);
        if (Dialogs.attempt(() -> system.sell(quantities, paid))) {
            cart.clear();
            Dialogs.info("Sale recorded", "Total: " + Fmt.money(paid));
            refresh();
        }
    }
 
    @Override
    public void refresh() {
        // stock may have changed since the last visit, so trim the cart to what can still be sold
        cart.entrySet().removeIf(e -> !e.getKey().isActive() || system.availableUnits(e.getKey()) == 0);
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            entry.setValue(Math.min(entry.getValue(), system.availableUnits(entry.getKey())));
        }
        showCards();
        showCart();
    }

}