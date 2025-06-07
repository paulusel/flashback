package org.flashback.helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

import java.security.DigestInputStream;
import java.security.MessageDigest;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FlashBackNote;
import org.flashback.types.FlashBackFile;

import jakarta.servlet.http.HttpServletRequest;

public class NoteProcessor {
    private static ExecutorService ffmpegProcessor = Executors.newSingleThreadExecutor();

    public static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
        Map.entry("image/jpeg", "jpg"),
        Map.entry("image/png", "png"),
        Map.entry("image/webp", "webp"),
        Map.entry("image/gif", "gif"),
        Map.entry("video/mp4", "mp4"),
        Map.entry("video/quicktime", "mov"),
        Map.entry("video/webm", "webm"),
        Map.entry("audio/mpeg", "mp3"),
        Map.entry("audio/ogg", "ogg"),
        Map.entry("application/pdf", "pdf"),
        Map.entry("application/zip", "zip"),
        Map.entry("application/x-rar-compressed", "rar"),
        Map.entry("text/plain", "txt")
    );

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

        factory = DiskFileItemFactory.builder()
            .setBufferSize(0)
            .setPath(tempDir)
            .get();
        upload = new JakartaServletDiskFileUpload(factory);
        //upload.setFileSizeMax(fileSizeMax);
    }

    public static FlashBackNote extractNoteFromForm(HttpServletRequest request) throws FlashbackException {

        if(!JakartaServletDiskFileUpload.isMultipartContent(request)){
            throw new FlashbackException("invalid content: expected multipart/form-data");
        }

        try {
            FlashBackNote note = new FlashBackNote();
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
                    else if(fieldName.equals("tag")) {
                        note.getTags().add(txt);
                    }
                }
                else {
                    try (InputStream stream = item.getInputStream()) {
                        FlashBackFile file = new FlashBackFile();
                        file = processFile(file, stream, item.getName());

                        String mime = item.getContentType();
                        if(mime.startsWith("image/")) { file.setFileType(FlashBackFile.Type.PHOTO); }
                        else if(mime.startsWith("video/")) { file.setFileType(FlashBackFile.Type.VIDEO); }
                        else if(mime.startsWith("audio/")) { file.setFileType(FlashBackFile.Type.AUDIO); }
                        else { file.setFileType(FlashBackFile.Type.DOCUMENT);}

                        note.getFiles().add(file);
                    }
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

    public static FlashBackFile processFile(FlashBackFile file, InputStream stream, String fileName) throws FlashbackException {
        try{
            Path filePath = tempDir.resolve(fileName);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestStream = new DigestInputStream(stream, digest);
            Files.copy(digestStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            String hash = HexFormat.of().formatHex(digest.digest());

            String extension = FilenameUtils.getExtension(fileName);
            String contentType = new Tika().detect(filePath);
            if(extension == null || extension.isEmpty() || extension.equals("tmp")) {
                extension = MIME_TO_EXTENSION.get(contentType);
                extension = extension == null ? "dat" : extension;
            }

            file.setHash(hash);
            file.setExtension(extension);

            Path destPath = tempDir.resolve(file.getFileName());
            Files.move(filePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            file.setSize(Files.size(destPath));

            return file;
        }
        catch(Exception e){
            e.printStackTrace();
            throw new FlashbackException();
        }
    }

    public static void cleanFiles(List<FlashBackFile> files) {
        for(var file : files) {
            try {
                Files.deleteIfExists(tempDir.resolve(file.getFileName()));
                File file_dir = new File(destDir.resolve(file.getHash()).toString());
                if(file_dir.exists()) {
                    FileUtils.deleteDirectory(file_dir);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void postProcessFiles(List<FlashBackFile> files) {
            try {
                if(!Files.exists(destDir)) {
                    Files.createDirectory(destDir);
                }

                for(FlashBackFile file: files) {
                    Path src = tempDir.resolve(file.getFileName());
                    Path dest_dir = destDir.resolve(file.getHash());

                    try { Files.createDirectory(dest_dir); }
                    catch(FileAlreadyExistsException e) { continue; }

                    Path dest = dest_dir.resolve(file.getFileName());
                    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);

                    if(file.getFileType() == FlashBackFile.Type.VIDEO) {
                        ffmpegProcessor.execute(() ->  ffmpegProcessVideo(dest));
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
    }

    private static void ffmpegProcessVideo(Path video_path) {
        try {
            String filename = video_path.toString();
            String output_path = FilenameUtils.getFullPathNoEndSeparator(filename);

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
        catch(Exception e) {
            System.err.println("postProcessing with ffmpeg failed");
            e.printStackTrace();
        }
    }
}
