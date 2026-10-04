package com.attendance;

import com.attendance.dao.DatabaseManager;
import com.attendance.server.ApiHandler;
import com.attendance.server.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("       QR CODE ATTENDANCE SYSTEM - JAVA MINI PROJECT           ");
        System.out.println("===============================================================");

        // 1. Initialize Database
        System.out.println(">> Initializing relational database & verifying seed data...");
        DatabaseManager.initializeDatabase();

        // 2. Resolve port
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (Exception ignored) {}
        }

        // 3. Locate web static directory
        File webDir = new File("web");
        if (!webDir.exists()) {
            webDir = new File("../web");
        }
        String webDirPath = webDir.getAbsolutePath();
        System.out.println(">> Web resources root: " + webDirPath);

        // 4. Start HTTP Server
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (Exception e) {
            System.out.println(">> Port " + port + " is busy, trying port 8081...");
            try {
                port = 8081;
                server = HttpServer.create(new InetSocketAddress(port), 0);
            } catch (Exception ex) {
                System.err.println(">> Error creating HTTP server: " + ex.getMessage());
                return;
            }
        }

        server.createContext("/api", new ApiHandler());
        server.createContext("/", new StaticFileHandler(webDirPath));
        server.setExecutor(Executors.newFixedThreadPool(12));
        server.start();

        System.out.println("===============================================================");
        System.out.println(">> Server successfully started!");
        System.out.println(">> Local URL:        http://localhost:" + port);
        System.out.println(">> Network / LAN:    http://127.0.0.1:" + port);
        System.out.println("---------------------------------------------------------------");
        System.out.println(">> DEFAULT DEMO CREDENTIALS:");
        System.out.println("   [Admin Portal]:   admin@attendance.edu  |  admin123");
        System.out.println("   [Teacher Portal]: turing@attendance.edu |  teacher123");
        System.out.println("   [Student Portal]: alex@attendance.edu   |  student123");
        System.out.println("===============================================================");
        System.out.println("Press Ctrl+C in terminal to stop server.");
    }
}
