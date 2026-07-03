package fr.aurel943.staffgui.moderation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Même principe que PendingKickManager/PendingEconomyManager, mais sans donnée associée : juste "en attente ou pas". */
public class PendingBroadcastManager {

    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public void setPending(UUID adminUuid) {
        pending.add(adminUuid);
    }

    public boolean hasPending(UUID adminUuid) {
        return pending.contains(adminUuid);
    }

    public void consume(UUID adminUuid) {
        pending.remove(adminUuid);
    }
}