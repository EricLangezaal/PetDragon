package com.ericdebouwer.petdragon.database;

import com.ericdebouwer.petdragon.PetDragon;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

class SQLiteConnection extends Connection {

    public SQLiteConnection(PetDragon plugin, ConfigurationSection config) {
        super(plugin, config);
    }

    @Override
    public void setupConnection(HikariConfig hikariConfig) {
        File dbFile = new File(plugin.getDataFolder(), getDatabaseName());

        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.addDataSourceProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");
    }

    public String getUpsertQuery(){
        // miss excluded.WorldName om niet te herhalen
        return "INSERT INTO " + DRAGON_TABLE +
                " (DragonID, OwnerID, WorldName, X, Y, Z) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (DragonID) DO UPDATE SET " +
                "WorldName = ?,  X = ?, Y = ?, Z = ?, LastUpdate = CURRENT_TIMESTAMP";
    }

    /*
    @Override
    public void addOrUpdate(EnderDragon dragon, UUID owner) throws SQLException {
        final String UPDATE = "UPDATE `" + DRAGON_TABLE + "` SET" +
                "`WorldName` = ?,  `X` = ?, `Y` = ?, `Z` = ?, `LastUpdate` = CURRENT_TIMESTAMP" +
                " WHERE DragonID = ?";

        final String INSERT = "INSERT INTO `" + DRAGON_TABLE + "`" +
                "(DragonID, OwnerID, WorldName, X, Y, Z) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO UPDATE SET " +
                "`WorldName` = ?,  `X` = ?, `Y` = ?, `Z` = ?, `LastUpdate` = CURRENT_TIMESTAMP";

        try (java.sql.Connection connection = dataSource.getConnection(); PreparedStatement updateStmt = connection.prepareStatement(UPDATE)) {
            updateStmt.setString(0, dragon.getLocation().getWorld().toString());
            updateStmt.setInt(1, dragon.getLocation().getBlockX());
            updateStmt.setInt(2, dragon.getLocation().getBlockY());
            updateStmt.setInt(3, dragon.getLocation().getBlockZ());
            updateStmt.setString(4, dragon.getUniqueId().toString());
            updateStmt.executeUpdate();

            if (updateStmt.executeUpdate() > 0) return;

            PreparedStatement insertStmt = connection.prepareStatement(INSERT);
            insertStmt.setString(0, dragon.getUniqueId().toString());
            insertStmt.setString(1, owner.toString());
            insertStmt.setString(2, dragon.getLocation().getWorld().toString());
            insertStmt.setInt(3, dragon.getLocation().getBlockX());
            insertStmt.setInt(4, dragon.getLocation().getBlockY());
            insertStmt.setInt(5, dragon.getLocation().getBlockZ());
            insertStmt.executeUpdate();
            insertStmt.close();
        }
    }*/


}
