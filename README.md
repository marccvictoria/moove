# Moo-ve Dairy

**Moving local dairy forward.**

*Moo-ve Dairy is a desktop management application designed to track raw milk deliveries, manage product inventory, and handle daily transactions for local dairy operations.*

## Overview

*Moo-ve helps manage the flow of dairy products from raw milk delivery to sales. It gives clerks and managers a simple dashboard to oversee inventory levels, handle sales, record production batches, and monitor financial transactions.*

## Features

### Product Management
- Create and manage dairy products like cheeses, milk, and butter.
- Organize products by category.
- Update product information and pricing.
- Activate or deactivate products.
- Search and browse the product catalog.

### Inventory Management
- Track raw milk inventory and deliveries.
- Record production batches and restocking activities.
- Monitor stock levels by product and batch.
- Track product freshness and expiration dates.
- Apply earliest-expiry-first inventory allocation.
- Record expired stock write-offs.
- Manage financial movements like cash withdrawals

## Architecture


```text
src/
├── main/          # Application entry point
├── ui/            # Screens and dashboards
├── components/    # Reusable UI components
├── service/       # Business logic and data storage
├── model/         # Domain models and entities
├── exception/     # Custom exception classes
└── util/          # Validation and formatting helpers

resources/
├── css/           # Application stylesheets
├── fonts/         # Bundled fonts
└── images/        # Logos and visual assets

data/              # CSV data files
```

### Application Flow

The UI communicates with the system through `AgriSystem`, which handles permissions, validation, and business operations before changes are persisted.

## Code Walkthrough

- **Application initialization** – `main.Main` [AppInitialization.md](AppInitialization.md) 
- **Domain models** – `model` [DomainModels.md](DomainModels.md) 
- **Business logic** – `service.AgriSystem` [BusinessLogic.md](BusinessLogic.md) 
- **User interface** – `ui` [UserInterface.md](UserInterface.md) 
- **Reusable components** – `components` [ReusableComponents.md](ReusableComponents.md) 
- **Data persistence** – `service.DataStore` and `service.Csv` [DataPersistence.md](DataPersistence.md) 
- **Validation and formatting** – `util` [Validation.md](Validation.md) 

## Data Storage

Moo-ve uses local CSV files to store application data.

The `data/` directory is generated automatically on the first run.

| File | Description |
|---|---|
| users.csv | User accounts and roles |
| products.csv | Product catalog |
| batches.csv | Production batches and stock |
| intakes.csv | Raw milk delivery records |
| transactions.csv | Financial and inventory transactions |
| sale_items.csv | Individual sale items |
| meta.csv | System counters and inventory metadata |

To restore the starter data, close the application and delete the `data/` directory.

## Contributors
**Marc Tristan Victoria (https://github.com/marccvictoria)**

**Denise Jade Barela (https://github.com/denlxe)**
