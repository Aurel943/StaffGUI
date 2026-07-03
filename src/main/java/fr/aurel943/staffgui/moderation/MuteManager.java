package fr.aurel943.staffgui.moderation;

import fr.aurel943.staffgui.database.StaffGUIDatabase;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les mutes, persistants en base (staffgui_mutes). Un cache mémoire
 * évite de requêter la BDD à chaque message envoyé par un joueur — seul le
 * mute/unmute écrit en base, la lecture passe toujours par le cache.
 */
public class MuteManager {

    private final StaffGUIDatabase database;
    private final Set<UUID> muted = ConcurrentHashMap.newKeySet();

    public MuteManager(StaffGUIDatabase database) {
        this.database = database;
        muted.addAll(database.getAllMutedUuids());
    }

    public boolean isMuted(UUID uuid) {
        return muted.contains(uuid);
    }

    public void mute(UUID uuid, String reason) {
        muted.add(uuid);
        database.setMute(uuid, reason);
    }

    public void unmute(UUID uuid) {
        muted.remove(uuid);
        database.removeMute(uuid);
    }
}