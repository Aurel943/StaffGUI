package fr.aurel943.staffgui.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import fr.aurel943.staffgui.moderation.FreezeManager;

/**
 * Bloque tout déplacement horizontal/vertical d'un joueur freeze — en ne
 * comparant QUE les coordonnées (pas le regard), pour ne pas empêcher un
 * joueur freeze de tourner la tête ou de parler, juste de bouger.
 */
public class MovementFreezeListener implements Listener {

    private final FreezeManager freezeManager;

    public MovementFreezeListener(FreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!freezeManager.isFrozen(player.getUniqueId())) return;
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return; // seule la tête a bougé, on laisse passer
        }
        event.setTo(event.getFrom());
    }
}