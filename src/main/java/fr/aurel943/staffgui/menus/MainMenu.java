package fr.aurel943.staffgui.menus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal de StaffGUI, ouvert via /staff. Quatre catégories : Joueurs,
 * Économie, Ranks, Outils serveur. Économie/Ranks se désactivent proprement
 * si Hub n'est pas installé sur ce serveur.
 *
 * Conçu comme HubMenu côté Hub : un bouton ici + un sous-menu dédié dans sa
 * propre classe + un "case" dans onClick, pour rester facile à étendre.
 */
public class MainMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    private static final String TITLE_KEY = "main-menu.titre";

    private static final int SLOT_JOUEURS = 10;
    private static final int SLOT_ECONOMIE = 12;
    private static final int SLOT_RANKS = 14;
    private static final int SLOT_OUTILS = 16;
    private static final int SLOT_FERMER = 22;

    public MainMenu(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colored(messages.get(TITLE_KEY)));

        boolean hubDispo = plugin.isHubAvailable();

        inv.setItem(SLOT_JOUEURS, buildItem(Material.PLAYER_HEAD,
                messages.get("main-menu.bouton-joueurs"),
                List.of(messages.get("main-menu.bouton-joueurs-lore"))));

        inv.setItem(SLOT_ECONOMIE, buildItem(Material.EMERALD,
                messages.get("main-menu.bouton-economie"),
                List.of(hubDispo
                        ? messages.get("main-menu.bouton-economie-lore")
                        : messages.get("main-menu.module-indisponible"))));

        inv.setItem(SLOT_RANKS, buildItem(Material.NAME_TAG,
                messages.get("main-menu.bouton-ranks"),
                List.of(hubDispo
                        ? messages.get("main-menu.bouton-ranks-lore")
                        : messages.get("main-menu.module-indisponible"))));

        inv.setItem(SLOT_OUTILS, buildItem(Material.COMMAND_BLOCK,
                messages.get("main-menu.bouton-outils"),
                List.of(messages.get("main-menu.bouton-outils-lore"))));

        inv.setItem(SLOT_FERMER, buildItem(Material.BARRIER,
                messages.get("main-menu.bouton-fermer"), List.of()));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String invTitle = legacyTitle(event.getView().title());
        String expectedTitle = colored(messages.get(TITLE_KEY));
        if (!invTitle.equals(expectedTitle)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_JOUEURS ->
                    plugin.getPlayerListMenu().open(player);
            case SLOT_ECONOMIE -> {
                if (!plugin.isHubAvailable()) {
                    messages.send(player, "main-menu.module-indisponible-message");
                    return;
                }
                player.sendMessage(colored("&7[StaffGUI] Menu Économie — arrive au Lot 3."));
            }
            case SLOT_RANKS -> {
                if (!plugin.isHubAvailable()) {
                    messages.send(player, "main-menu.module-indisponible-message");
                    return;
                }
                player.sendMessage(colored("&7[StaffGUI] Menu Ranks — arrive au Lot 4."));
            }
            case SLOT_OUTILS ->
                    player.sendMessage(colored("&7[StaffGUI] Menu Outils — arrive au Lot 5."));
            case SLOT_FERMER -> player.closeInventory();
            default -> { /* rien */ }
        }
    }

    // ---------------------------------------------------------------
    // Utilitaires partagés — réutilisés par les sous-menus des lots suivants
    // ---------------------------------------------------------------

    static ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colored(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colored(line));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    static String colored(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    static String legacyTitle(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}