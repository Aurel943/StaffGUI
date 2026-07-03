package fr.aurel943.staffgui.moderation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le vanish, en mémoire (comme le freeze — un redémarrage lève le
 * vanish, ce qui évite un admin invisible en permanence par oubli).
 *
 * Un joueur vanish reste visible des autres membres du staff (permission
 * staffgui.use), mais invisible de tous les joueurs normaux — utile pour
 * observer sans se faire repérer tout en restant coordonné avec le reste
 * de l'équipe.
 */
public class VanishManager {

    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /** Bascule l'état et applique immédiatement hidePlayer/showPlayer à tous les joueurs en ligne. */
    public boolean toggle(org.bukkit.plugin.Plugin plugin, Player target) {
        boolean nowVanished = !vanished.contains(target.getUniqueId());
        if (nowVanished) {
            vanished.add(target.getUniqueId());
        } else {
            vanished.remove(target.getUniqueId());
        }
        applyVisibility(plugin, target);
        return nowVanished;
    }

    /** Réapplique la visibilité correcte de "target" à tous les joueurs en ligne. */
    public void applyVisibility(org.bukkit.plugin.Plugin plugin, Player target) {
        boolean isVanished = vanished.contains(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            if (isVanished && !viewer.hasPermission("staffgui.use")) {
                viewer.hidePlayer(plugin, target);
            } else {
                viewer.showPlayer(plugin, target);
            }
        }
    }

    /** Cache tous les joueurs actuellement vanish aux yeux d'un joueur qui vient de se connecter (sauf s'il est staff). */
    public void hideAllFrom(org.bukkit.plugin.Plugin plugin, Player newcomer) {
        if (newcomer.hasPermission("staffgui.use")) return;
        for (UUID uuid : vanished) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                newcomer.hidePlayer(plugin, target);
            }
        }
    }
}