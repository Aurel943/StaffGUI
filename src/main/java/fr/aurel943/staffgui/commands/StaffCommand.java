package fr.aurel943.staffgui.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import fr.aurel943.staffgui.StaffGUI;
import fr.aurel943.staffgui.messages.MessagesManager;

public class StaffCommand implements CommandExecutor {

    private final StaffGUI plugin;
    private final MessagesManager messages;

    public StaffCommand(StaffGUI plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "commande.joueur-requis");
            return true;
        }
        if (!player.hasPermission("staffgui.use")) {
            messages.send(player, "commande.permission-refusee");
            return true;
        }
        plugin.getMainMenu().open(player);
        return true;
    }
}