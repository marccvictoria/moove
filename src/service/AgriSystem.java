package service;

import exception.AgriException;
import exception.AuthenticationException;
import exception.InsufficientFundsException;
import exception.PermissionException;
import exception.StorageException;
import exception.ValidationException;
import model.CashWithdrawal;
import model.Donation;
import model.Inventory;
import model.MilkIntake;
import model.ProcessingFee;
import model.Product;
import model.ProductBatch;
import model.ProductCategory;
import model.Role;
import model.Sale;
import model.SaleItem;
import model.Transaction;
import model.User;
import model.WriteOff;
import util.Fmt;
import util.Input;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Note: this class is named as AgriSystem because java.lang.System already exists
 *
 * AgriSystem
 * + owns accounts
 * + product catalog
 * + inventory and the transaction history 
 * + instead of user interface changing those objects directly, it calls methods here which:
 * 		+ check that signed-in user is allowed to do it
 * 		+ validate input and throw a AgriException if wrong, apply the change, and save everything to disk
 * 
 * The app is synchronized with local time to simulate real-life expiry situations.
 * java.time.Clock provides the methods for this. 
 * systemDefaultZone() : returns a Clock based on the system's default time zone
 * 
 * References:
 * https://www.geeksforgeeks.org/java/java-time-clock-class-in-java/
 * https://stackoverflow.com/questions/122105/how-to-filter-a-java-collection-based-on-predicate
 * https://www.w3schools.com/java/java_collections.asp
 * https://www.geeksforgeeks.org/java/java-method-references/
 */
public final class AgriSystem {

    private static final double MONEY_TOLERANCE = 0.005;

    private final DataStore store;
    private final Clock clock;
    private final Map<String, User> users = new LinkedHashMap<>(); // LinkedHashMap to preserve insertion order
    private final List<Product> products = new ArrayList<>();
    private final Inventory inventory = new Inventory();
    private final List<Transaction> transactions = new ArrayList<>();
    private int nextTransactionId = 1;
    private int nextProductNumber = 1;
    private User currentUser;

    // opens the system using the real clock
    public AgriSystem(Path dataDir) throws AgriException {
        this(dataDir, Clock.systemDefaultZone());
    }

    // opens the system with a given clock, then loads existing data if exists, otherwise generate one
    public AgriSystem(Path dataDir, Clock clock) throws AgriException {
        this.store = new DataStore(dataDir);
        this.clock = clock;
        if (store.hasData()) {
            load();
        } else {
            seed();
        }
    }

    private void load() throws StorageException {
        Map<String, String> meta = store.loadMeta();
        for (User u : store.loadUsers()) { 
            users.put(u.getUsername().toLowerCase(), u);
        }
        products.addAll(store.loadProducts());
        store.loadInventory(inventory, meta);
        transactions.addAll(store.loadTransactions());
        try {
            nextTransactionId = Integer.parseInt(meta.getOrDefault("nextTransactionId", Integer.toString(transactions.size() + 1)));
            nextProductNumber = Integer.parseInt(meta.getOrDefault("nextProductNumber", Integer.toString(products.size() + 1)));
        } catch (NumberFormatException e) {
            throw new StorageException("The data file meta.csv is damaged. Fix or delete the file.", e);
        }
    }

    private void seed() throws AgriException {
        for (User u : SeedData.users()) {
            users.put(u.getUsername().toLowerCase(), u);
        }
        products.addAll(SeedData.products());
        nextProductNumber = products.size() + 1;
        SeedData.openingStock(inventory, products, today());
        persist();
    }

    // saves system's data to dir
    private void persist() throws StorageException {
        store.save(users.values(), products, inventory, transactions, nextTransactionId, nextProductNumber);
    }

    // ========================= time ========================= 

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // ========================= sessions ========================= 
    public User login(String username, String password) throws AuthenticationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Enter your username and password.");
        }
        User user = users.get(username.trim().toLowerCase());
        if (user == null || !user.checkPassword(password)) {
            throw new AuthenticationException("Incorrect username or password.");
        }
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private User requireUser() throws PermissionException {
        if (currentUser == null) {
            throw new PermissionException("Please sign in first.");
        }
        return currentUser;
    }

    private void requireProductRights() throws PermissionException {
        if (!requireUser().canManageProducts()) {
            throw new PermissionException("Only a manager can change products.");
        }
    }

    private void requireInventoryRights() throws PermissionException {
        if (!requireUser().canManageInventory()) {
            throw new PermissionException("Only a manager can change inventory.");
        }
    }

    public User findUser(String username) {
        return username == null ? null : users.get(username.toLowerCase());
    }

    // filters a collection of users to find and return only those who have the role of "CLERK"
    public List<User> getClerks() {
        return users.values().stream() // takes User objects then turn them to stream to allow filtering
                .filter(u -> u.getRole() == Role.CLERK)
                .collect(Collectors.toList()); // packages them into standard List
    }

    public String displayNameOf(String username) {
        User u = findUser(username);
        return u == null ? username : u.getDisplayName();
    }

    // ========================= products ========================= 


    public List<Product> getProducts() {
        return Collections.unmodifiableList(products); // returns a read-only version of the products list so .add(), .remove(), 
        // and other similar methods wont work
    }

    public List<Product> getActiveProducts() {
        return products.stream().filter(Product::isActive).collect(Collectors.toList());
    }

    public Product findProduct(String id) throws ValidationException {
        for (Product p : products) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        throw new ValidationException("Unknown product: " + id);
    }

    public String productNameOf(String id) {
        for (Product p : products) {
            if (p.getId().equals(id)) {
                return p.getName();
            }
        }
        return id;
    }

    public Product addProduct(ProductCategory category, String name, String description,
                              double price, int shelfLifeDays) throws AgriException {
        requireProductRights();
        Input.choice(category, "category");
        String cleanName = validateProductFields(null, name, price, shelfLifeDays);
        String cleanDescription = Input.optionalText(description, "Description", 120);
        String id = String.format("P%03d", nextProductNumber++); // e.g. P001, P012, etc
        Product product = category.create(id, cleanName, cleanDescription, Fmt.round2(price), shelfLifeDays, true);
        products.add(product);
        persist();
        return product;
    }

   
    public void updateProduct(Product product, String name, String description,
                              double price, int shelfLifeDays) throws AgriException {
        requireProductRights();
        String cleanName = validateProductFields(product, name, price, shelfLifeDays);
        product.setName(cleanName);
        product.setDescription(Input.optionalText(description, "Description", 120));
        product.setPrice(Fmt.round2(price));
        product.setShelfLifeDays(shelfLifeDays);
        persist();
    }

   
    public void setProductActive(Product product, boolean active) throws AgriException {
        requireProductRights();
        product.setActive(active);
        persist();
    }

    private String validateProductFields(Product editing, String name, double price, int shelfLifeDays)
            throws ValidationException {
        String clean = Input.text(name, "Product name", 60);
        for (Product p : products) {
            if (p != editing && p.getName().equalsIgnoreCase(clean)) {
                throw new ValidationException("A product named \"" + clean + "\" already exists.");
            }
        }
        if (price <= 0) {
            throw new ValidationException("Price must be greater than zero.");
        }
        if (price > 1_000_000) {
            throw new ValidationException("Price looks too large. Check the amount.");
        }
        if (shelfLifeDays < 1 || shelfLifeDays > 3650) {
            throw new ValidationException("Shelf life must be between 1 and 3650 days.");
        }
        return clean;
    }

    // ========================= inventory ========================= 

    public Inventory getInventory() {
        return inventory;
    }

    public int availableUnits(Product product) {
        return inventory.availableUnits(product.getId(), today());
    }


    public MilkIntake receiveMilk(double liters, String source) throws AgriException {
        requireInventoryRights();
        String cleanSource = Input.text(source, "Source", 60);
        MilkIntake intake = inventory.receiveMilk(liters, cleanSource, today(), requireUser().getUsername());
        persist();
        return intake;
    }

  
    public ProductBatch addBatch(Product product, int units, double litersUsed,
                                 LocalDate producedOn, LocalDate expiresOn) throws AgriException {
        requireInventoryRights();
        Input.choice(product, "product");
        Input.date(producedOn, "production date");
        Input.date(expiresOn, "expiry date");
        ProductBatch batch = inventory.addBatch(product, units, litersUsed, producedOn, expiresOn);
        persist();
        return batch;
    }


    public WriteOff discardExpired() throws AgriException {
        requireInventoryRights();
        List<ProductBatch> expired = inventory.getExpiredBatchesWithStock(today());
        if (expired.isEmpty()) {
            throw new ValidationException("There is no expired stock to discard.");
        }
        int units = 0;
        List<String> parts = new ArrayList<>();
        for (ProductBatch b : expired) {
            int removed = b.discardAll();
            units += removed;
            parts.add(b.getId() + " (" + productNameOf(b.getProductId()) + " x" + removed + ")");
        }
        WriteOff writeOff = new WriteOff(nextTransactionId++, now(), requireUser().getUsername(),
                String.join(", ", parts), units);
        transactions.add(writeOff);
        persist();
        return writeOff;
    }

    // ========================= sales ========================= 
    
    public Sale sell(Map<String, Integer> quantities, double cashReceived) throws AgriException {
        User user = requireUser();
        if (quantities == null || quantities.isEmpty()) {
            throw new ValidationException("The cart is empty. Add at least one item.");
        }
        List<SaleItem> items = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
                Product product = findProduct(entry.getKey());
                if (!product.isActive()) {
                    throw new ValidationException(product.getName() + " is not currently on sale.");
                }
                int quantity = entry.getValue();
                Map<String, Integer> taken = inventory.allocate(product.getId(), quantity, today());
                items.add(new SaleItem(product.getId(), product.getName(), product.getPrice(), quantity, taken));
            }
            double total = 0;
            for (SaleItem item : items) {
                total += item.getSubtotal();
            }
            total = Fmt.round2(total);
            if (Fmt.round2(cashReceived) + MONEY_TOLERANCE < total) {
                throw new ValidationException("Cash received (" + Fmt.money(cashReceived)
                        + ") is less than the total (" + Fmt.money(total) + ").");
            }
        } catch (AgriException e) {
            undoAllocations(items);
            throw e;
        }
        Sale sale = new Sale(nextTransactionId++, now(), user.getUsername(), items, Fmt.round2(cashReceived));
        transactions.add(sale);
        persist();
        return sale;
    }

    private void undoAllocations(List<SaleItem> items) {
        for (SaleItem item : items) {
            for (Map.Entry<String, Integer> entry : item.getAllocations().entrySet()) {
                try {
                    inventory.restore(entry.getKey(), entry.getValue());
                } catch (ValidationException ignored) {
                    // the batch was just read from the inventory, so it still exists
                }
            }
        }
    }

    public ProcessingFee recordProcessingFee(String farmerName, double liters, double ratePerLiter)
            throws AgriException {
        User user = requireUser();
        String farmer = Input.text(farmerName, "Farmer name", 60);
        if (liters <= 0) {
            throw new ValidationException("Liters must be greater than zero.");
        }
        if (ratePerLiter <= 0) {
            throw new ValidationException("Rate per liter must be greater than zero.");
        }
        ProcessingFee fee = new ProcessingFee(nextTransactionId++, now(), user.getUsername(),
                farmer, liters, Fmt.round2(ratePerLiter));
        transactions.add(fee);
        persist();
        return fee;
    }

    
    public CashWithdrawal recordCashWithdrawal(String purpose, double amount) throws AgriException {
        User user = requireUser();
        if (!user.canWithdrawCash()) {
            throw new PermissionException("Only a manager can take cash out of the wallet.");
        }
        String cleanPurpose = Input.text(purpose, "Purpose", 80);
        if (amount <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        double rounded = Fmt.round2(amount);
        if (rounded > getWalletBalance() + MONEY_TOLERANCE) {
            throw new InsufficientFundsException("The wallet only holds " + Fmt.money(getWalletBalance())
                    + ", so " + Fmt.money(rounded) + " cannot be taken out.");
        }
        CashWithdrawal withdrawal = new CashWithdrawal(nextTransactionId++, now(), user.getUsername(),
                cleanPurpose, rounded);
        transactions.add(withdrawal);
        persist();
        return withdrawal;
    }

    // ========================= voids ========================= 

    public void voidTransaction(int transactionId, String reason) throws AgriException {
        User user = requireUser();
        Transaction t = findTransaction(transactionId);
        if (t.isVoided()) {
            throw new ValidationException("Transaction #" + transactionId + " is already voided.");
        }
        if (!t.isVoidable()) {
            throw new ValidationException(t.getType() + " records cannot be voided.");
        }
        double after = Fmt.round2(getWalletBalance() - t.getCashAmount());
        if (after < -MONEY_TOLERANCE) {
            throw new InsufficientFundsException("The wallet only holds " + Fmt.money(getWalletBalance())
                    + ", so " + Fmt.money(t.getCashAmount()) + " cannot be taken back.");
        }
        String cleanReason = Input.optionalText(reason, "Reason", 120);
        t.rollbackStock(inventory);
        t.markVoided(user.getUsername(), now(), cleanReason);
        persist();
    }

    private Transaction findTransaction(int id) throws ValidationException {
        for (Transaction t : transactions) {
            if (t.getId() == id) {
                return t;
            }
        }
        throw new ValidationException("Transaction #" + id + " does not exist.");
    }

    // ========================= reports ========================= 

    public double getWalletBalance() {
        double balance = 0;
        for (Transaction t : transactions) {
            balance += t.getWalletEffect();
        }
        return Fmt.round2(balance);
    }

    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparingInt(Transaction::getId).reversed());
        return copy;
    }

    public List<Transaction> getTransactionsBy(String username) {
        return getTransactions().stream()
                .filter(t -> t.getRecordedBy().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<Sale> getSales() {
        return getTransactions().stream()
                .filter(t -> t instanceof Sale)
                .map(t -> (Sale) t)
                .collect(Collectors.toList());
    }

    public List<Sale> getSalesBy(String username) {
        return getSales().stream()
                .filter(s -> s.getRecordedBy().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }


    public double salesTotalOn(LocalDate date, String username) {
        double total = 0;
        for (Sale s : username == null ? getSales() : getSalesBy(username)) {
            if (!s.isVoided() && s.getTimestamp().toLocalDate().equals(date)) {
                total += s.getTotal();
            }
        }
        return Fmt.round2(total);
    }

  
    public int salesCountOn(LocalDate date, String username) {
        int count = 0;
        for (Sale s : username == null ? getSales() : getSalesBy(username)) {
            if (!s.isVoided() && s.getTimestamp().toLocalDate().equals(date)) {
                count++;
            }
        }
        return count;
    }

    public double salesTotalAllTime(String username) {
        double total = 0;
        for (Sale s : getSalesBy(username)) {
            if (!s.isVoided()) {
                total += s.getTotal();
            }
        }
        return Fmt.round2(total);
    }
    
    
    // records a cash donation
    // money goes into the wallet
    public Donation recordCashDonation(String donorName, double amount) throws AgriException {
        User user = requireUser();
        String donor = Input.text(donorName, "Donor name", 60);
        if (amount <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        Donation donation = new Donation(nextTransactionId++, now(), user.getUsername(), donor,
                Donation.Kind.CASH, Fmt.round2(amount), "", 0);
        transactions.add(donation);
        persist();
        return donation;
    }
    
    // records an item donation
    // only kept in the history (no wallet or stock change)
    public Donation recordItemDonation(String donorName, String itemName, int quantity) throws AgriException {
        User user = requireUser();
        String donor = Input.text(donorName, "Donor name", 60);
        String item = Input.text(itemName, "Item name", 60);
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        Donation donation = new Donation(nextTransactionId++, now(), user.getUsername(), donor,
                Donation.Kind.ITEM, 0, item, quantity);
        transactions.add(donation);
        persist();
        return donation;
    }
}
