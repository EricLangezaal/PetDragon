package com.ericdebouwer.petdragon.database;

import com.ericdebouwer.petdragon.PetDragon;
import com.mysql.jdbc.JDBC4PreparedStatement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

abstract class Connection {

    protected PetDragon plugin;
    protected ConfigurationSection fileConfig;

    protected final String DRAGON_TABLE = "petdragon";
    protected HikariDataSource dataSource;

    public Connection(PetDragon plugin, ConfigurationSection config){
        this.plugin = plugin;
        this.fileConfig = config;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(getDriverName());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setUsername(fileConfig.getString("username", ""));
        hikariConfig.setPassword(fileConfig.getString("password", ""));

        this.setupConnection(hikariConfig);
        dataSource = new HikariDataSource(hikariConfig);
        setupTables();
    }

    public void close(){
        dataSource.close();
    }

    public String getDatabaseName(){
        return fileConfig.getString("database-name");
    }

    private String getDriverName(){
        return fileConfig.getString("driver");
    }

    public abstract void setupConnection(HikariConfig hikariConfig);

    private void setupTables(){
        final String CREATE = "CREATE TABLE IF NOT EXISTS `" + DRAGON_TABLE + "` (" +
                "`DragonID` CHAR(36) PRIMARY KEY," +
                "`OwnerID` CHAR(36) NOT NULL," +
                "`WorldName` VARCHAR(200) NOT NULL," +
                "`X` INT NOT NULL," +
                "`Y` INT NOT NULL," +
                "`Z` INT NOT NULL," +
                "`LastUpdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`remove` BOOLEAN NOT NULL DEFAULT FALSE" +
                ")";

        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CREATE)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to setup tables. Make sure your database setup is correct!");
            ex.printStackTrace();
        }
    }

    public void addOrUpdate(EnderDragon dragon){
        UUID owner = plugin.getFactory().getOwner(dragon);
        if (owner == null) return;

        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getUpsertQuery())){
            stmt.setString(1, dragon.getUniqueId().toString());
            stmt.setString(2, owner.toString());
            stmt.setString(3, dragon.getLocation().getWorld().getName());
            stmt.setInt(4, dragon.getLocation().getBlockX());
            stmt.setInt(5, dragon.getLocation().getBlockY());
            stmt.setInt(6, dragon.getLocation().getBlockZ());
            stmt.setString(7, dragon.getLocation().getWorld().getName());
            stmt.setInt(8, dragon.getLocation().getBlockX());
            stmt.setInt(9, dragon.getLocation().getBlockY());
            stmt.setInt(10, dragon.getLocation().getBlockZ());

            stmt.executeUpdate();

        } catch (SQLException ex){
            plugin.getLogger().log(Level.WARNING, "Failed to update " + owner.toString() + "'s (UUID) dragon at " + dragon.getLocation().toString());
            ex.printStackTrace();
        }
    }

    protected abstract String getUpsertQuery();

    public Set<DatabaseDragon> getDragons(UUID player){
        final String FETCH = "SELECT * FROM `" + DRAGON_TABLE + "` WHERE OwnerID = ? AND remove = FALSE";

        Set<DatabaseDragon> dragons = new HashSet<>();

        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FETCH)) {
            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                dragons.add(new DatabaseDragon(rs.getString("DragonID"), rs.getString("OwnerID"),
                        rs.getString("WorldName"),
                        rs.getInt("X"), rs.getInt("Y"), rs.getInt("Z")));
            }

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load dragons. Make sure your database setup is correct!");
            ex.printStackTrace();
        }
        return dragons;
    }

    public int getDragonCount(UUID player){
        final String COUNT = "SELECT COUNT(*) FROM `" + DRAGON_TABLE + "` WHERE OwnerID = ? AND remove = FALSE";
        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT)) {
            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load dragon count. Make sure your database setup is correct!");
            ex.printStackTrace();
        }
        return 0;
    }

    public boolean didJustRemove(UUID dragonID, boolean checkRemoval){
        String REMOVE = "DELETE FROM `" + DRAGON_TABLE + "` WHERE DragonID = ?";
        if (checkRemoval) REMOVE += " AND remove = TRUE";

        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(REMOVE)) {
            stmt.setString(1, dragonID.toString());
            return stmt.executeUpdate() > 0;

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove a dragon. Make sure your database setup is correct!");
            ex.printStackTrace();
        }
        return false;
    }

    public boolean markForRemoval(UUID dragonID){
        final String MARK = "UPDATE `" + DRAGON_TABLE + "` SET remove = TRUE WHERE DragonID = ? AND remove = FALSE";

        try (java.sql.Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(MARK)) {
            stmt.setString(1, dragonID.toString());
            return stmt.executeUpdate() > 0;

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to mark a dragon for removal. Make sure your database setup is correct!");
            ex.printStackTrace();
        }
        return false;
    }


}
