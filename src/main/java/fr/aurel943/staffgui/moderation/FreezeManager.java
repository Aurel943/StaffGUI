package fr.aurel943.staffgui.moderation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le freeze, volontairement EN MÉMOIRE uniquement (pas de table BDD) :
 * si le serveur redémarre, tous les freezes sont levés automatiquement —
 * comportement voulu, pour ne jamais laisser un joueur bloqué à vie après
 * un redémarrage imprévu.
 */
public class FreezeManager {

    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    /** Bascule l'état et retourne le nouvel état (true = maintenant freeze). */
    public boolean toggle(UUID uuid) {
        if (frozen.contains(uuid)) {
            frozen.remove(uuid);
            return false;
        } else {
            frozen.add(uuid);
            return true;
        }
    }
}