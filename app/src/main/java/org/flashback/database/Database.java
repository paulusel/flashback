package org.flashback.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

import org.flashback.types.User;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database implements Closeable {

    private static HikariDataSource ds;
    private final static String dbName = "flashback";
    private final static String dbUserName = "flashback";
    private final static String password = "";

    private Connection conn;

    private Database(Connection conn) {
        this.conn = conn;
    }

    public static Database getDatabase() throws SQLException{
        if(ds == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://localhost/" + dbName);
            config.setUsername(dbUserName);
            config.setPassword(password);

            config.setMaximumPoolSize(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            ds = new HikariDataSource(config);
        }

        return new Database(ds.getConnection());
    }

    public User getUser(String userId) throws SQLException {
        // TODO: Dummy code
        var stmnt = conn.prepareStatement("SELECT username, password, telegram_userid FROM users WHERE username = ?");
        stmnt.setString(1, userId);
        var result = stmnt.executeQuery();
        if(!result.next()) return null;
        User usr = new User();
        usr.setUserName(result.getString(1));
        usr.setPassword(result.getString(2));
        usr.setTelegramUserId(result.getString(3));
        return usr;
    }

    @Override
    public void close() {
        try {
            conn.close();
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }
}
