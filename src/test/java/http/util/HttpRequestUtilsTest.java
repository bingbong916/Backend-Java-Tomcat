package http.util;

import org.junit.jupiter.api.Test;
import webserver.RequestHandler;

import java.nio.file.*;
import java.util.Map;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
class HttpRequestUtilsTest {

    @Test
    void parseQuery() {
        Map<String, String> queryParameter = HttpRequestUtils.parseQueryParameter("userId=1");
        assertEquals("1", queryParameter.get("userId"));
    }

    @Test
    void parseQueryMore() {
        Map<String, String> queryParameter = HttpRequestUtils.parseQueryParameter("userId=1&password=1");
        assertEquals("1", queryParameter.get("userId"));
        assertEquals("1", queryParameter.get("password"));
    }

    @Test
    void parseQueryZero() {
        Map<String, String> queryParameter = HttpRequestUtils.parseQueryParameter("");
    }

    @Test
    void 리다이렉트_302() throws IOException {
        // given
        String requestData = "POST /user/signup HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Length: 68\r\n" +
                "\r\n";
        InputStream inputStream = new ByteArrayInputStream(requestData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Socket socket = new MockSocket(inputStream, outputStream);
        RequestHandler requestHandler = new RequestHandler(socket);

        // when
        requestHandler.run();

        // then
        String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(response.contains("HTTP/1.1 302 Found"));
        assertTrue(response.contains("Location: /index.html"));
    }
    @Test
    void 유저_생성시_리다이렉트() throws IOException {
        // given
        String requestData = "POST /user/signup HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Length: 68\r\n" +
                "\r\n" +
                "userId=javajigi&password=password&name=JaeSung&email=javajigi%40slipp.net";
        InputStream inputStream = new ByteArrayInputStream(requestData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Socket socket = new MockSocket(inputStream, outputStream);
        RequestHandler requestHandler = new RequestHandler(socket);

        // when
        requestHandler.run();

        // then
        String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(response.contains("HTTP/1.1 302 Found"));
        assertTrue(response.contains("Location: /index.html"));
    }


    private static class MockSocket extends Socket {
        private final InputStream input;
        private final OutputStream output;

        public MockSocket(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }
        @Override
        public InputStream getInputStream() throws IOException {
            return input;
        }
        @Override
        public OutputStream getOutputStream() throws IOException {
            return output;
        }
    }
}
