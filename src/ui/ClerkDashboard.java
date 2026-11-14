package ui;

import service.AgriSystem;
import model.User;

import java.util.Arrays;
import java.util.List;

/* 
 * viewOnly allows manager to open clerk dashboard in a separate window
 */
public class ClerkDashboard extends Dashboard {

    private final boolean viewOnly;

    public ClerkDashboard(SceneManager sceneManager, AgriSystem system, User clerk, boolean viewOnly) {
        super(sceneManager, system, clerk);
        this.viewOnly = viewOnly;
    }

    @Override
    protected String getTitle() {
        return viewOnly ? "Clerk dashboard (view only)" : "Clerk dashboard";
    }

    @Override
    protected boolean isSecondaryWindow() {
        return viewOnly;
    }

    @Override
    protected List<Screen> createScreens() {
        if (viewOnly) {
            return Arrays.asList(
                    new InventoryScreen(system, false),
                    new TransactionScreen(system, user.getUsername(), true),
                    new SellScreen(system, true)); // view only: cannot sell
        }
        return Arrays.asList(
                new InventoryScreen(system, false),
                new TransactionScreen(system, null, false),
                new SellScreen(system, false)); // the clerk's own dashboard
    }
}
