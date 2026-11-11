# Domain Models

## model package
*`model package` contains the "actors" of this management and holds information structures for classes like `Users`, `Products`, or `Transactions`and their subclasses like `clerk`, `yogurt`, or some other classes that would make data objects like `CashWithdrawal`*

### User abstract class 
A `User` is a person who can sign in; has a `username`, `displayName`, and a `password`. 
```text
case MANAGER:
return new Manager(username, displayName, password);
case CLERK:
      	return new Clerk(username, displayName, password);
default:
      	throw new IllegalArgumentException("Unknown role: " + role);
```
`create` method returns a `User`, that is either a `Manager` or a `Clerk` <br>
All types of User must override the following methods: `getRole()` that returns a Role and `canManageProducts()`, `canManageInventory()`, `canViewAllClerks()`, and `canWithdrawCash()` that all returns a Boolean.

```text
return this.password.equals(password);
```
`checkPassword` returns Boolean in validating entered password

#### class Manager extends User
```text 
return Role.MANAGER; 
``` 
returns `MANAGER` as Role
```text
return true;
```
A Manager returns `true` for `canManagerProducts()`, `canManageInventory()`, `canViewAllClerks()`, and `canWithdrawCash()`.

#### class Clerk extends User
```text 
return Role.CLERK; 
``` 
returns CLERK as Role
```text
return false;
```
A Clerk returns `false` for `canManagerProducts()`, `canManageInventory()`, `canViewAllClerks()`, and `canWithdrawCash()`.

#### enum Role 
```text
MANAGER("Manager"), CLERK("Clerk");
private final String label;
```
Manager and Clerk are the two kinds of account the system supports.

<br>

### Product abstract class 
A `Product` is an item the Moove offers to the customers. It has an `id`, `name`, `description`, `price`, `shelfLifeDays`, and a boolean type `active`. <br>

All types of Product must override `getCategory()` that returns a ProductCategory, `getUnitLabel()` that returns a String, and `getDefaultShelfLifeDays()` that returns an integer.

#### class LiquidProduct extends Product
```text 
return ProductCategory.LIQUID;
``` 
returns `LIQUID` as `ProductCategory'
```text
return "bottle";
```
returns `bottle` as unit in `getUnitLabel`
```text
return 7;
```
A Liquid product has 7 default shelf life days.

<br>

Product|Category|Unit|Default Shelf Life
|---|---|---|---|
LiquidProduct|LIQUID|"bottle"|7
Yogurt|LIQUID|"bottle"|7
Cheese|CHEESE|"pack"|10
Butter|BUTTER|"pack"|60

#### enum ProductCategory
This enum encapsulates category types, user-friendly labels, default behaviors, and object instantiation in one place.
```text
LIQUID("Fresh milk"),
YOGURT("Yogurt"),
CHEESE("Cheese"),
BUTTER("Butter");

private final String label;
```
```text
@Override
public String toString(){
	return label;
}
```
Overriding `toString()` to return the `label` makes binding this enum directly to JavaFX UI elements like dropdown menus. This ensures users see polished text instead of raw code names, e.g. "Fresh milk" not LIQUID.
<br><br>
When creating a new product, the application can simply call category.create(), and the enum safely returns the correct subclass instance.
```text
return create("sample", "", "", 1, 1, false).getDefaultShelfLifeDays();
```
By temporarily intantiating a dummy product of the category, `defaultShelfLifeDays()` retrieves and returns the usual shelf life in days for this kind of product (for prefilling forms).

### Inventory class
The shop's stock has raw milk waiting to be processed and batches of already finished/processed products.
<br><br>
Inventory has `List<ProductBatch> batches`, `List<MilkIntake> intakes`, `rawMilkLiters`, `nextBatchNumber` initially set to 1, and `nextIntakeNumber` also initially set to 1.

```text
public MilkIntake receiveMilk(double liters, String source, LocalDate date, String recordedBy) throws ValidationException {}
```
`receiveMilk` records the number of raw milk arriving at the plant in Liters and returns MilkIntake

```text
public double getRawMilkLiters(){return rawMilkLiters();}
```
`getRawMilkLiters()` returns liters of raw milk currently available for production

```text
public List<MilkIntake> getIntakes(){return Collections.unmodifiableList(intakes);}
```
`getIntakes` returns all milk intake records, oldest first

```text
public ProductBatch addBatch(Product product, int units, double litersUsed, LocalDate producedOn, LocalDate expiresOn) throws ValidationException, InsufficientStockException {...}
```
`addBatch` adds a production batch (also used to restock an existing product)

```text
public List<ProductBatch> getBatches(){}
```
returns every batch, in the order they were added

```text
public List<ProductBatch> getBatchesByExpiry(){}
```
returns every batch sorted by expiry date, earliest first

```text
public int availableUnits(String productId, LocalDate today) {}
```
returns units that can be sold right now (in stock and not expired)

```text
public ProductBatch nextBatchToSell(String productId, LocalDate today) {}
```
returns the sellable batch that will be sold next (earliest expiry), or null if none

```text
public Map<String, Integer> allocate(String productId, int quantity, LocalDate today)throws ValidationException, InsufficientStockException {}
```
`allocate()` takes quantity units of a product from stock (earliest expiring batch first) using a `PriorityQueue` and returns which batches supplied the units. Nothing changes if there is not enough stock

```text
public void restore(String batchId, int units) throws ValidationException {}
```
Puts units back into the batch they were taken from (used when a sale is voided)

```text
public List<ProductBatch> getNearExpiryBatches(LocalDate today) {}
```
returns the batches that still have units, are not expired, and expire within NEAR_EXPIRY_DAYS

```text
public List<ProductBatch> getExpiredBatchesWithStock(LocalDate today) {}
```
returns the batches that have expired but still hold units

```text
public void loadBatch(ProductBatch batch) {batches.add(batch);}
```
adds an already existing batch while loading saved data

```text
public void loadIntake(MilkIntake intake) {intakes.add(intake);}
```
adds an already existing intake record while loading saved data

```text
public void loadCounters(double rawMilkLiters, int nextBatchNumber, int nextIntakeNumber) {}
```
restores the raw milk pool and id counters while loading saved data

### class ProductBatch
one production run, with units made and units remaining, production and expiry dates, and liters of milk used. It reports freshness as `FRESH`, `NEAR_EXPIRY`(<= 2 days), or `EXPIRED`. `BY_EXPIRY` is a comparator that puts the earliest expiry first.

### class MilkIntake
A record of raw milk received, used to track how much milk is available for production. MilkIntake has a `id`, `date`, `liters`, `source` and `recordedBy`. 

### 

[not sure if included]
### Transaction abstract class 