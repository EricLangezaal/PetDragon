package com.ericdebouwer.petdragon.database;

import com.ericdebouwer.petdragon.PetDragon;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;

class MySqlConnection extends Connection {

    public MySqlConnection(PetDragon plugin, ConfigurationSection config) {
        super(plugin, config);
    }

    @Override
    public void setupConnection(HikariConfig hikariConfig) {
        String jdbcUrl = "jdbc:mysql://" + fileConfig.getString("host") + ':' +
                fileConfig.getInt("port") + '/' + getDatabaseName();
        hikariConfig.setJdbcUrl(jdbcUrl);

        boolean useSSL = fileConfig.getBoolean("useSSL");
        hikariConfig.addDataSourceProperty("useSSL", useSSL);
        hikariConfig.addDataSourceProperty("requireSSL", useSSL);
        hikariConfig.addDataSourceProperty("sslMode", "PREFERRED");
        hikariConfig.addDataSourceProperty("verifyServerCertificate", false);
    }

    @Override
    protected String getUpsertQuery() {
        return  "INSERT INTO `" + DRAGON_TABLE + "`" +
                "(DragonID, OwnerID, WorldName, X, Y, Z) VALUES (?, ?, ?, ?, ?, ?)" +
                "ON DUPLICATE KEY UPDATE `WorldName` = ?, `X` = ?, `Y` = ?, `Z` = ?, `LastUpdate` = CURRENT_TIMESTAMP";
    }



}
