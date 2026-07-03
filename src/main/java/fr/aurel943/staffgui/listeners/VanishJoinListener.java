package fr.aurel943.staffgui.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.moderation.VanishManager;

/**
 * Sans ça, un joueur qui se connecte APRÈS qu'un admin se soit vanish le
 * verrait quand même — hidePlayer() ne s'applique qu'aux joueurs déjà en
 * ligne au moment de l'appel, pas aux futurs arrivants.
 */
public class VanishJoinListener implements Listener {

    private final StaffGUI plugin;
    private final VanishManager vanishManager;

    public VanishJoinListener(StaffGUI plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanishManager.hideAllFrom(plugin, event.getPlayer());
    }
}