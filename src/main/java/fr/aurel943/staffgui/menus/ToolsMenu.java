package fr.aurel943.staffgui.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;
import fr.aurel943.staffgui.moderation.PendingBroadcastManager;
import fr.aurel943.staffgui.moderation.VanishManager;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

/**
 * Outils "serveur" — pas liés à un joueur ciblé, contrairement aux menus
 * Joueurs/Économie/Ranks. Chaque bouton agit immédiatement (sauf broadcast,
 * qui demande une saisie au clavier comme kick/give/take/set).
 */
public class ToolsMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;
    private final VanishManager vanishManager;
    private final PendingBroadcastManager pendingBroadcastManager;

    private static final String TITLE_KEY = "tools-menu.titre";
    private static final int SLOT_VANISH_SELF = 10;
    private static final int SLOT_BROADCAST = 12;
    private static final int SLOT_RELOAD = 14;
    private static final int SLOT_SETSPAWN = 16;
    private static final int SLOT_SERVER_INFO = 22;

    public ToolsMenu(StaffGUI plugin, VanishManager vanishManager, PendingBroadcastManager pendingBroadcastManager) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.vanishManager = vanishManager;
        this.pendingBroadcastManager = pendingBroadcastManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MainMenu.colored(messages.get(TITLE_KEY)));

        boolean vanishActif = vanishManager.isVanished(player.getUniqueId());
        inv.setItem(SLOT_VANISH_SELF, MainMenu.buildItem(vanishActif ? Material.GLASS : Material.GRAY_STAINED_GLASS,
                messages.get(vanishActif ? "tools-menu.vanish-self-actif" : "tools-menu.vanish-self-inactif"),
                List.of(messages.get("tools-menu.vanish-self-lore"))));

        inv.setItem(SLOT_BROADCAST, MainMenu.buildItem(Material.PAPER,
                messages.get("tools-menu.broadcast"), List.of(messages.get("tools-menu.broadcast-lore"))));

        inv.setItem(SLOT_RELOAD, MainMenu.buildItem(Material.CLOCK,
                messages.get("tools-menu.reload"), List.of(messages.get("tools-menu.reload-lore"))));

        inv.setItem(SLOT_SETSPAWN, MainMenu.buildItem(Material.RESPAWN_ANCHOR,
                messages.get("tools-menu.setspawn"), List.of(messages.get("tools-menu.setspawn-lore"))));

        inv.setItem(SLOT_SERVER_INFO, MainMenu.buildItem(Material.BOOK,
                messages.get("tools-menu.serverinfo"), List.of(messages.get("tools-menu.serverinfo-lore"))));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String invTitle = MainMenu.legacyTitle(event.getView().title());
        String expectedTitle = MainMenu.colored(messages.get(TITLE_KEY));
        if (!invTitle.equals(expectedTitle)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_VANISH_SELF -> handleVanishSelf(player);
            case SLOT_BROADCAST -> handleBroadcast(player);
            case SLOT_RELOAD -> handleReload(player);
            case SLOT_SETSPAWN -> handleSetspawn(player);
            case SLOT_SERVER_INFO -> handleServerInfo(player);
            default -> { /* rien */ }
        }
    }

    private void handleVanishSelf(Player player) {
        if (!player.hasPermission("staffgui.tools.vanish-self")) { deny(player); return; }
        boolean nowVanished = vanishManager.toggle(plugin, player);
        messages.send(player, nowVanished ? "tools-menu.vanish-self-confirme-actif" : "tools-menu.vanish-self-confirme-inactif");
        open(player);
    }

    private void handleBroadcast(Player player) {
        if (!player.hasPermission("staffgui.tools.broadcast")) { deny(player); return; }
        pendingBroadcastManager.setPending(player.getUniqueId());
        player.closeInventory();
        messages.send(player, "tools-menu.broadcast-demande-message");
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("staffgui.tools.reload")) { deny(player); return; }
        plugin.getMessagesManager().reload();
        messages.send(player, "tools-menu.reload-confirme");
    }

    private void handleSetspawn(Player player) {
        if (!player.hasPermission("staffgui.tools.setspawn")) { deny(player); return; }
        World world = player.getWorld();
        world.setSpawnLocation(player.getLocation());
        messages.send(player, "tools-menu.setspawn-confirme", Map.of("monde", world.getName()));
    }

    private void handleServerInfo(Player player) {
        if (!player.hasPermission("staffgui.tools.serverinfo")) { deny(player); return; }

        double tps = Bukkit.getServer().getTPS()[0]; // moyenne sur 1 minute, indice 0
        String tpsFormate = String.format("%.2f", Math.min(tps, 20.0));

        int enLigne = Bukkit.getOnlinePlayers().size();
        int maxJoueurs = Bukkit.getMaxPlayers();

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeFormate = formatUptime(uptimeMs);

        messages.send(player, "tools-menu.serverinfo-affichage", Map.of(
                "tps", tpsFormate,
                "en-ligne", String.valueOf(enLigne),
                "max", String.valueOf(maxJoueurs),
                "uptime", uptimeFormate
        ));
    }

    private String formatUptime(long millis) {
        long heures = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        return heures + "h" + String.format("%02d", minutes);
    }

    private void deny(Player player) {
        messages.send(player, "tools-menu.permission-refusee");
    }
}