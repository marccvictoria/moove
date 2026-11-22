# Data Persistence
## exception package
*This package provides a structured, domain-specific exception hierarchy for the Moove application. By analyzing all the business rule violations, security restrictions, and persistence failures through custom exceptions rathen than generic runtime crashes.*

#### class AgriException extends Exception
`AgriException` is the base class (bad input, missing stock, wrong password,etc).
```text
public AgriException(String message, Throwable cause) {super(message, cause);}
```
This preserves the underlying Java exception (such as `IOException` during file reading) for debugging while presenting a clean domain message. Being checked exception forces service methods in `AgriSystem` to explicitly declare `throws AgriException` (or a subclass of this).

##### class ValidationException extends AgriException
Thrown when user-entered form data or method arguments violate business formatting or domain rules (e.g., blank product names, negative prices, invalid quantities, or missing selections). 

#### class InsufficientStockException extends AgriException
Thrown when an operation attempts to consume more raw milk or sell/donate more product units than are currently available in valid, unexpired inventory batches. This is triggered during batch production (`receiveMilk`/`addbatch`) or checkout/donation processing in `AgriSystem` when `availableUnits` and `rawMilkLiters` falls short of the requested amount. 

#### class InsufficientFundsException extends AgriException
Thrown when a cash withdrawal or expense exceeds the current cash balance available in the shop's e-wallet. Triggered in `AgriSystem` during wallet withdrawal operations so the shop balance can never drop below zero.

#### class AuthenticationException extends AgriException
Thrown when a user submits invalid login credentials (unknown username, incorrect password, or empty login fields). Thrown by `AgriSystem.login(...)` and caught directly inside `LoginScreen.login()` to display the red `Incorrect username or password` banner and clear the password field.

#### class PermissionException extends AgriException
Thrown when an authenticated user attempts to execute an action restricted to a higher security role (e.g., a Clerk attempting to void a transaction, modify products, or discard expired stock). 

#### class StorageException extends AgriException
Throws when the persistence layer fails to read from, parse, or write to the local CSV files inside the `data/` folder. This is thrown by `DataStore` and `Csv` on I/O or corrupted-file errors; caught in `Main.start()` during startup to halt the app. If the StorageException occurs, the app will intentionally stop launching.
