package org.flashback.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.Config;
import org.flashback.types.FlashBackUser;
import org.flashback.types.NoteFile;
import org.flashback.types.FlashBackNote;
import org.mindrot.jbcrypt.BCrypt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

    private static HikariDataSource ds;

    public static void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + Config.getValue("dbhost") + "/" + Config.getValue("dbname"));
        config.setUsername(Config.getValue("dbusername"));
        config.setPassword(Config.getValue("dbpassword"));

        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);
    }

    public static FlashBackUser getUserByUserId(Integer userId) throws FlashbackException {
        try(var conn = ds.getConnection()) {

            var stmnt = conn.prepareStatement(
                "SELECT username, telegram_user_id, telegram_chat_id FROM users WHERE user_id = ?");
            stmnt.setInt(1, userId);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user not found");
            }

            var usr = new FlashBackUser();
            usr.setUserId(userId);
            usr.setUserName(result.getString(1));
            usr.setTelegramUserId(result.getLong(2));
            usr.setTelegramChatId(result.getLong(3));
            return usr;

        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackUser authenticate(FlashBackUser user) throws FlashbackException {
        try (var conn = ds.getConnection()){

            if(user.getPassword() == null || user.getUserName() == null){
                throw new FlashbackException("username or password missing");
            }

            var stmnt = conn.prepareStatement(
                "SELECT password, user_id, telegram_user_id, telegram_chat_id FROM users WHERE username = ?");
            stmnt.setString(1, user.getUserName());
            var result = stmnt.executeQuery();

            if(!result.next() || !BCrypt.checkpw(user.getPassword(), result.getString(1))) {
                throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "incorrect username or password");
            }

            user.setUserId(result.getInt(2));
            user.setTelegramUserId(result.getLong(3));
            user.setTelegramChatId(result.getLong(4));

            return user;

        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void deleteUser(Integer userId) throws FlashbackException {
        try(var conn = ds.getConnection()){
            var stmnt = conn.prepareStatement("DELETE FROM users WHERE user_id = ?");
            stmnt.setInt(1, userId);
            stmnt.execute();
            if(stmnt.getUpdateCount() == 0) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user not found");
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackUser addNewUser(FlashBackUser user) throws FlashbackException {
        if(user.getUserName() == null || user.getPassword() == null) {
            throw new FlashbackException("missing password or username");
        }

        try (var conn = ds.getConnection()){
            String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
            var stmnt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?) RETURNING user_id");
            stmnt.setString(1, user.getUserName());
            stmnt.setString(2, passwordHash);
            var result = stmnt.executeQuery();
            result.next();
            user.setUserId(result.getInt(1));
            return user;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNoteAndAssignId(Integer userId, FlashBackNote note) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var noteInsertStmnt = conn.prepareStatement("INSERT INTO notes (owner_id, note) VALUES (?,?) RETURNING note_id");
            noteInsertStmnt.setInt(1, userId);
            noteInsertStmnt.setString(2, note.getNote());

            var result = noteInsertStmnt.executeQuery();
            result.next();
            note.setNoteId(result.getInt(1));

            if(note.getFiles() != null) {
                insertNoteFiles(conn, note.getNoteId(), note.getFiles());
            }

            if(note.getTags() != null){
                insertNoteTags(conn, note.getNoteId(), note.getTags());
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void insertNoteFiles(Connection conn, Integer noteId, List<NoteFile> files) throws SQLException {
        var noteFileInsertStmnt = conn.prepareStatement("INSERT INTO note_files (note_id, file_id) VALUES (?,?)");
        noteFileInsertStmnt.setInt(1, noteId);

        var fileInsertStmnt = conn.prepareStatement(
            "INSERT INTO files (file_id, extension, mime_type, size, telegram_file_id) VALUES (?,?,?,?,?)");
        for(var file : files) {
            fileInsertStmnt.setString(1, file.getFileId());
            fileInsertStmnt.setString(2, file.getExtension());
            fileInsertStmnt.setString(3, file.getMimeType());
            fileInsertStmnt.setLong(4, file.getSize());
            fileInsertStmnt.setString(5, file.getTelegramFileId());
            fileInsertStmnt.addBatch();

            noteFileInsertStmnt.setString(2, file.getFileId());
            noteFileInsertStmnt.addBatch();
        }
        fileInsertStmnt.executeBatch();
        noteFileInsertStmnt.executeBatch();
    }

    private static void insertNoteTags(Connection conn, Integer noteId, List<String> tags) throws SQLException {
        var tagInsertStmnt = conn.prepareStatement("INSERT INTO tags (note_id, tag) VALUES (?,?)");
        tagInsertStmnt.setInt(1, noteId);

        for(var tag : tags) {
            tagInsertStmnt.setString(2, tag);
            tagInsertStmnt.addBatch();
        }
        tagInsertStmnt.executeBatch();
    }

    public static FlashBackNote getNote(Integer userId, Integer noteId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement( "SELECT note, modified, created  FROM notes WHERE note_id = ? AND owner_id = ?");
            stmnt.setInt(1, noteId);
            stmnt.setInt(2, userId);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "note not found: [" + noteId + "]");
            }

            FlashBackNote note = new FlashBackNote();
            note.setNote(result.getString(1));
            note.setModified(result.getTimestamp(2));
            note.setModified(result.getTimestamp(3));
            loadNoteFiles(conn, note);
            loadNoteTags(conn, note);
            return note;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void loadNoteFiles(Connection conn, FlashBackNote note) throws SQLException {
        var stmnt = conn.prepareStatement("SELECT file_id, extension, mime_type, size, telegram_file_id " + 
            " FROM files JOIN note_files USING (file_id) WHERE note_files.note_id = ?");
        stmnt.setInt(1, note.getNoteId());
        var result = stmnt.executeQuery();
        while(result.next()) {
            NoteFile file = new NoteFile();
            file.setFileId(result.getString(1));
            file.setExtension(result.getString(2));
            file.setMimeType(result.getString(3));
            file.setSize(result.getLong(4));
            file.setTelegramFileId(result.getString(5));
            note.getFiles().addLast(file);
        }
    }

    private static void loadNoteTags(Connection conn, FlashBackNote note) throws SQLException {
        var stmnt = conn.prepareStatement("SELECT tag FROM tags JOIN note_tags USING (file_id) WHERE note_tags.note_id = ?");
        stmnt.setInt(1, note.getNoteId());
        var result = stmnt.executeQuery();
        while(result.next()) {
            note.getTags().add(result.getString(1));
        }
    }

    public static void deleteNote(Integer userId, Integer noteId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("DELETE FROM notes WHERE note_id = ? AND owner_id = ?");
            stmnt.setInt(1, noteId);
            if(stmnt.executeUpdate() == 0) {
                throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "not authorized to take action");
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static NoteFile getFile(Integer userId, String fileId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("SELECT telegram_file_id, original_name, size, mime_type " +
                " FROM file_owners JOIN files USING (file_id) WHERE username = ? AND file_id = ?");
            stmnt.setLong(1, userId);
            stmnt.setString(2, fileId);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
            }

            NoteFile file = new NoteFile();
            file.setFileId(fileId);
            file.setTelegramFileId(result.getString(1));
            file.setSize(result.getLong(3));
            file.setMimeType(result.getString(4));
            return file;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackUser getUserByChatId(Long chatId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement(
                "SELECT user_id, username, telegram_user_id FROM users WHERE telegram_chat_id = ?");
            stmnt.setLong(1, chatId);
            var result = stmnt.executeQuery();
            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user note found");
            }
            FlashBackUser user = new FlashBackUser();
            user.setTelegramChatId(chatId);
            user.setUserId(result.getInt(1));
            user.setUserName(result.getString(2));
            user.setTelegramUserId(result.getLong(3));
            return user;
        }
        catch(SQLException e){
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void updateNote(Integer userId, FlashBackNote note) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            getNote(userId, note.getNoteId()); // verifies the user owns the note

            if(note.getNote() != null) {
                var stmnt = conn.prepareStatement("UPDATE notes SET note = ? WHERE note_id = ?");
                stmnt.setString(1, note.getNote());
                stmnt.setInt(2, note.getNoteId());
                stmnt.executeUpdate();
            }
            insertNoteFiles(conn, note.getNoteId(), note.getFiles());
            insertNoteTags(conn, note.getNoteId(), note.getTags());
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void updateUser(Integer userId, FlashBackUser user) throws FlashbackException {

    }

    public static void addOrUpdateNote(Integer userId, FlashBackNote note) throws FlashbackException {
        if(note.getNoteId() == null) {
            addNoteAndAssignId(userId, note);
        }
        else {
            updateNote(userId, note);
        }
    }

    public static List<FlashBackNote> searchNotes(Integer userId, String keyword) throws FlashbackException {
        List<FlashBackNote> notes = new ArrayList<>();
        return notes;
    }

    public static List<FlashBackNote> getFeed(Integer userId, Long timestamp) throws FlashbackException {
        List<FlashBackNote> notes = new ArrayList<>();
        return notes;
    }
}
