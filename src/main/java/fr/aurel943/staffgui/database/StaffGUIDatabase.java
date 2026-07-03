package fr.aurel943.staffgui.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Couche d'accès MySQL pour StaffGUI. Se connecte à la même base plugin2_db
 * que Hub et CowBrawl (Option B du projet : chaque plugin a sa propre classe
 * Database indépendante, pas de lib partagée). Table préfixée staffgui_ pour
 * éviter tout conflit avec les tables des autres plugins.
 *
 * Pour l'instant, une seule table : staffgui_mutes (mute persistant, survit
 * à un redémarrage — contrairement au freeze et au vanish qui restent en
 * mémoire, volontairement, voir FreezeManager/VanishManager).
 */
public class StaffGUIDatabase {

    private final Logger logger;
    private final File configFile;
    private HikariDataSource dataSource;

    public StaffGUIDatabase(File pluginFolder, Logger logger) {
        this.logger = logger;
        this.configFile = new File(pluginFolder, "config/database.yml");
    }

    public void connect() {
        // Le fichier peut ne pas exister au tout premier démarrage — on le
        // récupère depuis les ressources du jar dans ce cas.
    }

    public void connect(org.bukkit.plugin.java.JavaPlugin plugin) {
        if (!configFile.exists()) {
            plugin.saveResource("config/database.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = plugin.getResource("config/database.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }

        String host = config.getString("mysql.host", "127.0.0.1");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "plugin2_db");
        String utilisateur = config.getString("mysql.utilisateur", "root");
        String motDePasse = config.getString("mysql.mot-de-passe", "");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(utilisateur);
        hikariConfig.setPassword(motDePasse);
        hikariConfig.setMaximumPoolSize(config.getInt("mysql.pool.taille-max", 5));
        hikariConfig.setMinimumIdle(config.getInt("mysql.pool.taille-min-idle", 1));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.pool.timeout-connexion-ms", 10000));
        hikariConfig.setIdleTimeout(config.getLong("mysql.pool.timeout-idle-ms", 600000));
        hikariConfig.setMaxLifetime(config.getLong("mysql.pool.duree-vie-max-ms", 1800000));
        hikariConfig.setPoolName("StaffGUI-MySQL-Pool");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            logger.info("Connexion à la base MySQL réussie (" + host + ":" + port + "/" + database + ")");
        } catch (Exception e) {
            logger.severe("Impossible de se connecter à la base MySQL : " + e.getMessage());
        }
    }

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS staffgui_mutes (
                uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                reason VARCHAR(255) NOT NULL DEFAULT '',
                muted_at BIGINT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.severe("Erreur création table staffgui_mutes : " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ---------------------------------------------------------------
    // Mutes
    // ---------------------------------------------------------------

    public void setMute(UUID uuid, String reason) {
        String sql = """
            INSERT INTO staffgui_mutes (uuid, reason, muted_at) VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE reason = VALUES(reason), muted_at = VALUES(muted_at);
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, reason);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Erreur lors du mute en base : " + e.getMessage());
        }
    }

    public void removeMute(UUID uuid) {
        String sql = "DELETE FROM staffgui_mutes WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Erreur lors du unmute en base : " + e.getMessage());
        }
    }

    /** Retourne tous les UUID actuellement mute — utilisé pour précharger le cache mémoire au démarrage. */
    public java.util.Set<UUID> getAllMutedUuids() {
        java.util.Set<UUID> result = new java.util.HashSet<>();
        String sql = "SELECT uuid FROM staffgui_mutes";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la lecture des mutes : " + e.getMessage());
        }
        return result;
    }
}