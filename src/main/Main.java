/** *********************************************************
 * 
 * Moove
 * Program Description: A dairy-based agricultural data management system.
 * AI Use Disclosure:
 * Google AI Overview/Gemini assisted with errors that occurred during development. 
 * Additionally, suggestions from it are also used in styling the app that align with our
 * app design.
 * 
 * @author Marc Tristan Victoria
 * @author	Denise Jade Barela
 * @created_date 2026-10-02 20:00 
 * 
 * (c) Institute of Computer Science, UPLB
 * */

package main;

import exception.AgriException;
import service.AgriSystem;
import ui.SceneManager;
import components.Dialogs;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.application.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;

// entry point of app
public class Main extends Application {

    @Override
    public void start(Stage stage) {
    	
        Path dataDir = Path.of("data");
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Regular.ttf"), 13);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Medium.ttf"), 13);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-SemiBold.ttf"), 13);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Quicksand-Bold.ttf"), 13);
            
            AgriSystem system = new AgriSystem(dataDir); // loads csv file in data folder or creating starter data on first run
            SceneManager sceneManager = new SceneManager(stage, system);
            sceneManager.showLogin();
            stage.show();
        } catch (AgriException e) {
            Dialogs.error("Moove application could not start.\n\n" + e.getMessage());
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
