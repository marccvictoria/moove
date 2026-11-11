# App Initialization

## Main
*Extends javafx.application.Application. start() builds the AgriSystem using the folder data, creates a SceneManager, shows the login screen and then the window. If the data files cannot be read, it shows an error dialog and exits.*

#### Application Launch
```text
launch(args);
```
Standard Java Starting point

#### Stage Initialization and Resource Loading
```text
Path dataDir = Path.of("data");
```
`dataDir` points to a local `data` folder where the records/persistence persistent storage (CSV files) are kept

```text
Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Regular.ttf"), 13);
Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Medium.ttf"), 13);
Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-SemiBold.ttf"), 13);
Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Bold.ttf"), 13);
```
Loads custom Quicksand font files from the `resource` folder for consistent format

```text
 AgriSystem system = new AgriSystem(dataDir);
```
loads CSV file in data folder or creating starter data on first run

```text
SceneManager sceneManager = new SceneManager(stage, system);
sceneManager.showLogin();
stage.show();
```
`SceneManager` initializes by main window `stage` and the initialized `system` class. This handles switching between different views/screens in the app.
`showLogin()` renders the Login screen and `stage.show()` makes the main application window visible.

```text
catch (AgriException e)
```
[insert description/refer to exception]

```text
Dialogs.error("Moove application could not start.\n\n" + e.getMessage());
Platform.exit();
```
Pops up a clean error dialog `Dialog.error`, details the issue, and shuts down the JavaFX runtime by `Platform.exit()`
