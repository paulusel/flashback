package org.flashback.server.handlers;

import org.flashback.server.RequestResponsePair;
import org.flashback.types.MessageResponse;
import org.flashback.database.Database;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.nio.file.Files;

public class FileUploadHandler extends Handler {
    private final static int fileSizeMax = 20 * 1024 * 1024;
    private final static Path tempDir = Path.of(".");
    private final static Path destDir = Path.of(".");
    private static final DiskFileItemFactory factory;
    private static final JakartaServletDiskFileUpload upload;

    static {
        factory = DiskFileItemFactory.builder()
            .setBufferSize(0)
            .setPath(tempDir)
            .get();
        upload = new JakartaServletDiskFileUpload(factory);
        upload.setFileSizeMax(fileSizeMax);
    }

    public static void handle(RequestResponsePair exchange, Database db) {
        if(!JakartaServletDiskFileUpload.isMultipartContent(exchange.getRequest())){
            MessageResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "Expected multipart/form-data");
            Handler.sendJson(response, exchange);
            return;
        }

        List<String> processedFiles = new ArrayList<>();
        boolean errored = true;

        try{
            int[] fileCount = {0};

            upload.getItemIterator(exchange.getRequest()).forEachRemaining(item -> {
                String name = item.getFieldName();

                if(item.isFormField()) {
                    return; //nop
                }

                fileCount[0]++;

                String fileName = item.getName();
                String contentType = item.getContentType();
                Path filePath = tempDir.resolve(fileName);

                try{
                    String hash = processFile(item, filePath);
                    processedFiles.add(hash);
                }
                catch(IOException e){
                    Files.deleteIfExists(filePath);
                    throw e;
                }

            });

            if(fileCount[0] == 0) {
                MessageResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "No File Included");
                Handler.sendJson(response, exchange);
            }
            else {
                MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, "Files Uploaded: " + fileCount[0]);
                Handler.sendJson(response, exchange);
            }
            errored = false;
        }
        catch(FileUploadByteCountLimitException e){
            MessageResponse response = new MessageResponse(false, HttpStatus.PAYLOAD_TOO_LARGE_413,
                "File Too Big. Expected Size < 20MB: [" + e.getFileName() + "]");
            Handler.sendJson(response, exchange);
        }
        catch(FileUploadException e) {
            e.printStackTrace();
            Handler.sendServerError(exchange);
        }
        catch(IOException e) {
            e.printStackTrace();
            Handler.sendServerError(exchange);
        }

        if(errored){
            for(String name: processedFiles){
                try{
                    Files.deleteIfExists(destDir.resolve(name));
                }
                catch(Exception e){}
            }
        }
    }

    private static String processFile(FileItemInput item, Path filePath) throws IOException {
        try{
            InputStream stream = item.getInputStream();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestStream = new DigestInputStream(stream, digest);
            Files.copy(digestStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String hash = HexFormat.of().formatHex(digest.digest());
            Files.move(filePath, destDir.resolve(hash), StandardCopyOption.REPLACE_EXISTING);

            return hash;
        }
        catch(NoSuchAlgorithmException e){
            throw new IOException(e);
        }
    }

}
