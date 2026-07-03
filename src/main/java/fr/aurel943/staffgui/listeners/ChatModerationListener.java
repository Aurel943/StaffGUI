package fr.aurel943.staffgui.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;
import fr.aurel943.staffgui.moderation.MuteManager;
import fr.aurel943.staffgui.moderation.PendingKickManager;

import java.util.UUID;

/**
 * Un seul listener de chat pour deux besoins distincts, dans cet ordre :
 *   1) Si l'expéditeur a un kick "en attente de raison" (a cliqué sur Kick
 *      dans PlayerActionMenu), son prochain message de chat EST la raison
 *      — on l'intercepte, on exécute le kick, et on annule le message pour
 *      qu'il ne parte pas dans le chat public.
 *   2) Sinon, si l'expéditeur est mute, on annule simplement son message.
 *
 * Utilise AsyncPlayerChatEvent (API legacy mais texte brut direct, plus
 * simple ici qu'AsyncChatEvent + Component pour juste lire une raison).
 */
public class ChatModerationListener implements Listener {

    private final StaffGUI plugin;
    private final MuteManager muteManager;
    private final PendingKickManager pendingKickManager;
    private final MessagesManager messages;

    public ChatModerationListener(StaffGUI plugin, MuteManager muteManager, PendingKickManager pendingKickManager) {
        this.plugin = plugin;
        this.muteManager = muteManager;
        this.pendingKickManager = pendingKickManager;
        this.messages = plugin.getMessagesManager();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingKickManager.hasPending(uuid)) {
            event.setCancelled(true);
            UUID targetUuid = pendingKickManager.consume(uuid);
            String reason = event.getMessage();

            // Exécution du kick sur le thread principal : AsyncPlayerChatEvent
            // est asynchrone, mais Player#kick() doit être appelé sur le thread
            // principal du serveur.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = org.bukkit.Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    messages.send(player, "player-action-menu.kick.cible-deconnectee");
                    return;
                }
                target.kick(net.kyori.adventure.text.Component.text(
                        MessagesManager_colored("&c" + reason)));
                messages.send(player, "player-action-menu.kick.confirme",
                        java.util.Map.of("joueur", target.getName(), "raison", reason));
            });
            return;
        }

        if (muteManager.isMuted(uuid)) {
            event.setCancelled(true);
            messages.send(player, "chat.mute-actif");
        }
    }

    private static String MessagesManager_colored(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}