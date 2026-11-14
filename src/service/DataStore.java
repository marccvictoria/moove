package service;

import exception.StorageException;
import model.CashWithdrawal;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads the whole system as plain CSV files in one folder.
 *
 * users.csv          accounts (username, name, role, password)
 * products.csv       the catalog
 * batches.csv        production batches and remaining stock
 * intakes.csv        raw milk received
 * transactions.csv   sales, donations, processing fees, write-offs, cash-outs
 * sale_items.csv     the lines of each sale
 * meta.csv           id counters and the raw milk pool
 *
 */
class DataStore {

    private static final String USERS = "users.csv";
    private static final String PRODUCTS = "products.csv";
    private static final String BATCHES = "batches.csv";
    private static final String INTAKES = "intakes.csv";
    private static final String TRANSACTIONS = "transactions.csv";
    private static final String SALE_ITEMS = "sale_items.csv";
    private static final String META = "meta.csv";

    private final Path dir;

    DataStore(Path dir) {
        this.dir = dir;
    }


    // returns true if a previous run already saved data here
    boolean hasData() {
        return Files.exists(dir.resolve(META));
    }

    // ========================= saving ========================= 

    void save(Collection<User> users, List<Product> products, Inventory inventory,
              List<Transaction> transactions, int nextTransactionId, int nextProductNumber)
            throws StorageException {
        try {
            Files.createDirectories(dir);

            // users
            List<String> rows = new ArrayList<>();
            for (User u : users) {
                rows.add(Csv.format(u.getUsername(), u.getDisplayName(), u.getRole().name(), u.getPassword()));
            }
            Csv.write(dir.resolve(USERS), "username,displayName,role,password", rows); // Path, header, rows

            // products
            rows = new ArrayList<>();
            for (Product p : products) {
                rows.add(Csv.format(p.getId(), p.getCategory().name(), p.getName(), p.getDescription(),
                        Double.toString(p.getPrice()), Integer.toString(p.getShelfLifeDays()),
                        Boolean.toString(p.isActive())));
            }
            Csv.write(dir.resolve(PRODUCTS), "id,category,name,description,price,shelfLifeDays,active", rows);

            // batches
            rows = new ArrayList<>();
            for (ProductBatch b : inventory.getBatches()) {
                rows.add(Csv.format(b.getId(), b.getProductId(), Integer.toString(b.getUnitsProduced()),
                        Integer.toString(b.getUnitsRemaining()), b.getProducedOn().toString(),
                        b.getExpiresOn().toString(), Double.toString(b.getLitersUsed())));
            }
            Csv.write(dir.resolve(BATCHES), "id,productId,unitsProduced,unitsRemaining,producedOn,expiresOn,litersUsed", rows);

            // intakes
            rows = new ArrayList<>();
            for (MilkIntake m : inventory.getIntakes()) {
                rows.add(Csv.format(Integer.toString(m.getId()), m.getDate().toString(),
                        Double.toString(m.getLiters()), m.getSource(), m.getRecordedBy()));
            }
            Csv.write(dir.resolve(INTAKES), "id,date,liters,source,recordedBy", rows);

            // transactions and sale items
            List<String> txRows = new ArrayList<>();
            List<String> itemRows = new ArrayList<>();
            for (Transaction t : transactions) {
                txRows.add(transactionRow(t));
                if (t instanceof Sale) {
                    for (SaleItem item : ((Sale) t).getItems()) {
                        itemRows.add(Csv.format(Integer.toString(t.getId()), item.getProductId(),
                                item.getProductName(), Double.toString(item.getUnitPrice()),
                                Integer.toString(item.getQuantity()), encodeAllocations(item.getAllocations())));
                    }
                }
            }
            Csv.write(dir.resolve(TRANSACTIONS),
                    "id,type,timestamp,recordedBy,status,voidedBy,voidedAt,voidReason,f1,f2,f3,f4,f5", txRows);
            Csv.write(dir.resolve(SALE_ITEMS), "txId,productId,productName,unitPrice,quantity,allocations", itemRows);

            rows = new ArrayList<>();
            rows.add(Csv.format("nextTransactionId", Integer.toString(nextTransactionId)));
            rows.add(Csv.format("nextProductNumber", Integer.toString(nextProductNumber)));
            rows.add(Csv.format("nextBatchNumber", Integer.toString(inventory.getNextBatchNumber())));
            rows.add(Csv.format("nextIntakeNumber", Integer.toString(inventory.getNextIntakeNumber())));
            rows.add(Csv.format("rawMilkLiters", Double.toString(inventory.getRawMilkLiters())));
            Csv.write(dir.resolve(META), "key,value", rows);
        } catch (IOException e) {
            throw new StorageException("Could not save data to " + dir.toAbsolutePath() + ". Check that the folder is writable.", e);
        }
    }

    private String transactionRow(Transaction t) {
        String f1 = "";
        String f2 = "";
        String f3 = "";
        String f4 = "";
        String f5 = "";
        if (t instanceof Sale) {
            f1 = Double.toString(((Sale) t).getCashReceived());
        } else if (t instanceof ProcessingFee) {
            ProcessingFee fee = (ProcessingFee) t;
            f1 = fee.getFarmerName();
            f2 = Double.toString(fee.getLiters());
            f3 = Double.toString(fee.getRatePerLiter());
        } else if (t instanceof WriteOff) {
            WriteOff w = (WriteOff) t;
            f1 = w.getDescription();
            f2 = Integer.toString(w.getUnits());
        } else if (t instanceof CashWithdrawal) {
            CashWithdrawal c = (CashWithdrawal) t;
            f1 = c.getPurpose();
            f2 = Double.toString(c.getAmount());
        }
        return Csv.format(Integer.toString(t.getId()), t.getTypeCode(), t.getTimestamp().toString(),
                t.getRecordedBy(), t.getStatus().name(), t.getVoidedBy(),
                t.getVoidedAt() == null ? "" : t.getVoidedAt().toString(), t.getVoidReason(),
                f1, f2, f3, f4, f5);
    }

    private static String encodeAllocations(Map<String, Integer> allocations) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : allocations.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    private static Map<String, Integer> decodeAllocations(String text) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) {
            return map;
        }
        for (String part : text.split(";")) {
            String[] pair = part.split(":");
            map.put(pair[0], Integer.parseInt(pair[1]));
        }
        return map;
    }

    // ========================= loading ========================= 
    // return the saved accounts
    List<User> loadUsers() throws StorageException {
        List<User> users = new ArrayList<>();
        for (List<String> r : rows(USERS, 4)) {
            users.add(parse(USERS, r, () -> User.create(Role.valueOf(r.get(2)), r.get(0), r.get(1), r.get(3))));
        }
        return users;
    }

    // return the saved catalog
    List<Product> loadProducts() throws StorageException {
        List<Product> products = new ArrayList<>();
        for (List<String> r : rows(PRODUCTS, 7)) {
            products.add(parse(PRODUCTS, r, () -> ProductCategory.valueOf(r.get(1)).create(
                    r.get(0), r.get(2), r.get(3), Double.parseDouble(r.get(4)),
                    Integer.parseInt(r.get(5)), Boolean.parseBoolean(r.get(6)))));
        }
        return products;
    }

    // fills the given inventory with the saved batches, milk intakes and counters
    void loadInventory(Inventory inventory, Map<String, String> meta) throws StorageException {
        for (List<String> r : rows(BATCHES, 7)) {
            inventory.loadBatch(parse(BATCHES, r, () -> new ProductBatch(r.get(0), r.get(1),
                    Integer.parseInt(r.get(2)), Integer.parseInt(r.get(3)),
                    LocalDate.parse(r.get(4)), LocalDate.parse(r.get(5)), Double.parseDouble(r.get(6)))));
        }
        for (List<String> r : rows(INTAKES, 5)) {
            inventory.loadIntake(parse(INTAKES, r, () -> new MilkIntake(Integer.parseInt(r.get(0)),
                    LocalDate.parse(r.get(1)), Double.parseDouble(r.get(2)), r.get(3), r.get(4))));
        }
        inventory.loadCounters(
                Double.parseDouble(meta.getOrDefault("rawMilkLiters", "0")),
                Integer.parseInt(meta.getOrDefault("nextBatchNumber", "1")),
                Integer.parseInt(meta.getOrDefault("nextIntakeNumber", "1")));
    }

    // return the saved id counters and settings
    Map<String, String> loadMeta() throws StorageException {
        Map<String, String> meta = new HashMap<>();
        for (List<String> r : rows(META, 2)) {
            meta.put(r.get(0), r.get(1));
        }
        return meta;
    }

    // return every saved transaction, with its sale lines and void status restored
    List<Transaction> loadTransactions() throws StorageException {
        Map<Integer, List<SaleItem>> itemsBySale = new HashMap<>();
        for (List<String> r : rows(SALE_ITEMS, 6)) {
            SaleItem item = parse(SALE_ITEMS, r, () -> new SaleItem(r.get(1), r.get(2),
                    Double.parseDouble(r.get(3)), Integer.parseInt(r.get(4)), decodeAllocations(r.get(5))));
            itemsBySale.computeIfAbsent(Integer.parseInt(r.get(0)), k -> new ArrayList<>()).add(item);
        }
        List<Transaction> list = new ArrayList<>();
        for (List<String> r : rows(TRANSACTIONS, 13)) {
            list.add(parse(TRANSACTIONS, r, () -> {
                int id = Integer.parseInt(r.get(0));
                LocalDateTime when = LocalDateTime.parse(r.get(2));
                String by = r.get(3);
                Transaction t;
                switch (r.get(1)) {
                    case "SALE":
                        t = new Sale(id, when, by, itemsBySale.getOrDefault(id, new ArrayList<>()),
                                Double.parseDouble(r.get(8)));
                        break;
                    case "FEE":
                        t = new ProcessingFee(id, when, by, r.get(8),
                                Double.parseDouble(r.get(9)), Double.parseDouble(r.get(10)));
                        break;
                    case "WRITEOFF":
                        t = new WriteOff(id, when, by, r.get(8), Integer.parseInt(r.get(9)));
                        break;
                    case "CASHOUT":
                        t = new CashWithdrawal(id, when, by, r.get(8), Double.parseDouble(r.get(9)));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown transaction type " + r.get(1));
                }
                if (Transaction.Status.valueOf(r.get(4)) == Transaction.Status.VOIDED) {
                    t.markVoided(r.get(5), r.get(6).isEmpty() ? null : LocalDateTime.parse(r.get(6)), r.get(7));
                }
                return t;
            }));
        }
        return list;
    }

    // ========================= helpers ========================= 

    private interface RowParser<T> {
        T get();
    }

    private <T> T parse(String file, List<String> row, RowParser<T> parser) throws StorageException {
        try {
            return parser.get();
        } catch (RuntimeException e) {
            throw new StorageException("The data file " + file + " is damaged (row: " + row
                    + "). Fix or delete the file, then restart.", e);
        }
    }

    private List<List<String>> rows(String file, int minimumColumns) throws StorageException {
        try {
            List<List<String>> all = Csv.read(dir.resolve(file));
            for (List<String> r : all) {
                if (r.size() < minimumColumns) {
                    throw new StorageException("The data file " + file + " is damaged (a row has "
                            + r.size() + " columns, expected " + minimumColumns + "). Fix or delete the file.");
                }
            }
            return all;
        } catch (IOException e) {
            throw new StorageException("Could not read " + file + " from " + dir.toAbsolutePath() + ".", e);
        }
    }
}