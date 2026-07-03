package fr.aurel943.staffgui.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Liste des joueurs en ligne (tête + nom), clic → ouvre PlayerActionMenu
 * pour ce joueur. Pas de pagination pour l'instant (45 slots utilisables,
 * largement suffisant tant que le serveur ne dépasse pas ce nombre de
 * joueurs simultanés — à revoir si besoin plus tard).
 */
public class PlayerListMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    private static final String TITLE_KEY = "player-list-menu.titre";
    private static final int SLOT_RETOUR = 49;

    // slot -> UUID du joueur affiché à ce slot, réinitialisé à chaque open()
    private final Map<Integer, UUID> slotToPlayer = new HashMap<>();

    public PlayerListMenu(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, MainMenu.colored(messages.get(TITLE_KEY)));
        slotToPlayer.clear();

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break; // au-delà, la dernière rangée reste réservée à la nav
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName(MainMenu.colored("&e" + online.getName()));
            head.setItemMeta(meta);

            inv.setItem(slot, head);
            slotToPlayer.put(slot, online.getUniqueId());
            slot++;
        }

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("player-list-menu.retour"), List.of()));

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        String invTitle = MainMenu.legacyTitle(event.getView().title());
        String expectedTitle = MainMenu.colored(messages.get(TITLE_KEY));
        if (!invTitle.equals(expectedTitle)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == SLOT_RETOUR) {
            plugin.getMainMenu().open(viewer);
            return;
        }

        UUID targetUuid = slotToPlayer.get(slot);
        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            messages.send(viewer, "player-list-menu.joueur-deconnecte");
            open(viewer); // rafraîchit la liste
            return;
        }

        plugin.getPlayerActionMenu().open(viewer, target);
    }
}