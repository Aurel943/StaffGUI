package fr.aurel943.staffgui.moderation;

import java.util.UUID;

/** Action économie en attente de saisie d'un montant au clavier. */
public class PendingEconomyAction {

    public enum Type { GIVE, TAKE, SET }

    public final UUID targetUuid;
    public final Type type;

    public PendingEconomyAction(UUID targetUuid, Type type) {
        this.targetUuid = targetUuid;
        this.type = type;
    }
}