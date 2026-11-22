# Business Logic

## service package
*This contains the AgriSystem and persistence structure which includes managing the saving and loading function of data used in the system.*

#### final class AgriSystem 
It is final, holds the users, products, inventory, and transactions, and is the single entry point for every action. It takes a java.time.Clock, so the date used for expiry can be controlled. AgriSystem(Path) uses the real clock.
<br><br>
Maintains in-memory maps and lists of users, products, inventory, and transactions,
<br><br>
Instead of user interface changing accounts, product catalog, inventory, and transaction history directly, it calls AgriSystem methods. Every action then passes through `AgriSystem`, which validates input (throw AgriException), enforces security rules, updates application state, apply the change, and save everything to disk.

```text
public AgriSystem(Path dataDir) throws AgriException {this(dataDir,Clock.systemDefaultZone());}
```
Initialize the system pointing to a storage directory. The primary constructor defaults to the system clock.

```text
public AgriSystem(Path dataDir, Clock clock) throws AgriException {}
```
Opens the system with a given clock

```text
private void load() throws StorageException {}
private void seed() throws AgriException {}
```
When the application starts up, it checks if data files already exist via `DataStore`. If they do, it `load()`. Otherwise, it populates the system with starter data by `seed()`

```text
public User login(String username, String password) throws AuthenticationException {}
```
`currentUser` in this method keeps track of the currently signed-in user. It validates input fields, loop up the user account, verufues the password, and sets the `currentUser`.

```text
private void requireProductRights() throws PermissionException{}
private void requireInventoryRights() throws PermissionException {} 
```
Actions like adding a product, managing inventory, or withdrawing cash from the wallet necessitate checking the current user's permission. `PermissionException` blocks a Clerk trying to perform an action only a Manager is permitted to.

```text
public Product addProduct() throws AgriException {
requireProductRights();
Input.choice(category, "category");
String cleanName = validateProductFields(null, name, price, shelfLifeDays);
...
return product;
}
public void updateProduct() throws AgriException {}
```
Adding or updating a product requires permission with `requireProductRights()`. When prmitted, this method also `validateProductFields()` before it format the inputs and finally stores the data. This method also accounts for optional fields and possible ranges of the required input.

```text
public Sale sell(Map<String, Integer> quantities, double cashReceived) throws AgriException {}
```
`sell()` processes a custoemr purchase. It loops through cart items, checks product availability, allocates inventory units from specific batches, calculates totals, and verifies cash payment.

```text
} catch (AgriException e) {
undoAllocations(items);
       throw e;
  }
```
If any error occurs midway allocation, it calls `undoAllocations()` to put stock back into inventoy so items are not lost when not sold.
<br><br>
`recordProcessingFee()` records payments or fees associated with raw milk supplied by local farmers and `recordCashWithdrawals()` handles physical cash out of the store's e-wallet.
<br><br>

```text
public double getWalletBalance() {}
```
`getWalletBalance()` iterates through all recorded transactions, summing up their financial impacts to calculate current cash wallet balance.

#### final class Csv
Package-private. Reads and writes CSV lines, quoting fields that contain commas or quotes. Files are written to a temporary file and then renamed, so that a crash cannot leave half a file.
<br><br>
This class, in simple words, handles the translation from raw text rows in files to Java objects in memory.
```text
static String format(String... fields)
```
Takes a variable number of string fields and joins them into a single comma-separated text line. 
<br><br>
`StringBuilder line` builds the string in memory. The next block of codes is for looping though every field provided. It iserts a comma separator between fields, but avoids it if before first field. Null values will be empty strings.
<br>
It also checks if the field contains a comma or a double quote (characters that would break standard CSV file parsing) and if special characters exists.
```text
static List<String> parse(String line) {}
```
On the other hand, this function splits a CSV Line back into a list of individual columm values. It first stores it in `List<String> fields`. `boolean inQuotes` lets the method know if they are currently inside a quoted string which allows commas and quotes to be safely ignored as separators.
```text
static List<List<String>> read(Path file) throws IOException {}
```
This reads a CSV file, skipping the header row and blank lines. This returns a list of rows (each row is a list of string fields). This returns an empty list if the file does not exist yer (such as on first-time app launch).
```text
List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
```
This line in reading a CSV file loads all text lines from the file into memory using standard UTF-8 character encoding. Lines becomes a list containing n strings. The loop start at index 1 to skip the header row (contains columm names like "id", "name", "price")
```text
static void write(Path file, String header, List<String> rows) throws IOException {}
```
This saves a header string and a collection of formatted data rows back to a file. `files.write` writes the data to a temporary file first before making it the actual file using `files.move`.

#### class DataStore
Package-private. Saves and loads the whole system as CSV files.
```text
boolean hasData() {return Files.exists(dir.resolve(META));}
```
This returns true if a previous run already saved data here. It checks if the application has been run and saved before by checking if `meta.csv` already exists in the storage directory. If it does, the app loads existing records; if not, it triggers first-run seeding.
<br><br>
Saving methods include `save()`, `transactionRow()`, and `encodeAllocations()` while loading methods include 'decodeAllocations()', `loadUsers()`, `loadProducts()`, `loadInventory()`, `loadMeta()`, and `loadTransactions()`
```text
void save(Collection<User> users, List<Product> products, Inventory inventory, List<Transaction> transactions, int nextTransactionId, int nextProductNumber) throws StorageException {}
```
This serializes the entire in-memory system state across multiple CSV files (`batches.csv`, `intakes.csv`, `meta.csv`, `products.csv`, `sale_items.csv`, `transactions.csv`, `user.csv`). This saving process relies uses the methods of final class Csv (`Csv.format()`, `Csv.write()`, and `Csv.read()`).
```text
private String transactionRow(Transaction t) {
```
Helper method that normalizes (converts into uniform structure) polymorphic transaction subclasses(`Sale`, `ProcessingFee`, `WriteOff`, and `CashWithdrawal`) into a unified CSV row structured by mapping their specific attributes.
```text
private static String encodeAllocations(Map<String, Integer> allocations){}
```
Converts an inventory batch allocation map into a compact, semicolon-delimited string format so it can be stored inside a single CSV columm.
```text
private static Map<String, Integer> decodeAllocations(String text) {}
```
Reconstructs an inventory batch allocation map by parsing a semicolon-delimited text string back into a key-value pairs.
<br><br>
`loadUsers()`, `loadProducts()`, and `loadMeta()`, reads its corresponding .csv file and reconstructs key-value maps or catalogs or accounts with their respective classifications or roles and the authentication data. `loadInventory()` populates the inventory system with saved production batches and raw milk intakes. `loadTransactions()`, on the other hand, reconstitute all financial and operational transaction history.

#### final class SeedData
This class is responsible for generating initial starter data when the application is launched and no existing CSV files are found in the `data/` directory. It ensures that there are `Users` to login to. Package-private. It has the accounts, the 35 starter products, and the opening stock used on the first run.
```text
static List<User> users() {}
```
This returns the default accounts (manager/manager123, clerk1/clerk123, clerk2/clerk123)
```text
static List<Product> products() {}
```
This returns the starting product list (ids P001 to P035) and the data for expiry dates are guesses. It covers four main product categories (`LIQUID`, `YOGURT`, `CHEESE`, and `BUTTER`) with realistic description and shelf lives.
```text
static void openingStock(Inventory inventory, List<Product> products, LocalDate today) throws AgriException {}
```
This adds raw milk and opening batches. Initial batches creates starting stock batches for various product IDs with custom expiry dates relative to today. Few sizes are deliberately left out of stock.

