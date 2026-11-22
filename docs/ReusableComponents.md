# Reusable Components
## components package
*Stores ready-made UI building blocks (popups, table helpers, and summary cards) that different screens share. Instead of rewriting the same popup or tale code on every screen, screens reuse these tools to keep the design consistent and the code short.*

#### final class Dialogs
Static helpers for message boxes: error, info, confirm, and attempt(action), which runs an action, catches an AgriException and shows it. Applies the stylesheet to every dialog. 

#### class FormDialog
A reusable form pop-up. A screen adds labelled fields (addText, addChoice, addDate, addCheck, addInfo) and calls show(action). If the action throws an AgriException, the dialog stays open and shows the message inline so the user can fix the input. It builds a neat form with text boxes, dropdowns, date pickers, or checkboxes.

#### class InventoryDialogs
The stock dialogs: receiveMilk (popup to log new raw milk deliveries), addBatch (pre-fills the expiry date from the product's shelf life) and discardExpired (asks for confirmation). They are shared by the Inventory and Products screens. Each returns true if something changed so the caller can refresh.

#### final class Tables
A helper that builds table columms in one line of code instead of ten. Text (creates a standard left-aligned text columm), number (right aligned columm for numbers and prices) and chip (a coloured status label like green for `On sale` and red for `Out of stock`). Each table becomes a few readable lines.

#### final class StatCard extends VBox
A summary card with a caption and value, with an optional warning tint. A `StatCard` is a small box used at the top of the dashboards to highlight key numbers like `Raw milk on hand` and other descriptive statistics like `Products out of stock`.

#### class ProductCard extends Button
A clickable card shown on the `SellScreen` for each dairy product. It displays the item's name, price, and remaining stock so clerks can easily click it to add items to a customer's cart. 

#### final class TransactionTable extends TableView<Transaction?>
A ready-made table used across multiple screens to list past sales, milk intakes, donations, and withdrawals. 
