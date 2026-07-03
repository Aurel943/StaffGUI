package fr.aurel943.staffgui.moderation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Petit registre en mémoire : admin_uuid -> cible_uuid, le temps que l'admin
 * tape la raison du kick dans le chat après avoir cliqué sur "Kick" dans
 * PlayerActionMenu. Consommé (retiré) dès que la raison est saisie.
 */
public class PendingKickManager {

    private final Map<UUID, UUID> pending = new ConcurrentHashMap<>();

    public void setPending(UUID adminUuid, UUID targetUuid) {
        pending.put(adminUuid, targetUuid);
    }

    public boolean hasPending(UUID adminUuid) {
        return pending.containsKey(adminUuid);
    }

    /** Retourne la cible et retire l'entrée. */
    public UUID consume(UUID adminUuid) {
        return pending.remove(adminUuid);
    }

    public void cancel(UUID adminUuid) {
        pending.remove(adminUuid);
    }
}