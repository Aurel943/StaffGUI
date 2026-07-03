package fr.aurel943.staffgui.messages;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import fr.aurel943.staffgui.StaffGUI;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Gère les textes affichés aux joueurs, centralisés dans config/messages.yml.
 * Même logique que MessagesManager de Hub : rien en dur dans le Java,
 * remplacement de {placeholders} par leurs valeurs.
 */
public class MessagesManager {

    private final StaffGUI plugin;
    private final File file;
    private YamlConfiguration config;

    public MessagesManager(StaffGUI plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config/messages.yml");
        load();
    }

    private void load() {
        config = YamlConfiguration.loadConfiguration(file);
        InputStream defaultStream = plugin.getResource("config/messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
    }

    public void reload() {
        load();
    }

    public String get(String key) {
        return ChatColor.translateAlternateColorCodes('&', config.getString(key, key));
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }
}