package fr.aurel943.staffgui.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;
import fr.aurel943.staffgui.moderation.MuteManager;
import fr.aurel943.staffgui.moderation.PendingBroadcastManager;
import fr.aurel943.staffgui.moderation.PendingEconomyAction;
import fr.aurel943.staffgui.moderation.PendingEconomyManager;
import fr.aurel943.staffgui.moderation.PendingKickManager;

import java.util.Map;
import java.util.UUID;

/**
 * Un seul listener de chat pour quatre besoins, vérifiés dans cet ordre :
 *   1) Kick en attente de raison (Lot 2)
 *   2) Action économie en attente de montant (Lot 3)
 *   3) Broadcast en attente de message (Lot 5)
 *   4) Sinon, si l'expéditeur est mute, on annule son message (Lot 2)
 */
public class ChatModerationListener implements Listener {

    private final StaffGUI plugin;
    private final MuteManager muteManager;
    private final PendingKickManager pendingKickManager;
    private final PendingEconomyManager pendingEconomyManager;
    private final PendingBroadcastManager pendingBroadcastManager;
    private final MessagesManager messages;

    public ChatModerationListener(StaffGUI plugin, MuteManager muteManager,
                                  PendingKickManager pendingKickManager,
                                  PendingEconomyManager pendingEconomyManager,
                                  PendingBroadcastManager pendingBroadcastManager) {
        this.plugin = plugin;
        this.muteManager = muteManager;
        this.pendingKickManager = pendingKickManager;
        this.pendingEconomyManager = pendingEconomyManager;
        this.pendingBroadcastManager = pendingBroadcastManager;
        this.messages = plugin.getMessagesManager();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingKickManager.hasPending(uuid)) {
            event.setCancelled(true);
            handleKickReason(player, pendingKickManager.consume(uuid), event.getMessage());
            return;
        }

        if (pendingEconomyManager.hasPending(uuid)) {
            event.setCancelled(true);
            handleEconomyAmount(player, pendingEconomyManager.consume(uuid), event.getMessage());
            return;
        }

        if (pendingBroadcastManager.hasPending(uuid)) {
            event.setCancelled(true);
            pendingBroadcastManager.consume(uuid);
            handleBroadcastMessage(player, event.getMessage());
            return;
        }

        if (muteManager.isMuted(uuid)) {
            event.setCancelled(true);
            messages.send(player, "chat.mute-actif");
        }
    }

    private void handleKickReason(Player admin, UUID targetUuid, String reason) {
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = org.bukkit.Bukkit.getPlayer(targetUuid);
            if (target == null) {
                messages.send(admin, "player-action-menu.kick.cible-deconnectee");
                return;
            }
            target.kick(net.kyori.adventure.text.Component.text(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c" + reason)));
            messages.send(admin, "player-action-menu.kick.confirme",
                    Map.of("joueur", target.getName(), "raison", reason));
        });
    }

    private void handleEconomyAmount(Player admin, PendingEconomyAction action, String raw) {
        int montant;
        try {
            montant = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            messages.send(admin, "economy-action-menu.montant-invalide");
            return;
        }
        if (montant < 0) {
            messages.send(admin, "economy-action-menu.montant-invalide");
            return;
        }

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.isHubAvailable()) return;
            var economy = plugin.getHubPlugin().getEconomyManager();
            Player target = org.bukkit.Bukkit.getPlayer(action.targetUuid);
            String nomCible = target != null ? target.getName() : action.targetUuid.toString();

            switch (action.type) {
                case GIVE -> {
                    economy.addBalance(action.targetUuid, montant);
                    messages.send(admin, "economy-action-menu.give-confirme",
                            Map.of("montant", String.valueOf(montant), "joueur", nomCible));
                }
                case TAKE -> {
                    boolean ok = economy.removeBalance(action.targetUuid, montant);
                    if (ok) {
                        messages.send(admin, "economy-action-menu.take-confirme",
                                Map.of("montant", String.valueOf(montant), "joueur", nomCible));
                    } else {
                        messages.send(admin, "economy-action-menu.take-solde-insuffisant", Map.of("joueur", nomCible));
                    }
                }
                case SET -> {
                    economy.setBalance(action.targetUuid, montant);
                    messages.send(admin, "economy-action-menu.set-confirme",
                            Map.of("montant", String.valueOf(montant), "joueur", nomCible));
                }
            }
        });
    }

    private void handleBroadcastMessage(Player admin, String message) {
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String formatted = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    messages.get("tools-menu.broadcast-format").replace("{message}", message));
            org.bukkit.Bukkit.broadcastMessage(formatted);
        });
    }
}