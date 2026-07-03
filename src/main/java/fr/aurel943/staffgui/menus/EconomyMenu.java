package fr.aurel943.staffgui.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Page d'accueil du module Économie : gérer un joueur précis, ou réinitialiser
 * l'économie entière du serveur. Le reset demande une double confirmation
 * (premier clic = armé, deuxième clic dans les 5 secondes = exécuté) puisque
 * c'est une action irréversible.
 */
public class EconomyMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    private static final String TITLE_KEY = "economy-menu.titre";
    private static final int SLOT_GERER_JOUEUR = 11;
    private static final int SLOT_RESET = 15;
    private static final int SLOT_RETOUR = 22;

    // Admins ayant cliqué une première fois sur Reset, en attente de confirmation
    private final Set<UUID> resetArme = new HashSet<>();

    public EconomyMenu(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MainMenu.colored(messages.get(TITLE_KEY)));

        boolean arme = resetArme.contains(player.getUniqueId());

        inv.setItem(SLOT_GERER_JOUEUR, MainMenu.buildItem(Material.EMERALD,
                messages.get("economy-menu.gerer-joueur"), List.of(messages.get("economy-menu.gerer-joueur-lore"))));

        inv.setItem(SLOT_RESET, MainMenu.buildItem(arme ? Material.TNT : Material.BARRIER,
                messages.get(arme ? "economy-menu.reset-confirmer" : "economy-menu.reset"),
                List.of(messages.get(arme ? "economy-menu.reset-confirmer-lore" : "economy-menu.reset-lore"))));

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("economy-menu.retour"), List.of()));

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
            case SLOT_GERER_JOUEUR -> {
                if (!player.hasPermission("staffgui.economy.view")) { deny(player); return; }
                plugin.getEconomyPlayerListMenu().open(player);
            }
            case SLOT_RESET -> handleReset(player);
            case SLOT_RETOUR -> plugin.getMainMenu().open(player);
            default -> { /* rien */ }
        }
    }

    private void handleReset(Player player) {
        if (!player.hasPermission("staffgui.economy.reset")) { deny(player); return; }

        if (resetArme.contains(player.getUniqueId())) {
            resetArme.remove(player.getUniqueId());
            plugin.getHubPlugin().getDatabaseResetAll();
            messages.send(player, "economy-menu.reset-execute");
            player.closeInventory();
            return;
        }

        resetArme.add(player.getUniqueId());
        messages.send(player, "economy-menu.reset-arme");
        open(player); // rafraîchit pour montrer le bouton "confirmer"

        // Désarme automatiquement après 5 secondes si pas reconfirmé
        Bukkit.getScheduler().runTaskLater(plugin, () -> resetArme.remove(player.getUniqueId()), 100L);
    }

    private void deny(Player player) {
        messages.send(player, "economy-menu.permission-refusee");
    }
}