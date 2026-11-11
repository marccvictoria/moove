# Reusable Components
## components package
*Description*

#### Dialogs
Static helpers for message boxes: error, info, confirm, and attempt(action), which runs an action, catches an AgriException and shows it. Applies the stylesheet to every dialog.
```text
```
##### subclass

#### FormDialog
A reusable form pop-up. A screen adds labelled fields (addText, addChoice, addDate, addCheck, addInfo) and calls show(action). If the action throws an AgriException, the dialog stays open and shows the message inline so the user can fix the input.

#### InventoryDialogs
The stock dialogs: receiveMilk, addBatch (pre-fills the expiry date from the product's shelf life) and discardExpired (asks for confirmation). They are shared by the Inventory and Products screens. Each returns true if something changed so the caller can refresh.

#### Tables
Column builders: text, number (right aligned) and chip (a coloured status label). Each table becomes a few readable lines.

#### StatCard
A summary card with a caption and value, with an optional warning tint.

[added]
### ProductCard
Product tile for the sell screen

TransactionTable
the history table