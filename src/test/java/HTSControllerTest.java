import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HTSControllerTest {

    private static Thread serverThread;
    private static HTSController controller;

    @BeforeEach
    public void startServer() {
        controller = new HTSController();
        serverThread = new Thread(() -> {
            controller.run();
        });
        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    public void stopServer() {
        controller.stopServer();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testValidRequest() throws Exception {
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            // Simulate valid request to localhost:4221
            outputStream.write("GET / HTTP/1.1\r\nHost: localhost:4221\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = reader.readLine();

            // Check if response is 200 OK
            assertEquals("HTTP/1.1 200 OK", responseLine);
        }
    }

    @Test
    public void testInvalidHost() throws Exception {
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            // Simulate request with invalid host
            outputStream.write("GET / HTTP/1.1\r\nHost: localhost:4221\\banana\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = reader.readLine();

            // Check if response is 404 Not Found
            assertEquals("HTTP/1.1 404 Not Found", responseLine);
        }
    }

    @Test
    public void testMissingHostHeader() throws Exception {
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();

            // WHEN
            outputStream.write("GET / HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = reader.readLine();

            // THEN
            assertEquals("HTTP/1.1 404 Not Found", responseLine);
        }
    }

    @Test
    public void testEchoString() throws Exception {
        // GIVEN
        String testString = "banana";
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET /echo/" +
                    testString +
                    " HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n\r\n";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();

            String contentType = null;
            String contentLength = null;
            String contentEncoding = null;

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Type:")) {
                    contentType = line.substring("Content-Type:".length()).trim();
                }
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.substring("Content-Length:".length()).trim();
                }
                if (line.startsWith("Content-Encoding:")) {
                    contentEncoding = "Header Present";
                }
            }

            String responseBody = reader.readLine();

            // THEN
            assertEquals("HTTP/1.1 200 OK", statusLine);
            assertEquals("text/plain", contentType);
            assertEquals(String.valueOf(testString.length()), contentLength);
            assertNull(contentEncoding);
            assertEquals("banana", responseBody);
        }
    }

    @Test
    public void testUserAgentEndpoint() throws Exception {
        // GIVEN
        String expectedAgent = "ukulele/1.2.3";
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET /user-agent" +
                    " HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n" +
                    "User-Agent: " + expectedAgent + "\r\n" +
                    "\r\n";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();

            String contentType = null;
            String contentLength = null;

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Type:")) {
                    contentType = line.substring("Content-Type:".length()).trim();
                }
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.substring("Content-Length:".length()).trim();
                }
            }

            String responseBody = reader.readLine();

            // THEN
            assertEquals("HTTP/1.1 200 OK", statusLine);
            assertEquals("text/plain", contentType);
            assertEquals(String.valueOf(expectedAgent.length()), contentLength);
            assertEquals(expectedAgent, responseBody);
        }
    }

   /*
   @Test
    public void testGetFile_whenFileExists() throws Exception {
        AppConfig.directory = "C:\\Users\\vook7\\IdeaProjects\\codecrafters-http-server-java\\src\\main\\resources\\tmp";
        // GIVEN
        String expected = "Alpha streaming";
        String fileName = "alpha.txt";

        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET /files/" +
                    fileName +
                    " HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n\r\n";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();

            String contentType = null;
            String contentLength = null;

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Type:")) {
                    contentType = line.substring("Content-Type:".length()).trim();
                }
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.substring("Content-Length:".length()).trim();
                }
            }

            String responseBody = reader.readLine();

            // THEN
            assertEquals("HTTP/1.1 200 OK", statusLine);
            assertEquals("application/octet-stream", contentType);
            assertEquals(String.valueOf(expected.length()), contentLength);
            assertEquals(expected, responseBody);
        }
    }
    */

    @Test
    public void testGetFile_whenFileNotFound() throws Exception {
        // GIVEN
        String fileName = "foo";

        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET /files/" +
                    fileName +
                    " HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n\r\n";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();

            // THEN
            assertEquals("HTTP/1.1 404 Not Found", statusLine);
        }
    }

    @Test
    void postFile_contentTypeMissing() throws IOException {
        try (Socket socket = new Socket("localhost", 4221)) {

            // GIVEN
            OutputStream outputStream = socket.getOutputStream();

            String request = "POST /files/number HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n" +
                    "Content-Length: 5\r\n" +
                    "\r\n" +
                    "12345";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Skipping header lines
            }

            StringBuilder responseBody = new StringBuilder();
            int charRead;
            // Read until the end of the stream
            while ((charRead = reader.read()) != -1) {
                responseBody.append((char) charRead);
            }

            String body = responseBody.toString();

            // THEN
            assertEquals(HTSConstants.HTTP_BAD_REQUEST, responseLine);
            assertEquals("Content-Type must be application/octet-stream", body);
        }
    }

    @Test
    void postFile_contentLengthMissing() throws IOException {
        try (Socket socket = new Socket("localhost", 4221)) {

            // GIVEN
            OutputStream outputStream = socket.getOutputStream();

            String request = "POST /files/number HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "\r\n" +
                    "12345";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Skipping header lines
            }

            // Read the response body
            StringBuilder responseBody = new StringBuilder();
            int charRead;
            // Read until the end of the stream
            while ((charRead = reader.read()) != -1) {
                responseBody.append((char) charRead);
            }

            String body = responseBody.toString();

            // THEN
            assertEquals(HTSConstants.HTTP_BAD_REQUEST, responseLine);
            assertEquals("Content-Length is required", body);
        }
    }

    @Test
    public void echo_gzipEncodingTest() throws Exception {
        // GIVEN
        String testString = "banana";
        try (Socket socket = new Socket("localhost", 4221)) {
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET /echo/" +
                    testString +
                    " HTTP/1.1\r\n" +
                    "Host: localhost:4221\r\n" +
                    "Accept-Encoding: " + "gzip" + "\r\n" +
                    "\r\n";

            // WHEN
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();

            String contentType = null;
            String contentLength = null;
            String contentEncoding = null;

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Type:")) {
                    contentType = line.substring("Content-Type:".length()).trim();
                }
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.substring("Content-Length:".length()).trim();
                }
                if (line.startsWith("Content-Encoding:")) {
                    contentEncoding = line.substring("Content-Encoding:".length()).trim();
                }
            }

            // THEN
            assertEquals("HTTP/1.1 200 OK", statusLine);
            assertEquals("text/plain", contentType);
            assertEquals(String.valueOf(24), contentLength);
            assertEquals("gzip", contentEncoding);
        }
    }
}