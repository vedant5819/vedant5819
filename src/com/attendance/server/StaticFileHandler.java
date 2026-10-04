package com.attendance.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
    private final String baseDir;

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, "Method Not Allowed".getBytes(), "text/plain");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath.equals("/") || rawPath.isEmpty()) {
            rawPath = "/index.html";
        }

        // Security check against directory traversal
        if (rawPath.contains("..")) {
            sendResponse(exchange, 403, "Forbidden".getBytes(), "text/plain");
            return;
        }

        Path filePath = Paths.get(baseDir, rawPath.substring(1));
        File file = filePath.toFile();

        if (!file.exists() || file.isDirectory()) {
            // Check if adding .html helps (e.g. /student -> /student.html)
            Path altPath = Paths.get(baseDir, rawPath.substring(1) + ".html");
            if (altPath.toFile().exists() && !altPath.toFile().isDirectory()) {
                file = altPath.toFile();
            } else {
                byte[] notFound = ("<h1>404 Not Found</h1><p>The requested file " + rawPath + " was not found on this server.</p>").getBytes();
                sendResponse(exchange, 404, notFound, "text/html");
                return;
            }
        }

        String contentType = probeContentType(file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        sendResponse(exchange, 200, bytes, contentType);
    }

    private String probeContentType(String filename) {
        filename = filename.toLowerCase();
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".ico")) return "image/x-icon";
        if (filename.endsWith(".woff2")) return "font/woff2";
        if (filename.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] body, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
