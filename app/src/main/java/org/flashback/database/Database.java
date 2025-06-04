package org.flashback.database;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.Config;
import org.flashback.types.FlashBackUser;
import org.flashback.types.FileAddRemoveRequest;
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

    public static FlashBackUser getUser(Integer userId) throws FlashbackException {
        try(var conn = ds.getConnection()) {

            var stmnt = conn.prepareStatement("SELECT username, telegram_user_id FROM users WHERE username = ?");
            stmnt.setInt(1, userId);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user not found");
            }

            var usr = new FlashBackUser();
            usr.setUserName(result.getString(1));
            usr.setTelegramUserId(result.getLong(2));
            return usr;

        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackUser authenticate(FlashBackUser user) throws FlashbackException {
        try (var conn = ds.getConnection()){

            if(user.getPassword() == null || user.getUserName() == null){
                throw new FlashbackException("username or password missing");
            }

            var stmnt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            stmnt.setString(1, user.getUserName());
            var result = stmnt.executeQuery();
            if(!result.next() || !BCrypt.checkpw(user.getPassword(), result.getString(1))) {
                throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "incorrect username or password");
            }
            // TODO: add other data to user if necessary
            return user;

        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
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
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNewUser(FlashBackUser user) throws FlashbackException {
        if(user.getUserName() == null || user.getPassword() == null) {
            throw new FlashbackException("missing password or username");
        }

        try (var conn = ds.getConnection()){
            String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
            var stmnt = conn.prepareStatement("INSERT INTO users (username, password, telegram_user_id) VALUES (?, ?, ?)");
            stmnt.setString(1, user.getUserName());
            stmnt.setString(2, passwordHash);
            stmnt.setLong(3, user.getTelegramUserId());
            stmnt.execute();
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNoteAndAssignId(Integer userId, FlashBackNote note) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var memoInsertStmnt = conn.prepareStatement(
                "INSERT INTO memo_items (type, parent, name, note, username) VALUES (?,?,?,?,?) RETURNING item_id");
            memoInsertStmnt.setInt(5, userId);
            var fileInsertStmnt = conn.prepareStatement("INSERT INTO memo_files (item_id, file_id) VALUES (?,?)");
            var tagInsertStmnt = conn.prepareStatement("INSERT INTO tags (item_id, tag) VALUES (?,?)");

            memoInsertStmnt.setString(4, note.getNote());

            var result = memoInsertStmnt.executeQuery();
            result.next();
            Integer noteId = result.getInt(1);

            if(note.getFiles() != null) {
                fileInsertStmnt.setInt(1, noteId);
                for(var file : note.getFiles()) {
                    fileInsertStmnt.setString(2, file.getFileId());
                    fileInsertStmnt.addBatch();
                }
                fileInsertStmnt.executeBatch();
                // TODO: fileIds need to copy back to the note
            }

            if(note.getTags() != null){
                tagInsertStmnt.setInt(1, noteId);
                for(var tag : note.getTags()) {
                    tagInsertStmnt.setString(2, tag);
                    tagInsertStmnt.addBatch();
                }
                tagInsertStmnt.executeBatch();
            }

            note.setNoteId(noteId);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackNote getNote(Integer userId, Integer noteId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement(
                "SELECT username, type, name, parent, note, modified  FROM memo_items WHERE item_id = ?");
                            stmnt.setInt(1, noteId);
                var result = stmnt.executeQuery();
                if(!result.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "note not found: [" + noteId + "]");
                }

                if(!userId.equals(result.getInt(1))) {
                    throw new FlashbackException(HttpStatus.UNAUTHORIZED_401,
                        "unauthorized to access note: [" + noteId + "]");
                }

                FlashBackNote note = new FlashBackNote();
                note.setNote(result.getString(5));
                note.setModified(result.getTimestamp(6));
            return note;
        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void deleteNote(Integer userId, Integer noteId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("DELETE FROM memo_items WHERE item_id = ? RETURNING username");
            stmnt.setInt(1, noteId);
            var result = stmnt.executeQuery();
            if(result.next() && result.getInt(1) != userId) {
                throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "not authorized to take action");
            }
        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNoteFiles(Integer userId, FileAddRemoveRequest[] requests) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("INSERT INTO memo_files (item_id, file_id) VALUES (? , ?)");
            var owenerCheckStmnt = conn.prepareStatement("SELECT username FROM file_owners WHERE file_id = ? AND username = ?");
            for(var request : requests) {
                if(request.getNoteId() == null || request.getFileId() == null) {
                    throw new FlashbackException("invalid request: itemId or fileId missing");
                }

                owenerCheckStmnt.setString(1, request.getFileId());
                owenerCheckStmnt.setLong(2, userId);
                var owner = owenerCheckStmnt.executeQuery();

                if(!owner.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
                }

                stmnt.setInt(1, request.getNoteId());
                stmnt.setString(2, request.getFileId());
                stmnt.addBatch();
            }
            stmnt.executeBatch();
        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void removeNoteFiles(Integer userId, FileAddRemoveRequest[] requests) throws FlashbackException {
        try (var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("DELETE FROM memo_files WHERE item_id = ? AND file_id = ?");
            for(var request : requests) {
                stmnt.setInt(1, request.getNoteId());
                stmnt.setString(2, request.getFileId());
                stmnt.addBatch();
            }
            stmnt.executeBatch();
        }
        catch(Exception e) {
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
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addFiles(Integer userId, List<NoteFile> files) throws FlashbackException {
        try (var conn = ds.getConnection()) {
            //inset file
            var ownerInsertStmnt = conn.prepareStatement(
                "INSERT INTO file_owners (username, file_id) VALUES (?, ?) ON CONFLICT (username, file_id) DO NOTHING"
            );
            ownerInsertStmnt.setInt(1, userId);

            var fileInsertStmnt = conn.prepareStatement(
                "INSERT INTO files (file_id, telegram_file_id, original_name, mime_type, size)"
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (file_id) DO NOTHING"
            );
            for(var file : files) {
                fileInsertStmnt.setString(1, file.getFileId());
                fileInsertStmnt.setString(2, file.getTelegramFileId());
                fileInsertStmnt.setString(4, file.getMimeType());
                fileInsertStmnt.setLong(5, file.getSize());

                ownerInsertStmnt.setString(2, file.getFileId());

                fileInsertStmnt.addBatch();
                ownerInsertStmnt.addBatch();
            }

            fileInsertStmnt.executeBatch();
            ownerInsertStmnt.executeBatch();
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackUser getUserByChatId(Long chatId) throws FlashbackException {
        return new FlashBackUser();
    }

    public static void updateNote(Integer userId, FlashBackNote note) throws FlashbackException {

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
