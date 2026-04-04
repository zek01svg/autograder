package grader.web;

import java.io.IOException;

/**
 * Entry point for the AutoGrader Web UI.
 * Starts an embedded HTTP server with no external dependencies.
 *
 * Usage: java grader.web.WebMain [--port 8080]
 */
public class WebMain {

    public static void main(String[] args) {
        int port = 8080;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }

        try {
            WebServer server = new WebServer(port);
            server.start();
            System.out.println("AutoGrader Web UI running at http://localhost:" + port);
            System.out.println("Press Ctrl+C to stop.");
        } catch (IOException e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            System.exit(1);
        }
    }
}
