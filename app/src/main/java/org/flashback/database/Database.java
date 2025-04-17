package org.flashback.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flashback.helpers.Config;
import org.flashback.types.User;
import org.mindrot.jbcrypt.BCrypt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

    private static HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.getDatabaseUrl());
        config.setUsername(Config.getDatabaseUserName());
        config.setPassword(Config.getDbpassword());

        config.setMaximumPoolSize(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);
    }

    public static User getUser(String userId) throws DatabaseException {
        // TODO: Dummy code
        try(Connection conn = ds.getConnection()) {

            var stmnt = conn.prepareStatement("SELECT username, telegram_userid FROM users WHERE username = ?");
            stmnt.setString(1, userId);
            var result = stmnt.executeQuery();
            if(!result.next()) return null;
            User usr = new User();
            usr.setUserName(result.getString(1));
            usr.setPassword(result.getString(2));
            usr.setTelegramUserId(result.getString(3));
            return usr;

        }
        catch(SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public static boolean authenticate(User user) throws DatabaseException {
        try (Connection conn = ds.getConnection()){

            if(user.getPassword() == null || user.getUserName() == null) return false;

            PreparedStatement stmnt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            stmnt.setString(1, user.getUserName());
            ResultSet result = stmnt.executeQuery();
            result.next();

            return BCrypt.checkpw(user.getPassword(), result.getString(1));

        }
        catch(SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public static void addNewUser(User user) throws DatabaseException {
        if(user.getUserName() == null || user.getPassword() == null) throw new DatabaseException("username or password missing");
        try (Connection conn = ds.getConnection()){
            String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            PreparedStatement stmnt = conn.prepareStatement("INSERT INTO users (username, password, telegram_userid) VALUES (?, ?, ?)");
            stmnt.setString(1, user.getUserName());
            stmnt.setString(2, passwordHash);
            stmnt.setString(3, user.getTelegramUserId());
            stmnt.execute();
        }
        catch(SQLException e) {
            throw new DatabaseException(e);
        }
    }
}
