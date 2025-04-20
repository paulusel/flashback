package org.flashback.server.handlers;

import org.flashback.types.RequestResponsePair;
import org.flashback.auth.Authenticator;
import org.flashback.types.ListResponse;
import org.flashback.types.MemoFile;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.Config;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.nio.file.Files;

public class FileUploadHandler extends Handler {
    //private final static int fileSizeMax = 20 * 1024 * 1024;
    private final static Path tempDir = Path.of(".");
    private final static Path destDir = Config.getUploadsdir();
    private static final DiskFileItemFactory factory;
    private static final JakartaServletDiskFileUpload upload;

    static {
        factory = DiskFileItemFactory.builder()
            .setBufferSize(0)
            .setPath(tempDir)
            .get();
        upload = new JakartaServletDiskFileUpload(factory);
        //upload.setFileSizeMax(fileSizeMax);
    }

    public static void handle(RequestResponsePair exchange) {

        List<MemoFile> responseFiles = new ArrayList<>();
        List<MemoFile> uploadedFiles = new ArrayList<>();

        try{
            String username = Authenticator.authenticate(exchange.getRequest());

            if(!JakartaServletDiskFileUpload.isMultipartContent(exchange.getRequest())){
                throw new FlashbackException("invalid content: expected multipart/form-data");
            }

            try {
                var iterator = upload.getItemIterator(exchange.getRequest());

                while(iterator.hasNext()) {
                    var file = processFile(iterator.next());
                    uploadedFiles.add(file);

                    MemoFile responseFile = new MemoFile();
                    responseFile.setTempId(file.getTempId());
                    responseFile.setFileId(file.getFileId());
                    responseFiles.add(responseFile);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                throw new FlashbackException();
            }
            finally {
                if(!uploadedFiles.isEmpty()) {
                    Database.addFiles(username, uploadedFiles);
                }
            }

            if(uploadedFiles.isEmpty()) {
                throw new FlashbackException("no file included");
            }

            ListResponse<MemoFile> response = new ListResponse<>(true, HttpStatus.OK_200, responseFiles);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            ListResponse<MemoFile> response = new ListResponse<>(false, e.getStatusCode(), responseFiles);
            response.setMessage(e.getMessage());
            Handler.sendJsonResponse(response, exchange);
        }
    }

    private static MemoFile processFile(FileItemInput item) throws FlashbackException {
        Integer tempId = Integer.valueOf(item.getFieldName());
        String fileName = item.getName();
        String contentType = item.getContentType();
        Path filePath = tempDir.resolve(fileName);

        if(tempId == null || fileName == null || contentType == null) {
            throw new FlashbackException("invalid upload form: missing fieldname, filename or content type");
        }

        try{
            InputStream stream = item.getInputStream();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestStream = new DigestInputStream(stream, digest);
            Files.copy(digestStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String hash = HexFormat.of().formatHex(digest.digest());
            Path destPath = destDir.resolve(hash);
            Files.move(filePath, destPath, StandardCopyOption.REPLACE_EXISTING);


            MemoFile file = new MemoFile();
            file.setFileId(hash);
            int size = (int) Files.size(destPath);
            file.setSize(size);
            file.setMime_type(contentType);
            file.setOriginalName(fileName);
            file.setTempId(tempId);

            return file;
        }
        catch(Exception e){
            e.printStackTrace();
            try{
                Files.deleteIfExists(filePath);
            }
            catch(IOException ex) {
                e.printStackTrace();
            }
            throw new FlashbackException();
        }
    }

}
