# User Interface
## ui package
*Description*
-----------------------------------NOTES
abstract class Dashboard
class ClerkDashboard extends Dashboard
class ManagerDashBoard extends Dashboard

abstract class Screen
class InventoryScreen extends Screen
class LoginScreen extends Screen
class OverviewScreen extends Screen
class ProductScreen extends Screen

class SceneManager

interface ThrowingAction
-----------------------------------------------------------------------------
#### Screen
Base of every page
```text
```
##### subclass

#### Dashboard
Includes a top bar with logo, title, user name and Logout; a left navigation column with one toggle button per screen; and a content area.
Subclasses implement `createScreens()`

#### ManagerDashboard
Overview, Products, Inventory, Transactions

#### ClerkDashboard
Only Inventory. It takes a `viewOnly` flag so managers can open it read-only 

#### SceneManager
owns the main window. it swaps between the login and the dashboard, opens a clerk's dashboard in a view-only window for managers, and handles logout

#### LoginScreen
the form calls `system.login()` and shows the error online

#### OverviewScreen
stat cards, stock alerts, and the list of clerks

#### ProductScreen
product management and batch/milk management

#### InventoryScreen
product management and batch/milk management. INventory takes a `canEdit` flag

#### interface ThrowingAction 
Intended for an action that may throw an AgriException.