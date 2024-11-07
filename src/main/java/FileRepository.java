import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileRepository {

    public String getFileByName(String path) {
        var fileName = getFileName(path);
        System.out.println("Retrieving file " + fileName);

        File file = new File(AppConfig.directory, fileName);
        if (!file.exists()) {
            return HTSConstants.HTTP_NOT_FOUND + HTSConstants.RN_RN;
        } else {
            return readFileContent(file);
        }
    }

    public String postFile(String path, String content) {
        var fileName = getFileName(path);
        System.out.println("Posting file " + fileName);

        var file = new File(AppConfig.directory, fileName);
        if (file.exists()) {
            return HTSConstants.HTTP_CONFLICT + HTSConstants.RN_RN;
        } else {
            try {
                File parentDirectory = file.getParentFile();
                if (parentDirectory != null) {
                    parentDirectory.mkdirs();
                }
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();

                return HTSConstants.HTTP_CREATED + HTSConstants.RN_RN;
            } catch (IOException e) {
                return HTSConstants.HTTP_INT_SERVER_ERROR + HTSConstants.RN_RN + e.getMessage();
            }
        }
    }

    private String readFileContent(File file) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            return HTSConstants.HTTP_OK_RN +
                    HTSConstants.CONTENT_TYPE + "application/octet-stream" + HTSConstants.RN +
                    HTSConstants.CONTENT_LENGTH + fileBytes.length +
                    HTSConstants.RN_RN +
                    new String(fileBytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            return HTSConstants.HTTP_INT_SERVER_ERROR + HTSConstants.RN_RN + e.getMessage();
        }
    }

    private String getFileName(String path) {
        var fileName = path.substring("/files/".length());
        fileName = fileName.replaceAll("\\.\\.", "");
        return fileName;
    }
}
