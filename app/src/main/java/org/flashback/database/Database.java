package org.flashback.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;
import org.flashback.exceptions.NoteNotFound;
import org.flashback.exceptions.UserNotFoundException;
import org.flashback.helpers.Config;
import org.flashback.types.FlashBackUser;
import org.flashback.types.FlashBackFile;
import org.flashback.types.FlashBackNote;
import org.mindrot.jbcrypt.BCrypt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.lang3.tuple.Pair;

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
            user.setPassword(null);

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
            user.setPassword(null);
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

            if(note.getTags() != null){
                insertNoteTags(conn, note.getNoteId(), note.getTags());
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void insertNoteTags(Connection conn, Integer noteId, List<String> tags) throws SQLException {
        var tagInsertStmnt = conn.prepareStatement(
            "INSERT INTO note_tags (note_id, tag) VALUES (?,?) ON CONFlICT DO NOTHING");
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
            note.setNoteId(noteId);

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
        var stmnt = conn.prepareStatement("SELECT files.file_hash, type, extension, size, telegram_file_id " + 
            " FROM files JOIN note_files USING (file_hash) WHERE note_files.note_id = ?");
        stmnt.setInt(1, note.getNoteId());
        var result = stmnt.executeQuery();
        while(result.next()) {
            FlashBackFile file = new FlashBackFile();
            file.setHash(result.getString(1));
            file.setFileType(FlashBackFile.Type.typeOf(result.getInt(2)));
            file.setExtension(result.getString(3));
            file.setSize(result.getLong(4));
            file.setTelegramFileId(result.getString(5));
            note.getFiles().addLast(file);
        }
    }

    private static void loadNoteTags(Connection conn, FlashBackNote note) throws SQLException {
        var stmnt = conn.prepareStatement("SELECT tag FROM notes JOIN note_tags USING (note_id) WHERE note_tags.note_id = ?");
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
            stmnt.setInt(2, userId);

            if(stmnt.executeUpdate() == 0) {
                throw new NoteNotFound();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static FlashBackFile getFile(Integer userId, String fileHash) throws FlashbackException {
        String sql = "SELECT f.type, f.extension, f.size, f.telegram_file_id " +
            "FROM flashback.files f " +
            "WHERE f.file_hash = ? " +
            "AND EXISTS ( " +
            "    SELECT 1 " +
            "    FROM flashback.note_files nf " +
            "    JOIN flashback.notes n ON nf.note_id = n.note_id " +
            "    WHERE nf.file_hash = f.file_hash " +
            "    AND n.owner_id = ? " +
            ")";

        try(var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {

            stmnt.setString(1, fileHash);
            stmnt.setLong(2, userId);

            try(var result = stmnt.executeQuery()) {
                if(!result.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
                }

                FlashBackFile file = new FlashBackFile();
                file.setHash(fileHash);
                file.setFileType(FlashBackFile.Type.typeOf(result.getInt(1)));
                file.setExtension(result.getString(2));
                file.setSize(result.getLong(3));
                file.setTelegramFileId(result.getString(4));

                return file;
            }

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
                throw new UserNotFoundException();
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
            insertNoteTags(conn, note.getNoteId(), note.getTags());
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void updateUser(Integer userId, FlashBackUser user) throws FlashbackException {
        try (var conn = ds.getConnection()){
            List<String> updates = new ArrayList<>();
            List<Pair<Integer, Object>> values = new ArrayList<>();

            if(user.getPassword() != null) {
                updates.add("password = ?");
                String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
                values.add(Pair.of(1, passwordHash));
            }

            if(user.getTelegramChatId() != null) {
                updates.add("telegram_chat_id = ?");
                values.add(Pair.of(2, user.getTelegramChatId()));
            }

            if(user.getTelegramUserId() != null) {
                updates.add("telegram_user_id = ?");
                values.add(Pair.of(2, user.getTelegramUserId()));
            }

            if(updates.isEmpty()) {
                throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "empty update");
            }

            String sql = "UPDATE users SET " + String.join(", ", updates) + " WHERE user_id = ?";
            var stmnt = conn.prepareStatement(sql);

            int i = 1;
            for(var val : values) {
                if(val.getLeft() == 0) {
                    stmnt.setString(i, (String)val.getRight());
                }
                else {
                    stmnt.setLong(i, (Long)val.getRight());
                }
                ++i;
            }
            stmnt.setInt(i, userId);
            stmnt.executeUpdate();
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
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
        String sql = "SELECT DISTINCT n.note_id, n.note, n.modified, n.created " +
                    "FROM flashback.notes n " +
                    "LEFT JOIN flashback.note_tags nt ON n.note_id = nt.note_id " +
                    "WHERE n.owner_id = ? AND (n.note_tsvector @@ to_tsquery('english', ?) OR nt.tag ILIKE ?)";

        try (var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {

            stmnt.setInt(1, userId);
            stmnt.setString(2, keyword.trim().replace(" ", " & "));
            stmnt.setString(3, "%" + keyword + "%");

            List<FlashBackNote> notes = new ArrayList<>();
            try (var result = stmnt.executeQuery()) {
                while (result.next()) {
                    FlashBackNote note = new FlashBackNote();
                    note.setNoteId(result.getInt(1));
                    note.setNote(result.getString(2));
                    note.setModified(result.getTimestamp(3));
                    note.setCreated(result.getTimestamp(4));

                    loadNoteTags(conn, note);
                    loadNoteFiles(conn, note);

                    notes.add(note);
                }
            }

            return notes;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static List<FlashBackNote> getFeed(Integer userId, Long timestamp) throws FlashbackException {
        List<FlashBackNote> notes = new ArrayList<>();
        String sql = "SELECT note_id, note, modified, created FROM notes " +
            "WHERE owner_id = ? AND modified < ? " +
            "ORDER BY modified ASC LIMIT 10";;

        try (var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)){

            stmnt.setInt(1, userId);
            stmnt.setTimestamp(2, new Timestamp(timestamp * 1000));

            try(var result = stmnt.executeQuery()) {
                while(result.next()) {
                    FlashBackNote note = new FlashBackNote();
                    note.setNoteId(result.getInt(1));
                    note.setNote(result.getString(2));
                    note.setModified(result.getTimestamp(3));
                    note.setCreated(result.getTimestamp(4));

                    loadNoteFiles(conn, note);
                    loadNoteTags(conn, note);

                    notes.add(note);
                }
            }
            return notes;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static List<FlashBackFile> getNoteFiles(Integer noteId) throws FlashbackException {
        String sql = "SELECT files.file_hash, type, extension, size, telegram_file_id FROM files JOIN note_files "
            + " USING (file_hash) WHERE note_files.note_id = ?";
        try(var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, noteId);
            var result = stmnt.executeQuery();

            List<FlashBackFile> files = new ArrayList<>();
            while(result.next()) {
                FlashBackFile file = new FlashBackFile();
                file.setHash(result.getString(1));
                file.setFileType(FlashBackFile.Type.typeOf(result.getInt(2)));
                file.setExtension(result.getString(3));
                file.setSize(result.getLong(4));
                file.setTelegramFileId(result.getString(5));
                files.add(file);
            }

            return files;
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void saveFiles(List<FlashBackFile> files) throws FlashbackException {
        String sql = "INSERT INTO files (file_hash, type, extension, size, telegram_file_id) "
            + " VALUES (?, ?, ?, ?, ?) ON CONFlICT DO NOTHING";
        try(var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {
            for(var file : files) {
                stmnt.setString(1, file.getHash());
                stmnt.setInt(2, file.getFileType().getValue());
                stmnt.setString(3, file.getExtension());
                stmnt.setLong(4, file.getSize());
                stmnt.setString(5, file.getTelegramFileId());
                stmnt.addBatch();
            }
            stmnt.executeBatch();
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNoteFiles(Integer noteId, List<FlashBackFile> files) throws FlashbackException {
        String sql ="INSERT INTO note_files (note_id, file_hash) VALUES (?, ?) ON CONFlICT DO NOTHING";
        try (var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, noteId);
            for(var file : files) {
                stmnt.setString(2, file.getHash());
                stmnt.addBatch();
            }
            stmnt.executeBatch();
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void saveTelegramFileIds(List<FlashBackFile> files) throws FlashbackException {
        String sql ="UPDATE files SET telegram_file_id = ? WHERE file_hash = ?";
        try (var conn = ds.getConnection();
            var stmnt = conn.prepareStatement(sql)) {

            for(var file : files) {
                stmnt.setString(1, file.getTelegramFileId());
                stmnt.setString(2, file.getHash());
                stmnt.addBatch();
            }
            stmnt.executeBatch();
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }

    }

    public static void removeNoteFile(Integer userId, Integer noteId, String hash) throws FlashbackException {
        String sql = "DELETE FROM note_files " +
                    "WHERE note_id = ? " +
                    "AND file_hash = ? " +
                    "AND EXISTS (" +
                    "    SELECT 1 " +
                    "    FROM notes " +
                    "    WHERE note_id = note_files.note_id " +
                    "    AND owner_id = ?" +
                    ")";

        try (var conn = ds.getConnection();
            var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, noteId);
            stmt.setString(2, hash);
            stmt.setInt(3, userId);

            if(stmt.executeUpdate() == 0) {
                throw new NoteNotFound();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void removeNoteTag(Integer userId, Integer noteId, String tag) throws FlashbackException {
        String sql = "DELETE FROM note_tags " +
                    "WHERE note_id = ? " +
                    "AND tag = ? " +
                    "AND EXISTS (" +
                    "    SELECT 1 " +
                    "    FROM notes " +
                    "    WHERE note_id = note_tags.note_id " +
                    "    AND owner_id = ?" +
                    ")";

        try (var conn = ds.getConnection();
            var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, noteId);
            stmt.setString(2, tag);
            stmt.setInt(3, userId);

            if(stmt.executeUpdate() == 0) {
                throw new NoteNotFound();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }
}
