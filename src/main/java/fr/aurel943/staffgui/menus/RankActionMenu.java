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
import fr.aurel943.hub.economy.Database;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Affiche le rank actuel d'un joueur ciblé (tête, slot 4) + tous les ranks
 * existants en dessous, triés par poids décroissant (comme /rank list côté
 * Hub). Clic sur un rank = assignation immédiate, pas de confirmation
 * supplémentaire (contrairement au reset économie, changer un rank est
 * réversible en un clic).
 */
public class RankActionMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    private static final int SLOT_TETE = 4;
    private static final int SLOT_RETOUR = 8;
    private static final int PREMIER_SLOT_RANK = 9;

    private final Map<UUID, UUID> viewerTarget = new HashMap<>();
    // slot -> rank_id affiché à ce slot, réinitialisé à chaque open()
    private final Map<Integer, String> slotToRankId = new HashMap<>();

    public RankActionMenu(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    public void open(Player viewer, Player target) {
        viewerTarget.put(viewer.getUniqueId(), target.getUniqueId());
        slotToRankId.clear();

        Inventory inv = Bukkit.createInventory(null, 54, MainMenu.colored(
                messages.get("rank-action-menu.titre-prefixe") + target.getName()));

        var rankManager = plugin.getHubPlugin().getRankManager();
        Database.RankData rankActuel = rankManager.getPlayerRank(target.getUniqueId());

        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) tete.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(MainMenu.colored("&e" + target.getName()));
        skullMeta.setLore(List.of(MainMenu.colored(messages.get("rank-action-menu.rank-actuel-lore",
                Map.of("rank", rankActuel != null ? rankActuel.rankId : "?")))));
        tete.setItemMeta(skullMeta);
        inv.setItem(SLOT_TETE, tete);

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("rank-action-menu.retour"), List.of()));

        List<Database.RankData> ranksTries = new ArrayList<>(rankManager.getAllRanks());
        ranksTries.sort((a, b) -> Integer.compare(b.poids, a.poids));

        int slot = PREMIER_SLOT_RANK;
        for (Database.RankData rank : ranksTries) {
            if (slot >= 54) break;
            boolean estActuel = rankActuel != null && rank.rankId.equals(rankActuel.rankId);

            List<String> lore = new ArrayList<>();
            lore.add(messages.get("rank-action-menu.rank-poids-lore", Map.of("poids", String.valueOf(rank.poids))));
            lore.add(messages.get("rank-action-menu.rank-permissions-lore",
                    Map.of("nb", String.valueOf(rank.permissions.size()))));
            lore.add(estActuel
                    ? messages.get("rank-action-menu.rank-deja-actif")
                    : messages.get("rank-action-menu.rank-cliquer-assigner"));

            String nomAffiche = rank.prefix.isEmpty() ? rank.rankId : rank.prefix + " &7(" + rank.rankId + ")";
            ItemStack item = MainMenu.buildItem(Material.NAME_TAG, nomAffiche, lore);
            if (estActuel) {
                var meta = item.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slotToRankId.put(slot, rank.rankId);
            slot++;
        }

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        String invTitle = MainMenu.legacyTitle(event.getView().title());
        if (!invTitle.startsWith(MainMenu.colored(messages.get("rank-action-menu.titre-prefixe")))) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        UUID targetUuid = viewerTarget.get(viewer.getUniqueId());
        if (targetUuid == null) return;

        if (slot == SLOT_RETOUR) {
            plugin.getRankPlayerListMenu().open(viewer);
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            messages.send(viewer, "rank-action-menu.cible-deconnectee");
            viewer.closeInventory();
            return;
        }

        String rankId = slotToRankId.get(slot);
        if (rankId == null) return;

        if (!viewer.hasPermission("staffgui.ranks.edit")) {
            messages.send(viewer, "rank-action-menu.permission-refusee");
            return;
        }

        plugin.getHubPlugin().getRankManager().setPlayerRank(target.getUniqueId(), rankId);
        messages.send(viewer, "rank-action-menu.assignation-confirme",
                Map.of("joueur", target.getName(), "rank", rankId));
        open(viewer, target); // rafraîchit pour montrer le nouveau rank actif en surbrillance
    }
}