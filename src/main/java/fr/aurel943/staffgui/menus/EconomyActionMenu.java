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
import fr.aurel943.staffgui.moderation.PendingEconomyAction;
import fr.aurel943.staffgui.moderation.PendingEconomyManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Affiche le solde d'un joueur ciblé + 3 boutons (Give/Take/Set). Chaque
 * bouton ferme l'inventaire et attend un montant tapé dans le chat, capturé
 * par ChatModerationListener (même pattern que le kick au Lot 2).
 */
public class EconomyActionMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;
    private final PendingEconomyManager pendingEconomyManager;

    private static final int SLOT_TETE = 4;
    private static final int SLOT_GIVE = 11;
    private static final int SLOT_TAKE = 13;
    private static final int SLOT_SET = 15;
    private static final int SLOT_RETOUR = 22;

    private final Map<UUID, UUID> viewerTarget = new HashMap<>();

    public EconomyActionMenu(StaffGUI plugin, PendingEconomyManager pendingEconomyManager) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.pendingEconomyManager = pendingEconomyManager;
    }

    public void open(Player viewer, Player target) {
        viewerTarget.put(viewer.getUniqueId(), target.getUniqueId());

        int solde = (int) plugin.getHubPlugin().getEconomyManager().getBalance(target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, MainMenu.colored(
                messages.get("economy-action-menu.titre-prefixe") + target.getName()));

        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) tete.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(MainMenu.colored("&e" + target.getName()));
        skullMeta.setLore(List.of(MainMenu.colored(messages.get("economy-action-menu.solde-lore",
                Map.of("solde", String.valueOf(solde))))));
        tete.setItemMeta(skullMeta);
        inv.setItem(SLOT_TETE, tete);

        inv.setItem(SLOT_GIVE, MainMenu.buildItem(Material.EMERALD,
                messages.get("economy-action-menu.give"), List.of(messages.get("economy-action-menu.give-lore"))));

        inv.setItem(SLOT_TAKE, MainMenu.buildItem(Material.REDSTONE,
                messages.get("economy-action-menu.take"), List.of(messages.get("economy-action-menu.take-lore"))));

        inv.setItem(SLOT_SET, MainMenu.buildItem(Material.NETHER_STAR,
                messages.get("economy-action-menu.set"), List.of(messages.get("economy-action-menu.set-lore"))));

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("economy-action-menu.retour"), List.of()));

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        String invTitle = MainMenu.legacyTitle(event.getView().title());
        if (!invTitle.startsWith(MainMenu.colored(messages.get("economy-action-menu.titre-prefixe")))) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        UUID targetUuid = viewerTarget.get(viewer.getUniqueId());
        if (targetUuid == null) return;

        if (slot == SLOT_RETOUR) {
            plugin.getEconomyPlayerListMenu().open(viewer);
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            messages.send(viewer, "economy-action-menu.cible-deconnectee");
            viewer.closeInventory();
            return;
        }

        PendingEconomyAction.Type type = switch (slot) {
            case SLOT_GIVE -> PendingEconomyAction.Type.GIVE;
            case SLOT_TAKE -> PendingEconomyAction.Type.TAKE;
            case SLOT_SET -> PendingEconomyAction.Type.SET;
            default -> null;
        };
        if (type == null) return;

        if (!viewer.hasPermission("staffgui.economy.edit")) {
            messages.send(viewer, "economy-action-menu.permission-refusee");
            return;
        }

        pendingEconomyManager.setPending(viewer.getUniqueId(), new PendingEconomyAction(targetUuid, type));
        viewer.closeInventory();
        messages.send(viewer, "economy-action-menu.demande-montant",
                Map.of("action", type.name(), "joueur", target.getName()));
    }
}