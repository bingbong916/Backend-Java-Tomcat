package webserver;

import controller.*;
import db.MemoryUserRepository;
import db.Repository;
import http.constants.HttpHeader;
import http.request.HttpRequest;
import http.response.HttpResponse;
import model.User;
import model.constants.UserQueryKey;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static http.request.RequestURL.*;

public class RequestHandler implements Runnable{
    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    private final Repository repository;
    private Controller controller = new ForwardController();


    public RequestHandler(Socket connection) {
        this.connection = connection;
        repository = MemoryUserRepository.getInstance();
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            byte[] body = new byte[0];

            // Header 분석
            HttpRequest httpRequest = HttpRequest.from(br);
            HttpResponse httpResponse = new HttpResponse(dos);

            RequestMapper requestMapper = new RequestMapper(httpRequest, httpResponse);
            requestMapper.proceed();

        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

}