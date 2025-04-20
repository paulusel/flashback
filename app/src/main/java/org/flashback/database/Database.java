package org.flashback.database;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.Config;
import org.flashback.types.User;
import org.flashback.types.FileAddRemoveRequest;
import org.flashback.types.MemoFile;
import org.flashback.types.MemoItem;
import org.mindrot.jbcrypt.BCrypt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

    private static HikariDataSource ds;

    public static void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.getDatabaseUrl());
        config.setUsername(Config.getDatabaseUserName());
        config.setPassword(Config.getDbpassword());

        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);
    }

    public static User getUser(String username) throws FlashbackException {
        try(var conn = ds.getConnection()) {

            var stmnt = conn.prepareStatement("SELECT telegram_user_id FROM users WHERE username = ?");
            stmnt.setString(1, username);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user not found");
            }

            var usr = new User();
            usr.setUsername(username);
            usr.setTelegramUserId(result.getString(1));
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

    public static boolean authenticate(User user) throws FlashbackException {
        try (var conn = ds.getConnection()){

            if(user.getPassword() == null || user.getUsername() == null){
                throw new FlashbackException("username or password missing");
            }

            var stmnt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            stmnt.setString(1, user.getUsername());
            var result = stmnt.executeQuery();
            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "user not found");
            }

            return BCrypt.checkpw(user.getPassword(), result.getString(1));

        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void deleteUser(User user) throws FlashbackException {
        if(!authenticate(user)) {
            throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "not authorized to take action");
        }

        try(var conn = ds.getConnection()){
            var stmnt = conn.prepareStatement("DELETE FROM users WHERE username = ?");
            stmnt.setString(1, user.getUsername());
            stmnt.execute();
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void addNewUser(User user) throws FlashbackException {
        if(user.getUsername() == null || user.getPassword() == null) {
            throw new FlashbackException("missing password or username");
        }

        try (var conn = ds.getConnection()){
            String passwordHash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
            var stmnt = conn.prepareStatement("INSERT INTO users (username, password, telegram_user_id) VALUES (?, ?, ?)");
            stmnt.setString(1, user.getUsername());
            stmnt.setString(2, passwordHash);
            stmnt.setString(3, user.getTelegramUserId());
            stmnt.execute();
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void verifyNewMemoes(Connection conn, String username, MemoItem[] memo_Items) throws SQLException, FlashbackException{

        var ownerCheckStmnt = conn.prepareStatement("SELECT username FROM file_owners WHERE username = ? AND file_id = ?");
        ownerCheckStmnt.setString(1, username);

        var parentCheckStmnt = conn.prepareStatement("SELECT type FROM memo_items WHERE username = ? AND item_id = ?");
        parentCheckStmnt.setString(1, username);

        // file_ids already processed so that we don't have to process them again
        HashSet<String> visited = new HashSet<>();

        for(var item : memo_Items) {
            if(item.getType() == null || item.getName() == null) {
                throw new FlashbackException("invalid item: item missing 'type' or 'name' attribute");
            }

            if(item.getType() == MemoItem.ItemType.FOLDER && (item.getNote() != null || item.getFiles() != null)) {
                throw new FlashbackException("invalid item: folder can't have body or files");
            }

            if(item.getParent() != null) {
                parentCheckStmnt.setInt(2, item.getParent());
                var parentResult = parentCheckStmnt.executeQuery();
                if(!parentResult.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "parent not found");
                }
                if(parentResult.getInt(1) == 1) {
                    throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "invalid parent: expected folder, found note");
                }
            }

            if(item.getFiles() != null) {
                for(var file : item.getFiles()) {

                    if(!visited.add(file.getFileId())) continue;

                    ownerCheckStmnt.setString(2, file.getFileId());
                    var result = ownerCheckStmnt.executeQuery();
                    if(!result.next()) {
                        throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
                    }
                }
            }
        }
    }

    public static List<MemoItem> addMemo(String username, MemoItem[] memo_Items) throws FlashbackException {
        try(var conn = ds.getConnection()) {

            verifyNewMemoes(conn, username, memo_Items);

            var memoInsertStmnt = conn.prepareStatement(
                "INSERT INTO memo_items (type, parent, name, note, username) VALUES (?,?,?,?,?) RETURNING item_id");
            memoInsertStmnt.setString(5, username);
            var fileInsertStmnt = conn.prepareStatement("INSERT INTO memo_files (item_id, file_id) VALUES (?,?)");
            var tagInsertStmnt = conn.prepareStatement("INSERT INTO tags (item_id, tag) VALUES (?,?)");

            List<MemoItem> processed = new ArrayList<>();
            for(var memo : memo_Items ) {

                memoInsertStmnt.setInt(1, memo.getType() == MemoItem.ItemType.NOTE ? 1 : 0);
                memoInsertStmnt.setInt(2, memo.getParent() == null ? 1 : memo.getParent());
                memoInsertStmnt.setString(3, memo.getName());
                memoInsertStmnt.setString(4, memo.getNote());

                var result = memoInsertStmnt.executeQuery();
                result.next();
                Integer itemId = result.getInt(1);

                if(memo.getFiles() != null) {
                    fileInsertStmnt.setInt(1, itemId);
                    for(var file : memo.getFiles()) {
                        fileInsertStmnt.setString(2, file.getFileId());
                        fileInsertStmnt.addBatch();
                    }
                    fileInsertStmnt.executeBatch();
                }

                if(memo.getTags() != null){
                    tagInsertStmnt.setInt(1, itemId);
                    for(var tag : memo.getTags()) {
                        tagInsertStmnt.setString(2, tag);
                        tagInsertStmnt.addBatch();
                    }
                    tagInsertStmnt.executeBatch();
                }

                var item = new MemoItem();
                item.setItemid(itemId);
                item.setTempId(memo.getTempId());
                processed.add(item);
            }

            return processed;
        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static List<MemoItem> getMemoItems(String username, int[] item_ids) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            List<MemoItem> mItems = new ArrayList<>();
            var stmnt = conn.prepareStatement(
                "SELECT username, type, name, parent, note, modified  FROM memo_items WHERE item_id = ?");
            for(int item_id : item_ids) {
                stmnt.setInt(1, item_id);
                var result = stmnt.executeQuery();
                if(!result.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "item not found: [" + item_id + "]");
                }

                if(!username.equals(result.getString(1))) {
                    throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "unauthorized to access memo: [" + item_id + "]");
                }

                MemoItem item = new MemoItem();
                item.setType(result.getInt(2) == 1 ? MemoItem.ItemType.NOTE : MemoItem.ItemType.FOLDER);
                item.setName(result.getString(3));
                item.setParent(result.getInt(4));
                item.setNote(result.getString(5));
                item.setModified(result.getTimestamp(6));
                mItems.add(item);
            }

            return mItems;
        }
        catch(FlashbackException e) {
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void deleteMemoItems(String username, int[] item_ids) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("DELETE FROM memo_items WHERE item_id = ? RETURNING username");
            for(int item_id : item_ids) {
                stmnt.setInt(1, item_id);
                var result = stmnt.executeQuery();
                if(result.next() && !result.getString(1).equals(username)) {
                    throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "not authorized to take action");
                }
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

    public static void addMemoFiles(String username, FileAddRemoveRequest[] requests) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("INSERT INTO memo_files (item_id, file_id) VALUES (? , ?)");
            var owenerCheckStmnt = conn.prepareStatement("SELECT username FROM file_owners WHERE file_id = ? AND username = ?");
            for(var request : requests) {
                if(request.getMemoId() == null || request.getFileId() == null) {
                    throw new FlashbackException("invalid request: itemId or fileId missing");
                }

                owenerCheckStmnt.setString(1, request.getFileId());
                owenerCheckStmnt.setString(2, username);
                var owner = owenerCheckStmnt.executeQuery();

                if(!owner.next()) {
                    throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
                }

                stmnt.setInt(1, request.getMemoId());
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

    public static void removeMemoFiles(String username, FileAddRemoveRequest[] requests) throws FlashbackException {
        try (var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("DELETE FROM memo_files WHERE item_id = ? AND file_id = ?");
            for(var request : requests) {
                stmnt.setInt(1, request.getMemoId());
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

    public static MemoFile getFile(String username, String fileId) throws FlashbackException {
        try(var conn = ds.getConnection()) {
            var stmnt = conn.prepareStatement("SELECT telegram_file_id, original_name, size, mime_type " +
                " FROM file_owners JOIN files USING (file_id) WHERE username = ? AND file_id = ?");
            stmnt.setString(1, username);
            stmnt.setString(2, fileId);
            var result = stmnt.executeQuery();

            if(!result.next()) {
                throw new FlashbackException(HttpStatus.NOT_FOUND_404, "file not found");
            }

            MemoFile file = new MemoFile();
            file.setFileId(fileId);
            file.setTelegramFileId(result.getString(1));
            file.setOriginalName(result.getString(2));
            file.setSize(result.getInt(3));
            file.setMime_type(result.getString(4));
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

    public static void addFiles(String username, List<MemoFile> files) throws FlashbackException {
        try (var conn = ds.getConnection()) {
            //inset file
            var ownerInsertStmnt = conn.prepareStatement(
                "INSERT INTO file_owners (username, file_id) VALUES (?, ?) ON CONFLICT (username, file_id) DO NOTHING"
            );
            ownerInsertStmnt.setString(1, username);

            var fileInsertStmnt = conn.prepareStatement(
                "INSERT INTO files (file_id, telegram_file_id, original_name, mime_type, size)"
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (file_id) DO NOTHING"
            );
            for(var file : files) {
                fileInsertStmnt.setString(1, file.getFileId());
                fileInsertStmnt.setString(2, file.getTelegramFileId());
                fileInsertStmnt.setString(3, file.getOriginalName());
                fileInsertStmnt.setString(4, file.getMime_type());
                fileInsertStmnt.setInt(5, file.getSize());

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
}
