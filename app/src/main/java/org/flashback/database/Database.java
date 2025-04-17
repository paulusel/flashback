package org.flashback.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flashback.helpers.Config;
import org.flashback.types.User;
import org.mindrot.jbcrypt.BCrypt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database implements Closeable {

    private static HikariDataSource ds;

    private Connection conn;

    private Database(Connection conn) {
        this.conn = conn;
    }

    public static Database getDatabase() throws DatabaseException {
        if(ds == null) {
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

        try {
            return new Database(ds.getConnection());
        }
        catch(SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public User getUser(String userId) throws DatabaseException {
        // TODO: Dummy code
        try {

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

    @Override
    public void close() {
        try {
            conn.close();
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    public boolean authenticate(User user) throws DatabaseException {
        try {

            if(user.getPassword() == null || user.getUserName() == null) return false;

            String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

            PreparedStatement stmnt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            stmnt.setString(1, user.getUserName());
            ResultSet result = stmnt.executeQuery();

            if(!result.next()) return false;
            return passwordHash.equals(result.getString(1));

        }
        catch(SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void addNewUser(User user) throws DatabaseException {
        if(user.getUserName() == null || user.getPassword() == null) throw new DatabaseException("username or password missing");
        try {
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
