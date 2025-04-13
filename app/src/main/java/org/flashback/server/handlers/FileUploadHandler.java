package org.flashback.server.handlers;

import org.flashback.server.RequestResponsePair;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.nio.file.Files;

public class FileUploadHandler extends Handler {
    private final static int fileSizeMax = 20 * 1024 * 1024;
    private final static Path tempDir = Path.of(".");
    //private final static String destDir = "uploads";

    public static void handle(RequestResponsePair exchange) {
        if(!JakartaServletDiskFileUpload.isMultipartContent(exchange.getRequest())){
            ErrorHandler.handle(400, "Upload should be multipart/form-data form", exchange);
            return;
        }

        DiskFileItemFactory factory = DiskFileItemFactory.builder()
            .setBufferSize(0)
            .setPath(tempDir)
            .get();

        JakartaServletDiskFileUpload upload = new JakartaServletDiskFileUpload(factory);
        upload.setFileSizeMax(fileSizeMax);

        try{

            List<DiskFileItem> items = upload.parseRequest(exchange.getRequest());
            int fileCount = 0;

            for(DiskFileItem item: items) {

                if(!item.isFormField()) {

                    ++fileCount;

                    String fileName = item.getName();
                    String contentType = item.getContentType();

                    Path dest = tempDir.resolve(fileName);
                    try(InputStream in = item.getInputStream()) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        // TODO: save file
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(fileCount == 0) {
                ErrorHandler.handle(400, "Empty request", exchange);
            }
            else {
                SuccessHandler.handle("Files uploaded: " + fileCount, exchange);
            }
        }
        catch(FileUploadByteCountLimitException e){
            ErrorHandler.handle(415, "File too big. Must be less that 20MB: [" + e.getFileName() + "]", exchange);
        }
        catch(FileUploadException e) {
            e.printStackTrace();
            ErrorHandler.handle(500, "File upload failed", exchange);
        }
    }
}
