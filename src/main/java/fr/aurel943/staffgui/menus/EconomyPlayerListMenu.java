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
 * Même logique que PlayerListMenu (Lot 2), dupliquée volontairement plutôt
 * que réutilisée : PlayerListMenu ouvre PlayerActionMenu (modération),
 * celle-ci ouvre EconomyActionMenu (économie) — deux contextes différents,
 * chaque menu reste responsable d'un seul enchaînement.
 */
public class EconomyPlayerListMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    private static final String TITLE_KEY = "economy-player-list-menu.titre";
    private static final int SLOT_RETOUR = 49;

    private final Map<Integer, UUID> slotToPlayer = new HashMap<>();

    public EconomyPlayerListMenu(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, MainMenu.colored(messages.get(TITLE_KEY)));
        slotToPlayer.clear();

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName(MainMenu.colored("&e" + online.getName()));
            head.setItemMeta(meta);

            inv.setItem(slot, head);
            slotToPlayer.put(slot, online.getUniqueId());
            slot++;
        }

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("economy-player-list-menu.retour"), List.of()));

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
            plugin.getEconomyMenu().open(viewer);
            return;
        }

        UUID targetUuid = slotToPlayer.get(slot);
        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            messages.send(viewer, "economy-player-list-menu.joueur-deconnecte");
            open(viewer);
            return;
        }

        plugin.getEconomyActionMenu().open(viewer, target);
    }
}