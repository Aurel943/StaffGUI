package fr.aurel943.staffgui;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import fr.aurel943.hub.Hub;
import fr.aurel943.staffgui.commands.StaffCommand;
import fr.aurel943.staffgui.database.StaffGUIDatabase;
import fr.aurel943.staffgui.listeners.ChatModerationListener;
import fr.aurel943.staffgui.listeners.MovementFreezeListener;
import fr.aurel943.staffgui.listeners.VanishJoinListener;
import fr.aurel943.staffgui.menus.EconomyActionMenu;
import fr.aurel943.staffgui.menus.EconomyMenu;
import fr.aurel943.staffgui.menus.EconomyPlayerListMenu;
import fr.aurel943.staffgui.menus.MainMenu;
import fr.aurel943.staffgui.menus.PlayerActionMenu;
import fr.aurel943.staffgui.menus.PlayerListMenu;
import fr.aurel943.staffgui.menus.RankActionMenu;
import fr.aurel943.staffgui.menus.RankPlayerListMenu;
import fr.aurel943.staffgui.menus.ToolsMenu;
import fr.aurel943.staffgui.messages.MessagesManager;
import fr.aurel943.staffgui.moderation.FreezeManager;
import fr.aurel943.staffgui.moderation.MuteManager;
import fr.aurel943.staffgui.moderation.PendingBroadcastManager;
import fr.aurel943.staffgui.moderation.PendingEconomyManager;
import fr.aurel943.staffgui.moderation.PendingKickManager;
import fr.aurel943.staffgui.moderation.VanishManager;

import java.io.File;

public class StaffGUI extends JavaPlugin {

    private Hub hubPlugin;
    private MessagesManager messagesManager;

    private StaffGUIDatabase database;
    private FreezeManager freezeManager;
    private VanishManager vanishManager;
    private MuteManager muteManager;
    private PendingKickManager pendingKickManager;
    private PendingEconomyManager pendingEconomyManager;
    private PendingBroadcastManager pendingBroadcastManager;

    private MainMenu mainMenu;
    private PlayerListMenu playerListMenu;
    private PlayerActionMenu playerActionMenu;
    private EconomyMenu economyMenu;
    private EconomyPlayerListMenu economyPlayerListMenu;
    private EconomyActionMenu economyActionMenu;
    private RankPlayerListMenu rankPlayerListMenu;
    private RankActionMenu rankActionMenu;
    private ToolsMenu toolsMenu;

    @Override
    public void onEnable() {
        saveDefaultResourceIfMissing("config/messages.yml");
        messagesManager = new MessagesManager(this);

        Plugin found = getServer().getPluginManager().getPlugin("Hub");
        if (found instanceof Hub hub) {
            hubPlugin = hub;
            getLogger().info("Hub détecté — modules Économie et Ranks activés.");
        } else {
            getLogger().warning("Hub introuvable sur ce serveur — modules Économie et Ranks désactivés.");
        }

        database = new StaffGUIDatabase(getDataFolder(), getLogger());
        database.connect(this);

        freezeManager = new FreezeManager();
        vanishManager = new VanishManager();
        muteManager = new MuteManager(database);
        pendingKickManager = new PendingKickManager();
        pendingEconomyManager = new PendingEconomyManager();
        pendingBroadcastManager = new PendingBroadcastManager();

        mainMenu = new MainMenu(this);
        playerListMenu = new PlayerListMenu(this);
        playerActionMenu = new PlayerActionMenu(this, freezeManager, vanishManager, muteManager, pendingKickManager);
        economyMenu = new EconomyMenu(this);
        economyPlayerListMenu = new EconomyPlayerListMenu(this);
        economyActionMenu = new EconomyActionMenu(this, pendingEconomyManager);
        rankPlayerListMenu = new RankPlayerListMenu(this);
        rankActionMenu = new RankActionMenu(this);
        toolsMenu = new ToolsMenu(this, vanishManager, pendingBroadcastManager);

        getServer().getPluginManager().registerEvents(mainMenu, this);
        getServer().getPluginManager().registerEvents(playerListMenu, this);
        getServer().getPluginManager().registerEvents(playerActionMenu, this);
        getServer().getPluginManager().registerEvents(economyMenu, this);
        getServer().getPluginManager().registerEvents(economyPlayerListMenu, this);
        getServer().getPluginManager().registerEvents(economyActionMenu, this);
        getServer().getPluginManager().registerEvents(rankPlayerListMenu, this);
        getServer().getPluginManager().registerEvents(rankActionMenu, this);
        getServer().getPluginManager().registerEvents(toolsMenu, this);
        getServer().getPluginManager().registerEvents(new MovementFreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(
                new ChatModerationListener(this, muteManager, pendingKickManager, pendingEconomyManager, pendingBroadcastManager), this);
        getServer().getPluginManager().registerEvents(new VanishJoinListener(this, vanishManager), this);

        getCommand("staff").setExecutor(new StaffCommand(this));

        getLogger().info("StaffGUI activé.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.disconnect();
        }
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
    public PlayerListMenu getPlayerListMenu() { return playerListMenu; }
    public PlayerActionMenu getPlayerActionMenu() { return playerActionMenu; }
    public EconomyMenu getEconomyMenu() { return economyMenu; }
    public EconomyPlayerListMenu getEconomyPlayerListMenu() { return economyPlayerListMenu; }
    public EconomyActionMenu getEconomyActionMenu() { return economyActionMenu; }
    public RankPlayerListMenu getRankPlayerListMenu() { return rankPlayerListMenu; }
    public RankActionMenu getRankActionMenu() { return rankActionMenu; }
    public ToolsMenu getToolsMenu() { return toolsMenu; }
}