# User Interface
## ui package
*all interface views*

### abstract class Screen
Declares an abstract base class. Specific screens like Dashboard, Inventory, or Sales will extend it

```text
private Parent view;
```
Caches the root UI node tree so the screen is only built once
<br><br>
`getName()` forces subclasses to provide a string name for the screen and `build()` forces subclasses to implement their own layout of screen. Only screens with dynamic data will override `refresh()` to reload data whenever they become visible.

```text
public final Parent getView() {}
```
This makes the UI being constructed only once which is the very first time it requested. Every subsequent switch to that screen will then be instantaneous since it reuses the already-built node tree from memory.

```text
protected static Parent page(String title, String subtitle, Node... content) {}
```
A utility method that wraps any screen content into a standardized, consistent layout structure. This builds page layout.

### abstract class Dashboard
Includes a top bar with logo, title, user name and Logout; a left navigation columm with one toggle button per screen; and a content area.
Subclasses implement `createScreens()`. 
<br><br>
When a user signs in, the application passes three pieces of data into the `Dashboard` constructor: `User` or the currently authenticated account, `AgriSystem` or the central backend engine reference, and `SceneManager`, the window controller reference.
<br><br>
`Dashboard` is an abstract controller template. It cannot be used directly but provides the standard UI fraame for specialized dashboards. `Dashboard` holds references to `sceneManager` (for window switching and logging out), `system`, and the `user` (the currently authenticated account.
<br><br>
`getTitle()` and `createScreens()` are abstract methods that must be implemented by a particular dashboard (e.g. a ClerkDasboard, ManagerDashboard) to define their custom window title and list of accessible tabs(screen).

```text
public void build() {
root = new BorderPane()
...
}
```
In `Dashboard`, this code uses a JavaFX `BorderPane` layout,which divides the window into structural sections: Top(`root.setTop`), Left(`root.setLeft`), and Center(`root.setCenter`). `Vbox nav` sets up a vertical box `VBox` with its desired format. `ToggleGroup` ensures that only one navigation button can be selected at a time like radio buttons.

```text
public void show(Screen screen) {}
```
Switches the content area to a screen and refreshes its data.

```text
private HBox buildTopBar() {}
```
`Logo` and `Label`s loads the application icon image, stes up the brand label `Moo-ve Dairy`, and fetches the dynamic window title 	`getTitle()`.`spacer` is a flexible layout element configured with `HBox.setHgrow(spacer, Priority.ALWAYS) that pushes elements to the far left and right sides of the top bar. `who` variable will display the name of the user and its role. And `Button exit` either says `Close` or `Log out` depending on whether it's a secondary window.
#### ManagerDashboard
Overview, Products, Inventory, Transactions

#### class ClerkDashboard extends Dashboard
`ClerkDashboard` automatically inherits the top navigation bar, side panel, and lazy-loading window structure(`getRoot()`)
Only Inventory. It takes a `viewOnly` flag so managers can open it read-only. 
```text
private final boolean viewOnly();
```
This tracks whether this dashboard is being opened in a normal active work mode (for a clerk) or a read-only inspection mode (for the manager).

```text
protected String getTitle(){}
```
If clerk dashboard, view only, otherwise display the standard "Clerk Dashboard" on the top bar. Clerk also overrides `isSecondaryWindow()` to `viewOnly` value from the parent DashBoard. If its`true` for `viewOnly` (meaning it is opened by a manager) then it tells the top bar to treat it as a secondary window.

```text
protected List<Screen> createScreans(){}
```
Also overriden in `ClerkDashboard`. Returns a fixed list containing `InventoryScreen` to view stock details and batches and `SellScreen` to process customer cart checkout and sales.

### class SceneManager
Owns the main window. It swaps between the login and the dashboard, opens a clerk's dashboard in a view-only window for managers, and handles logout

```text
public SceneManager(Stage stage, AgriSystem system){}
```
Initialization stores the primary window `stage` and backend `system` references

```text
scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
```
`SceneManager` constructor attaches the master application stysheet(`app.css`) to the scene, applies the window icon to the stage, and sets minimum window size limits (1020 x 660) to prevent the UI from breaking on small screens.

```text
Dialogs.setOwner(stage);
```
This ensures that any popup alerts or dialog boxes correctly anchor themselves to the main application window.

```text
public void showLogin(){}
```
`showLogin()` handles switching the application back to the login state(such as on stratup or after logging out). `scene.setRoot(...)` swaps the scene's active view to a newly instandiated `LoginScreen`, passing references to the `SceneManager(this)` and `system`

```text
public void showDashboardFor(User user){}
```
This determines which dashboard to load based on the user's security role.

```text
public void openClerkWindow(User clerk) {}
```
This lets the manager open clerk's dashboard in a separate view-only window.

```text
pulic void logout(){}
```
Handles user signing out. Clears the active user session in `AgriSystem`. Also immediately calls `showLogin() to transistion the window back to the login screen.

#### class LoginScreen extends Screen
The form calls `system.login()` and shows the error online. Inherits from `Screen` class so it utilizes the lazy-loading view pattern (`getView()`). Its initialization receives and stores references to the 	SceneManager` and `AgriSystem`.
<br><br>
`getName()` overriden returning `Login` as the identifier for this view.
<br><br>
In `protected Parent build(){}` of this class, it loads the Moove logo image(`moove-logo.png`), scales it to 300 pixels wide while keeping aspect ratio, and adds stylized tagline and welcome labels.

#### class OverviewScreen extends Screen
Stat cards, stock alerts, and the list of clerks
`getName()` returns "Overview" so `Overview` is the tab name for the manager's sidebar navigation. Has four stat cards aligned horizontally withconsistent gap, wrapping them neatly if window size changes. Stat Cards include `productCard`, 	milkCard`, `nearCard`, and `outCard`. `stockAlert` is a warning banner label to highlight expired stock or near-expiry batches. `clerkTable` is a table displaying all registeres clerk accounts.

```text
FlowPane stats = new FlowPane(14, 14, productsCard, milkCard, nearCard, outCard);
```
`FlowPane stats` arranges the four stat cards horizontally in `protected Parent build()` 
<br><br> 
```text
private void buildClerkTable(){}
```
This constructs the table columms and sets a prefereed height. It has columms for the clerk's display name and username using utility helpers from `Tables`.

```text
button.setOnAction(e -> sceneManager.openClerkWindow(item));
```
This line of code from `buildClerktable()` creates a custom table columm that embeds an interactive `Open dashboard` button inside each row.

```text
productsCard.setValue(Integer.toString(system.getActiveProducts().size()));
milkCard.setValue(String.format("%,.1f L",  system.getInventory().getRawMilkLiters()));
```
This line of code in `public void refresh(){}` refreshes the active product count and raw milk storage volume and update their respective statistic cards.
<br><br>
The List<ProductBatch> `near` and `expiry` query the inventory for batches nearing expiration date, update the counter on the stat card, and trigger a visual waring `.setWarning()`. This `refresh()` method also loops through active products, counts how many have zero stocks, updates the out-of-stock card, and flags a visual warning.

#### class ProductScreen extends Screen
product management and batch/milk management
The Table Columms uses `build` for the nine columms: `ID`, `Name`, `Category`, `Description`, `Price`, `Unit`, `Shelf life`, `In stock`, and `Status`. It applies `.setColumnResizePolicy()` which sets each columm's minimum size.
```text
search.setPromptText("Search products...");
search.setMaxWidth(260);
search.textProperty().addListener((o, a, b) -> applyFilter());
```
This makes `applyFilter()` run immediately on every keystrokes.
<br><br>
Toolbar Buttons besides the `Search producst...` are `Add product` which opens a creation dialog, and the `Edit selected`, `Show/hide on sale`, and `Add stock for selected` which all acts inside `withSelection(...)` to verify a table row is clicked/highlighted first. 

#### class InventoryScreen extends Screen
This displays real time raw milk volume, sellable product stock, and expiration status across three organized tabs. For manage, it enables action buttons to receive raw milk, record new production batches, and discard expired stocks. On the other hand, this only serves as a read-only stock monitor for a clerk.
```text
HBox stats = new HBox(14, milkCard, unitsCard, nearCard, expiredCard);
```
This groups the four summary metric cards `StatCard` horizontally to the display `Raw milk on hand`, `Unit in stock`, `Batches near expiry`, and `Expired units`
```text
if (canEdit) {
    Button receive = new Button("Receive milk");
    receive.setOnAction(e -> changed(InventoryDialogs.receiveMilk(system)));
    Button add = new Button("Add batch / restock");
    add.setOnAction(e -> changed(InventoryDialogs.addBatch(system, null)));
    Button discard = new Button("Discard expired");
    discard.setOnAction(e -> changed(InventoryDialogs.discardExpired(system)));
    body.getChildren().add(new HBox(10, receive, add, discard));
}
```
The `canEdit` permission only allows the Manager. Hence, this toolar is only visible for the manager. `Receive milk`, `Add batch / restock`, and `Discard expired` can only be visible and used by the manager.
```text
TabPane tabs = new TabPane(
        tab("Batches (earliest expiry first)", batches),
        tab("Stock by product", stockTable),
        tab("Milk intake log", intakeTable));
```
This organizes the three inventory views. The `Batches (earliest expiry first)` which offers another filtering arrangement of showing the most recent emptied and discarded batches, the `Stock by product`, and the `Milk intake log`.
```text
private String stockStatus(Product p) {
```
This method evaluates each product's stock health in priority order to drive the color-coded status badges. 

#### interface ThrowingAction 
Intended for an action that may throw an AgriException.
