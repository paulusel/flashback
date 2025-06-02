package org.flashback.helpers;

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.Note;
import org.flashback.types.NoteFile;

import jakarta.servlet.http.HttpServletRequest;

public class NoteProcessor {
    //private final static int fileSizeMax = 20 * 1024 * 1024;
    private static List<String> vidoes_extensions = Arrays.asList("mp4", "mov", "mkv", "avi", "webm", "flv");
    private static Path tempDir;
    private static Path destDir;
    private static DiskFileItemFactory factory;
    private static JakartaServletDiskFileUpload upload;

    public static void init() throws Exception {

        String upload_dir = Config.getValue("uploads_dir");
        String temp_dir = Config.getValue("temp_dir");

        if(upload_dir == null || upload_dir.isEmpty()) {
            throw new Exception("no uploads dir given in config");
        }

        if(temp_dir == null || temp_dir.isEmpty()) {
            temp_dir = FileUtils.getTempDirectoryPath();
        }

        destDir = Path.of(upload_dir);
        tempDir = Path.of(temp_dir);

        if(!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        if(!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        factory = DiskFileItemFactory.builder()
            .setBufferSize(0)
            .setPath(tempDir)
            .get();
        upload = new JakartaServletDiskFileUpload(factory);
        //upload.setFileSizeMax(fileSizeMax);
    }

    public static Note extractNoteFromForm(HttpServletRequest request) throws FlashbackException {

        if(!JakartaServletDiskFileUpload.isMultipartContent(request)){
            throw new FlashbackException("invalid content: expected multipart/form-data");
        }

        try {
            Note note = new Note();
            var iterator = upload.getItemIterator(request);
            while(iterator.hasNext()) {
                FileItemInput item = iterator.next();
                if(item.isFormField()) {
                    String fieldName = item.getFieldName();
                    String txt = getFieldText(item);

                    if(fieldName.equals("note")) {
                        note.setNote(txt);
                    }
                    else if(fieldName.equals("note_id")) {
                        int id = Integer.valueOf(txt);
                        note.setNoteId(id);
                    }
                    else if(fieldName.startsWith("tag")) {
                        note.addTag(txt);
                    }
                }
                else {
                    NoteFile file = processFile(iterator.next());
                    note.addFile(file);
                }
            }

            return note;
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    private static String getFieldText(FileItemInput item) {
        if(!item.isFormField()) return null;
        try (Scanner in = new Scanner(item.getInputStream(), StandardCharsets.UTF_8)) {
            in.useDelimiter("\\A");
            return in.next();
        }
        catch(IOException e) {
            return null;
        }
    }

    private static NoteFile processFile(FileItemInput item) throws FlashbackException {
        try{
            String fileName = item.getName();
            Path filePath = tempDir.resolve(fileName);

            InputStream stream = item.getInputStream();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestStream = new DigestInputStream(stream, digest);
            Files.copy(digestStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            String hash = HexFormat.of().formatHex(digest.digest());

            String extension = FilenameUtils.getExtension(fileName);
            String contentType = Files.probeContentType(filePath);

            NoteFile file = new NoteFile();
            file.setFileId(hash);
            file.setExtension(extension);

            Path destPath = tempDir.resolve(file.getFileName());
            Files.move(filePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            file.setSize(Files.size(destPath));
            file.setMimeType(contentType);

            return file;
        }
        catch(Exception e){
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void cleanFiles(Long userId, Note note) {
        for(var file : note.getFiles()) {
            try {
                Files.deleteIfExists(tempDir.resolve(file.getFileName()));
                File file_dir = new File(destDir.resolve(file.getFileId()).toString());
                if(file_dir.exists()) {
                    FileUtils.deleteDirectory(file_dir);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void postProcessFiles(Long userId, Note note) throws Exception {
        Path user_dir = destDir.resolve(String.valueOf(userId));
        if(!Files.exists(user_dir)) {
            Files.createDirectory(user_dir);
        }

        for(NoteFile file: note.getFiles()) {
            Path src = tempDir.resolve(file.getFileName());
            Path dest_dir = user_dir.resolve(file.getFileId());
            Files.createDirectory(dest_dir);
            Path dest = dest_dir.resolve(file.getFileName());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);

            if(vidoes_extensions.contains(file.getExtension())) {
                ffmpegProcessVideo(dest);
            }
        }
    }

    private static void ffmpegProcessVideo(Path video_path) throws Exception {
        String filename = video_path.toString();
        String output_path = FilenameUtils.getBaseName(filename);

        List<String> command = Arrays.asList(
            "ffmpeg", "-i", filename,
            "-c:v", "libx264",
            "-crf", "23",
            "-preset", "veryfast",
            "-profile:v", "main",
            "-level", "3.1",
            "-vf", "scale=-2:720",
            "-c:a", "aac",
            "-b:a", "128k",
            "-g", "60",
            "-keyint_min", "60",
            "-sc_threshold", "0",
            "-f", "hls",
            "-hls_time", "6",
            "-hls_list_size", "0",
            "-hls_playlist_type", "vod",
            "-hls_segment_filename", output_path + "/%d.ts",
            output_path + "/playlist.m3u8"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if(exitCode != 0) throw new Exception();
    }
}
