package fr.aurel943.staffgui;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import fr.aurel943.hub.Hub;
import fr.aurel943.staffgui.commands.StaffCommand;
import fr.aurel943.staffgui.menus.MainMenu;
import fr.aurel943.staffgui.messages.MessagesManager;

import java.io.File;

public class StaffGUI extends JavaPlugin {

    private Hub hubPlugin;
    private MessagesManager messagesManager;
    private MainMenu mainMenu;

    @Override
    public void onEnable() {
        saveDefaultResourceIfMissing("config/messages.yml");
        messagesManager = new MessagesManager(this);

        // softdepend garantit que Hub est déjà chargé à ce stade s'il est présent
        // sur ce serveur. StaffGUI reste utilisable sans lui (menu Joueurs/Outils
        // fonctionnent quand même), mais Économie/Ranks se désactivent proprement.
        Plugin found = getServer().getPluginManager().getPlugin("Hub");
        if (found instanceof Hub hub) {
            hubPlugin = hub;
            getLogger().info("Hub détecté — modules Économie et Ranks activés.");
        } else {
            getLogger().warning("Hub introuvable sur ce serveur — modules Économie et Ranks désactivés.");
        }

        mainMenu = new MainMenu(this);
        getServer().getPluginManager().registerEvents(mainMenu, this);

        getCommand("staff").setExecutor(new StaffCommand(this));

        getLogger().info("StaffGUI activé.");
    }

    @Override
    public void onDisable() {
        getLogger().info("StaffGUI désactivé.");
    }

    private void saveDefaultResourceIfMissing(String name) {
        File target = new File(getDataFolder(), name);
        if (!target.exists()) {
            saveResource(name, false);
        }
    }

    public Hub getHubPlugin() { return hubPlugin; }

    public boolean isHubAvailable() { return hubPlugin != null; }

    public MessagesManager getMessagesManager() { return messagesManager; }

    public MainMenu getMainMenu() { return mainMenu; }
}