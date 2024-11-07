import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class HTSService {

    private final FileRepository fileRepository = new FileRepository();
    private int index = 1;
    private String encoding = "none";
    private final List<byte[]> gzipEncodedBody = new ArrayList<>();

    public void handleRequest(Socket clientSocket) {
        System.out.println("Processing request number " + index);
        index++;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = reader.readLine();

            if (requestLine != null) {
                String[] parts = requestLine.split(" ");
                String httpRequestMethod = parts[0];
                String path = parts[1];
                var headers = readHeaders(reader);

                String response;
                if (httpRequestMethod.equals(HTSConstants.GET)) {
                    response = processGetRequest(path, headers);
                } else if (httpRequestMethod.equals(HTSConstants.POST)) {
                    response = processPostRequest(path, headers, reader);
                } else {
                    response = HTSConstants.HTTP_REQUEST_NOT_ALLOWED + HTSConstants.RN_RN;
                }

                var outputStream = clientSocket.getOutputStream();
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                if (!gzipEncodedBody.isEmpty()) {
                    outputStream.write(gzipEncodedBody.getFirst());
                    gzipEncodedBody.clear();
                }
            }
        } catch (IOException e) {
            System.out.println("Error processing request: " + e.getMessage());
        }
    }

    protected Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        System.out.println("Reading headers");
        String headerLine;
        Map<String, String> headers = new HashMap<>();

        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int index = headerLine.indexOf(':');
            if (index > 0) {
                var headerName = headerLine.substring(0, index).trim();
                var headerValue = headerLine.substring(index + 1).trim();
                headers.put(headerName, headerValue);
            }
        }
        if (headers.containsKey("Accept-Encoding")) {
            encoding = headers.get("Accept-Encoding");
        }
        return headers;
    }

    private String processGetRequest(String path, Map<String, String> headers) {
        var host = headers.get("Host");
        if ("/".equals(path) && "localhost:4221".equals(host)) {
            return HTSConstants.HTTP_OK_RN + HTSConstants.RN;
        } else if (path.startsWith("/echo/")) {
            return getEcho(path);
        } else if (path.equals("/user-agent")) {
            return getUserAgent(headers);
        } else if (path.startsWith("/files/")) {
            return getFile(path);
        } else {
            return HTSConstants.HTTP_NOT_FOUND + HTSConstants.RN_RN;
        }
    }

    private String processPostRequest(String path, Map<String, String> headers, BufferedReader reader) throws IOException {
        if (!headers.containsKey("Content-Type") || !headers.get("Content-Type").equals("application/octet-stream")) {
            return HTSConstants.HTTP_BAD_REQUEST + HTSConstants.RN_RN + "Content-Type must be application/octet-stream";
        }
        if (!headers.containsKey("Content-Length")) {
            return HTSConstants.HTTP_BAD_REQUEST + HTSConstants.RN_RN + "Content-Length is required";
        }
        if (path.startsWith("/files/")) {
            var fileContent = getFileContent(reader);
            return postFile(path, fileContent);
        } else {
            return HTSConstants.HTTP_NOT_FOUND + HTSConstants.RN_RN;
        }
    }

    private String getEcho(String path) {
        String contentEncoding = "";
        String param = path.substring("/echo/".length());

        if (encoding.contains(HTSConstants.GZIP)) {
            contentEncoding = HTSConstants.CONTENT_ENCODING + HTSConstants.GZIP + HTSConstants.RN;
            byte[] body = gzipEncode(param);
            gzipEncodedBody.add(body);
            return HTSConstants.HTTP_OK_RN +
                    contentEncoding +
                    HTSConstants.CONTENT_TYPE + HTSConstants.TEXT_PLAIN + HTSConstants.RN +
                    HTSConstants.CONTENT_LENGTH + body.length +
                    HTSConstants.RN_RN;
        }

        return HTSConstants.HTTP_OK_RN +
                contentEncoding +
                HTSConstants.CONTENT_TYPE + HTSConstants.TEXT_PLAIN + HTSConstants.RN +
                HTSConstants.CONTENT_LENGTH + param.length() +
                HTSConstants.RN_RN +
                param;
    }

    private byte[] gzipEncode(String param) {
        System.out.println("GZIP encoding body");
        var baos = new ByteArrayOutputStream();
        try {
            var gzipOut = new GZIPOutputStream(baos);
            gzipOut.write(param.getBytes(StandardCharsets.UTF_8));
            gzipOut.close();
        } catch (IOException e) {
            System.out.println("Error. GZIP encoding failed: " + e.getMessage());
        }
        return baos.toByteArray();
    }

    private String getUserAgent(Map<String, String> headers) {
        String userAgent = headers.get("User-Agent");
        if (userAgent != null) {
            return HTSConstants.HTTP_OK_RN +
                    HTSConstants.CONTENT_TYPE + HTSConstants.TEXT_PLAIN + HTSConstants.RN +
                    HTSConstants.CONTENT_LENGTH + userAgent.length() +
                    HTSConstants.RN_RN +
                    userAgent;
        } else {
            return HTSConstants.HTTP_BAD_REQUEST +
                    HTSConstants.RN_RN +
                    "User-Agent header not found";
        }
    }

    private String getFile(String path) {
        return fileRepository.getFileByName(path);
    }

    private String postFile(String path, String fileContent) {
        return fileRepository.postFile(path, fileContent);
    }

    private String getFileContent(BufferedReader reader) throws IOException {
        StringBuffer bodyBuffer = new StringBuffer();
        while (reader.ready()) {
            bodyBuffer.append((char) reader.read());
        }
        return bodyBuffer.toString();
    }
}
