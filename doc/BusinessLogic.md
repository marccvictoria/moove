# Business Logic

## service package
*Description*

#### AgriSystem
It is final, holds the users, products, inventory, and transactions, and is the single entry point for every action. It takes a java.time.Clock, so the date used for expiry can be controlled. AgriSystem(Path) uses the real clock.
<br><br>
Instead of user interface changing accounts, product catalog, inventory, and transaction history directly, it calls AgriSystem methods which (1) check whether that signed-in user is allowed for an action (2) validate input and throw AgriException when wrong, apply the change, and save everything to disk

```text
public AgriSystem(Path dataDir) throws AgriException {this(dataDir,Clock.systemDefaultZone());}
```
opens the system using the real clock

```text
public AgriSystem(Path dataDir, Clock clock) throws AgriException {}
```
opens the system with a given clock
```text
private void load() throws StorageException {}
private void seed() throws AgriException {}
```
When the application starts up, it checks if data files already exist via `DataStore`. If they do, it `load()`. Otherwise, it populates the system with starter data by `seed()`
```text
public User login(String username, String password) throws AuthenticationException {}
```
`currentUser` in this method keeps track of th ecurrently signed-in user
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

```

#### DataStore
Package-private. Saves and loads the whole system as CSV files.

#### Csv
Package-private. Reads and writes CSV lines, quoting fields that contain commas or quotes. Files are written to a temporary file and then renamed, so that a crash cannot leave half a file.

#### SeedData
Package-private. The accounts, the 35 starter products, and the opening stock used on the first run.
