package org.flashback.server.handlers;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FlashBackFile;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.*;

public class FileDownloadHandler {
    public static void handle(RequestResponsePair exchange) {
        try{
            Integer userId = Authenticator.authenticate(exchange.request);
            String[] tokens = exchange.request.getRequestURI().split("/");

            if(
                tokens.length < 4 ||
                (tokens[3].equalsIgnoreCase("segment") && tokens.length != 5) ||
                (!tokens[3].equalsIgnoreCase("segment") && tokens.length != 4))
            {
                throw new FlashbackException(
                    HttpStatus.BAD_REQUEST_400,
                    "malformed file path"
                );
            }

            String fileHash = tokens[2].toLowerCase();
            String action = tokens[3];

            if(action.equalsIgnoreCase("segment")) {
                try {
                    Integer segmentNo = Integer.valueOf(tokens[4]);
                    handleSegmentRequest(exchange, userId, fileHash, segmentNo);
                }
                catch(NumberFormatException e) {
                    throw new FlashbackException(
                        HttpStatus.BAD_REQUEST_400,
                        "invalid segment number: [" + tokens[4] + "]"
                    );
                }
            }
            else if(action.equalsIgnoreCase("display")) {
                handleFileFetchRequest(exchange, userId, fileHash, false);
            }
            else if(action.equalsIgnoreCase("thumbnail")) {
                handleThumbnailRequest(exchange, userId, fileHash);
            }
            else if(action.equalsIgnoreCase("playlist")) {
                handlePlaylistRequest(exchange, userId, fileHash);
            }
            else if(action.equalsIgnoreCase("download")) {
                handleFileFetchRequest(exchange, userId, fileHash, true);
            }
            else {
                throw new FlashbackException(
                    HttpStatus.BAD_REQUEST_400,
                    "unrecognized file request: [" + action + "]"
                );
            }

        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
        catch(Exception e) {
            e.printStackTrace();
            GenericHandler.handleException(exchange, new FlashbackException());
        }
    }

    private static void handlePlaylistRequest(
        RequestResponsePair exchange,
        Integer userId,
        String fileHash) throws FlashbackException
    {
        FlashBackFile file = Database.getFile(userId, fileHash);

        if(file.getFileType() != FlashBackFile.Type.AUDIO && file.getFileType() != FlashBackFile.Type.VIDEO) {
            throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "only videos and audios have playlists");
        }

        Path filePath =  Path.of(Config.getValue("uploads_dir"))
            .resolve(file.getHash())
            .resolve("playlist.m3u8");

        try {
            GenericHandler.sendFile(exchange, filePath, false);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void handleFileFetchRequest(
        RequestResponsePair exchange,
        Integer userId,
        String fileHash,
        boolean download) throws FlashbackException
    {
        // we need file extension and also confirm ownwership
        FlashBackFile file = Database.getFile(userId, fileHash);

        Path filePath = Path.of(Config.getValue("uploads_dir"))
            .resolve(file.getHash())
            .resolve(file.getFileName());

        try {
            GenericHandler.sendFile(exchange, filePath, download);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void handleThumbnailRequest (
        RequestResponsePair exchange,
        Integer userId,
        String fileHash) throws FlashbackException
    {
        FlashBackFile file = Database.getFile(userId, fileHash);

        if(file.getFileType() != FlashBackFile.Type.VIDEO) {
            throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "only videos have thumbnails");
        }

        Path filePath = Path.of(Config.getValue("uploads_dir"))
            .resolve(file.getHash())
            .resolve("thumbnail.jpg");

        try {
            GenericHandler.sendFile(exchange, filePath, false);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static void handleSegmentRequest (
        RequestResponsePair exchange,
        Integer userId,
        String fileHash,
        Integer segmentNo) throws FlashbackException
    {
        // this just for ownwership confirmation
        FlashBackFile file = Database.getFile(userId, fileHash);

        if(file.getFileType() != FlashBackFile.Type.AUDIO && file.getFileType() != FlashBackFile.Type.VIDEO) {
            throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "only videos and audios have stream segments");
        }

        Path filePath = Path.of(Config.getValue("uploads_dir"))
            .resolve(file.getHash())
            .resolve(segmentNo + ".ts");

        try {
            GenericHandler.sendFile(exchange, filePath, false);
        }
        catch(FileNotFoundException e) {
            throw new FlashbackException(HttpStatus.NOT_FOUND_404, "segment not found");
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }
}
