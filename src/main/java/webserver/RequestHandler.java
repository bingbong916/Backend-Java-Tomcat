package webserver;

import db.MemoryUserRepository;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable{
    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {

            String requestLine = br.readLine();
            if (requestLine == null) return;

            int requestContentLength = getRequestContentLength(br);

            if (requestLine.startsWith("POST /user/")) {
                handlePostRequest(requestLine, requestContentLength, br, dos);
            }
            if (requestLine.startsWith("GET /")) {
                handleGetRequest(br, dos, requestLine);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    private int getRequestContentLength(BufferedReader br) throws IOException {
        int contentLength = 0;
        while (true) {
            final String line = br.readLine();
            if (line.equals("")) {
                break;
            }
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.split(": ")[1]);
            }
        }
        return contentLength;
    }

    private void handlePostRequest(String requestLine, int requestContentLength, BufferedReader br, DataOutputStream dos) throws IOException {
        char[] body = new char[requestContentLength];
        br.read(body, 0, requestContentLength);
        String requestBody = new String(body);
        Map<String, String> params = parseQueryString(requestBody);

        if (requestLine.startsWith("POST /user/signup")) {
            handleSignupRequest(params, dos);
        }
        if (requestLine.startsWith("POST /user/login")) {
            handleLoginRequest(params, dos);
        }
    }


    private void handleGetRequest(BufferedReader br, DataOutputStream dos, String requestLine) throws IOException {
        String requestPath = requestLine.split(" ")[1];

        // 회원가입
        if ("/user/form.html".equals(requestPath)) {
            serveFile(dos, "webapp/user/form.html");
            return;
        }

        // 로그인
        if ("/user/login.html".equals(requestPath)) {
            serveFile(dos, "webapp/user/login.html");
            return;
        }

        // 로그인 실패
        if ("/user/logined_failed.html".equals(requestPath)) {
            serveFile(dos, "webapp/user/login_failed.html");
            return;
        }

        // 유저 리스트
        if ("/user/list.html".equals(requestPath) || "/user/userList".equals(requestPath)) {
            if (isLogined(br)) {
                serveFile(dos, "webapp/user/list.html");
            } else {
                response302Header(dos, "/user/login.html");
            }
            return;
        }


        // 메인 화면
        if ("/index.html".equals(requestPath) || "/".equals(requestPath)) {
            serveFile(dos, "webapp/index.html");
            return;
        }

        // css
        if (requestPath.endsWith(".css")) {
            serveCSSFile(dos, "webapp/css/styles.css");
            return;
        }

        String filePath = "webapp" + requestPath;
        if (!Files.exists(Paths.get(filePath))) {
            response404Header(dos);
        } else {
            serveFile(dos, filePath);
        }
    }
    private void serveFile(DataOutputStream dos, String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        response200Header(dos, fileBytes.length);
        responseBody(dos, fileBytes);
    }
    private void serveCSSFile(DataOutputStream dos, String cssFilePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(cssFilePath));
        response200CSSHeader(dos, fileBytes.length);
        responseBody(dos, fileBytes);
    }

    private void response200CSSHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }


    private void handleLoginRequest(Map<String, String> params, DataOutputStream dos) throws IOException {
        String userId = params.get("userId");
        String password = params.get("password");
        User user = MemoryUserRepository.getInstance().findUserById(userId);

        if (user != null && user.getPassword().equals(password)) {
            // 로그인 성공
            response302HeaderWithCookie(dos, "/index.html", "logined=true");
        } else {
            // 로그인 실패
            response302Header(dos, "/user/logined_failed.html");
        }
    }


    private void handleSignupRequest(Map<String, String> params, DataOutputStream dos) {
        User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
        MemoryUserRepository.getInstance().addUser(user);
        response302Header(dos, "/index.html");
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String location, String cookie) throws IOException {
        dos.writeBytes("HTTP/1.1 302 Found \r\n");
        dos.writeBytes("Location: " + location + "\r\n");
        dos.writeBytes("Set-Cookie: " + cookie + "; Path=/" + "\r\n");
        dos.writeBytes("\r\n");
    }
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response404Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    // Query String을 파싱하는 메소드
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> parameters = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 1) {
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
        return parameters;
    }

    private boolean isLogined(BufferedReader br) throws IOException {
        while (true) {
            final String line = br.readLine();
            if (line.equals("")) {
                break;
            }
            if (line.startsWith("Cookie")) {
                return line.contains("logined=true");
            }
        }
        return false;
    }
}