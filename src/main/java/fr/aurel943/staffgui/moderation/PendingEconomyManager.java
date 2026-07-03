package fr.aurel943.staffgui.moderation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Même principe que PendingKickManager : admin_uuid -> action en attente,
 * le temps que l'admin tape un montant dans le chat après avoir cliqué sur
 * Give/Take/Set dans EconomyActionMenu.
 */
public class PendingEconomyManager {

    private final Map<UUID, PendingEconomyAction> pending = new ConcurrentHashMap<>();

    public void setPending(UUID adminUuid, PendingEconomyAction action) {
        pending.put(adminUuid, action);
    }

    public boolean hasPending(UUID adminUuid) {
        return pending.containsKey(adminUuid);
    }

    public PendingEconomyAction consume(UUID adminUuid) {
        return pending.remove(adminUuid);
    }
}