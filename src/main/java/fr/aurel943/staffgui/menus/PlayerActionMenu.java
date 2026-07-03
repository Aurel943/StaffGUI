package fr.aurel943.staffgui.menus;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;
import fr.aurel943.staffgui.moderation.FreezeManager;
import fr.aurel943.staffgui.moderation.MuteManager;
import fr.aurel943.staffgui.moderation.PendingKickManager;
import fr.aurel943.staffgui.moderation.VanishManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sous-menu d'actions de modération pour UN joueur ciblé, ouvert depuis
 * PlayerListMenu. Chaque bouton vérifie sa propre permission staffgui.players.*
 * avant d'agir (déclarées dans plugin.yml).
 */
public class PlayerActionMenu implements Listener {

    private final StaffGUI plugin;
    private final MessagesManager messages;
    private final FreezeManager freezeManager;
    private final VanishManager vanishManager;
    private final MuteManager muteManager;
    private final PendingKickManager pendingKickManager;

    private static final String TITLE_KEY = "player-action-menu.titre";

    private static final int SLOT_TETE = 4;
    private static final int SLOT_TP_VERS = 10;
    private static final int SLOT_FAIRE_VENIR = 11;
    private static final int SLOT_GAMEMODE = 12;
    private static final int SLOT_HEAL = 13;
    private static final int SLOT_FREEZE = 14;
    private static final int SLOT_VANISH = 15;
    private static final int SLOT_INVSEE = 16;
    private static final int SLOT_CLEAR = 19;
    private static final int SLOT_KICK = 20;
    private static final int SLOT_MUTE = 21;
    private static final int SLOT_RETOUR = 26;

    // viewer_uuid -> target_uuid, pour savoir sur qui agir quand ce viewer clique
    private final Map<UUID, UUID> viewerTarget = new HashMap<>();

    public PlayerActionMenu(StaffGUI plugin, FreezeManager freezeManager, VanishManager vanishManager,
                            MuteManager muteManager, PendingKickManager pendingKickManager) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.freezeManager = freezeManager;
        this.vanishManager = vanishManager;
        this.muteManager = muteManager;
        this.pendingKickManager = pendingKickManager;
    }

    public void open(Player viewer, Player target) {
        viewerTarget.put(viewer.getUniqueId(), target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, MainMenu.colored(
                messages.get("player-action-menu.titre-prefixe") + target.getName()));

        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) tete.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(MainMenu.colored("&e" + target.getName()));
        tete.setItemMeta(skullMeta);
        inv.setItem(SLOT_TETE, tete);

        inv.setItem(SLOT_TP_VERS, MainMenu.buildItem(Material.ENDER_PEARL,
                messages.get("player-action-menu.tp-vers"), List.of(messages.get("player-action-menu.tp-vers-lore"))));

        inv.setItem(SLOT_FAIRE_VENIR, MainMenu.buildItem(Material.ENDER_EYE,
                messages.get("player-action-menu.faire-venir"), List.of(messages.get("player-action-menu.faire-venir-lore"))));

        inv.setItem(SLOT_GAMEMODE, MainMenu.buildItem(Material.GRASS_BLOCK,
                messages.get("player-action-menu.gamemode"), List.of(messages.get("player-action-menu.gamemode-lore",
                        Map.of("mode", target.getGameMode().name())))));

        inv.setItem(SLOT_HEAL, MainMenu.buildItem(Material.GOLDEN_APPLE,
                messages.get("player-action-menu.heal"), List.of(messages.get("player-action-menu.heal-lore"))));

        boolean frozen = freezeManager.isFrozen(target.getUniqueId());
        inv.setItem(SLOT_FREEZE, MainMenu.buildItem(frozen ? Material.ICE : Material.PACKED_ICE,
                messages.get(frozen ? "player-action-menu.freeze-actif" : "player-action-menu.freeze-inactif"),
                List.of(messages.get("player-action-menu.freeze-lore"))));

        boolean vanished = vanishManager.isVanished(target.getUniqueId());
        inv.setItem(SLOT_VANISH, MainMenu.buildItem(vanished ? Material.GLASS : Material.GRAY_STAINED_GLASS,
                messages.get(vanished ? "player-action-menu.vanish-actif" : "player-action-menu.vanish-inactif"),
                List.of(messages.get("player-action-menu.vanish-lore"))));

        inv.setItem(SLOT_INVSEE, MainMenu.buildItem(Material.CHEST,
                messages.get("player-action-menu.invsee"), List.of(messages.get("player-action-menu.invsee-lore"))));

        inv.setItem(SLOT_CLEAR, MainMenu.buildItem(Material.LAVA_BUCKET,
                messages.get("player-action-menu.clear"), List.of(messages.get("player-action-menu.clear-lore"))));

        inv.setItem(SLOT_KICK, MainMenu.buildItem(Material.BARRIER,
                messages.get("player-action-menu.kick.bouton"), List.of(messages.get("player-action-menu.kick.bouton-lore"))));

        boolean muted = muteManager.isMuted(target.getUniqueId());
        inv.setItem(SLOT_MUTE, MainMenu.buildItem(muted ? Material.PAPER : Material.BOOK,
                messages.get(muted ? "player-action-menu.mute-actif" : "player-action-menu.mute-inactif"),
                List.of(messages.get("player-action-menu.mute-lore"))));

        inv.setItem(SLOT_RETOUR, MainMenu.buildItem(Material.ARROW, messages.get("player-action-menu.retour"), List.of()));

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        String invTitle = MainMenu.legacyTitle(event.getView().title());
        if (!invTitle.startsWith(MainMenu.colored(messages.get("player-action-menu.titre-prefixe")))) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        UUID targetUuid = viewerTarget.get(viewer.getUniqueId());
        if (targetUuid == null) return;
        Player target = Bukkit.getPlayer(targetUuid);

        if (slot == SLOT_RETOUR) {
            plugin.getPlayerListMenu().open(viewer);
            return;
        }

        if (target == null) {
            messages.send(viewer, "player-action-menu.cible-deconnectee");
            viewer.closeInventory();
            return;
        }

        switch (slot) {
            case SLOT_TP_VERS -> handleTpVers(viewer, target);
            case SLOT_FAIRE_VENIR -> handleFaireVenir(viewer, target);
            case SLOT_GAMEMODE -> handleGamemode(viewer, target);
            case SLOT_HEAL -> handleHeal(viewer, target);
            case SLOT_FREEZE -> handleFreeze(viewer, target);
            case SLOT_VANISH -> handleVanish(viewer, target);
            case SLOT_INVSEE -> handleInvsee(viewer, target);
            case SLOT_CLEAR -> handleClear(viewer, target);
            case SLOT_KICK -> handleKick(viewer, target);
            case SLOT_MUTE -> handleMute(viewer, target);
            default -> { /* rien */ }
        }
    }

    private void handleTpVers(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.teleport")) { deny(viewer); return; }
        viewer.teleport(target.getLocation());
        messages.send(viewer, "player-action-menu.tp-vers-confirme", Map.of("joueur", target.getName()));
        viewer.closeInventory();
    }

    private void handleFaireVenir(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.teleport")) { deny(viewer); return; }
        target.teleport(viewer.getLocation());
        messages.send(viewer, "player-action-menu.faire-venir-confirme", Map.of("joueur", target.getName()));
        messages.send(target, "player-action-menu.faire-venir-cible", Map.of("admin", viewer.getName()));
    }

    private void handleGamemode(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.gamemode")) { deny(viewer); return; }
        GameMode[] cycle = GameMode.values();
        GameMode next = cycle[(target.getGameMode().ordinal() + 1) % cycle.length];
        target.setGameMode(next);
        messages.send(viewer, "player-action-menu.gamemode-confirme",
                Map.of("joueur", target.getName(), "mode", next.name()));
        open(viewer, target); // rafraîchit la lore avec le nouveau mode
    }

    private void handleHeal(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.heal")) { deny(viewer); return; }
        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        messages.send(viewer, "player-action-menu.heal-confirme", Map.of("joueur", target.getName()));
    }

    private void handleFreeze(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.freeze")) { deny(viewer); return; }
        boolean nowFrozen = freezeManager.toggle(target.getUniqueId());
        messages.send(viewer, nowFrozen ? "player-action-menu.freeze-confirme-actif" : "player-action-menu.freeze-confirme-inactif",
                Map.of("joueur", target.getName()));
        messages.send(target, nowFrozen ? "player-action-menu.freeze-subi-actif" : "player-action-menu.freeze-subi-inactif");
        open(viewer, target);
    }

    private void handleVanish(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.vanish")) { deny(viewer); return; }
        boolean nowVanished = vanishManager.toggle(plugin, target);
        messages.send(viewer, nowVanished ? "player-action-menu.vanish-confirme-actif" : "player-action-menu.vanish-confirme-inactif",
                Map.of("joueur", target.getName()));
        open(viewer, target);
    }

    private void handleInvsee(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.invsee")) { deny(viewer); return; }
        viewer.openInventory(target.getInventory());
    }

    private void handleClear(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.clear")) { deny(viewer); return; }
        target.getInventory().clear();
        messages.send(viewer, "player-action-menu.clear-confirme", Map.of("joueur", target.getName()));
        messages.send(target, "player-action-menu.clear-subi", Map.of("admin", viewer.getName()));
    }

    private void handleKick(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.kick")) { deny(viewer); return; }
        pendingKickManager.setPending(viewer.getUniqueId(), target.getUniqueId());
        viewer.closeInventory();
        messages.send(viewer, "player-action-menu.kick.demande-raison", Map.of("joueur", target.getName()));
    }

    private void handleMute(Player viewer, Player target) {
        if (!viewer.hasPermission("staffgui.players.mute")) { deny(viewer); return; }
        if (muteManager.isMuted(target.getUniqueId())) {
            muteManager.unmute(target.getUniqueId());
            messages.send(viewer, "player-action-menu.mute-confirme-inactif", Map.of("joueur", target.getName()));
        } else {
            muteManager.mute(target.getUniqueId(), "Mute via StaffGUI");
            messages.send(viewer, "player-action-menu.mute-confirme-actif", Map.of("joueur", target.getName()));
            messages.send(target, "player-action-menu.mute-subi");
        }
        open(viewer, target);
    }

    private void deny(Player viewer) {
        messages.send(viewer, "player-action-menu.permission-refusee");
    }
}