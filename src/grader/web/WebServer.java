package grader.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import grader.core.*;
import grader.model.*;
import grader.report.*;
import grader.util.*;

/**
 * Embedded HTTP server for the AutoGrader Web UI.
 * Uses only java built-in com.sun.net.httpserver — zero external deps.
 *
 * Endpoints:
 *   GET  /                  → Serve the frontend SPA
 *   POST /api/upload        → Upload submissions zip + tester files (multipart)
 *   POST /api/grade         → Start the grading pipeline
 *   GET  /api/progress      → SSE stream of grading progress
 *   GET  /api/report        → Serve the generated HTML report
 *   GET  /api/report/csv    → Download the generated CSV
 *   GET  /api/status        → Current grading state (JSON)
 */
public class WebServer {

    private final int port;
    private HttpServer server;

    // --- Shared state ---
    private final AtomicBoolean grading = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>(null);
    private final AtomicReference<String> currentPhase = new AtomicReference<>("idle");
    private final List<String> progressMessages = new CopyOnWriteArrayList<>();
    private final List<OutputStream> sseClients = new CopyOnWriteArrayList<>();

    // Upload dirs
    private static final String UPLOAD_BASE = "web-uploads";
    private static final String UPLOAD_SUBMISSIONS = UPLOAD_BASE + "/submissions";
    private static final String UPLOAD_TESTERS = UPLOAD_BASE + "/testers";
    private static final String UPLOAD_TEMPLATE = UPLOAD_BASE + "/template";
    private static final String WORK_DIR = "web-work";
    private static final String RESULTS_DIR = "web-results";

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.createContext("/", new StaticHandler());
        server.createContext("/api/upload", new UploadHandler());
        server.createContext("/api/grade", new GradeHandler());
        server.createContext("/api/progress", new ProgressSSEHandler());
        server.createContext("/api/report", new ReportHandler());
        server.createContext("/api/report/csv", new CsvHandler());
        server.createContext("/api/status", new StatusHandler());

        server.start();
    }

    // ============================================================
    // SSE Broadcasting
    // ============================================================

    private void broadcast(String event, String data) {
        String msg = "event: " + event + "\ndata: " + data.replace("\n", "\ndata: ") + "\n\n";
        progressMessages.add(data);
        Iterator<OutputStream> it = sseClients.iterator();
        while (it.hasNext()) {
            OutputStream os = it.next();
            try {
                os.write(msg.getBytes());
                os.flush();
            } catch (IOException e) {
                sseClients.remove(os);
            }
        }
    }

    private void broadcastProgress(String message) {
        broadcast("progress", message);
    }

    private void broadcastPhase(String phase) {
        currentPhase.set(phase);
        broadcast("phase", phase);
    }

    private void broadcastComplete(String reportPath) {
        broadcast("complete", reportPath);
    }

    private void broadcastError(String error) {
        lastError.set(error);
        broadcast("error", error);
    }

    // ============================================================
    // Handlers
    // ============================================================

    /** Serves the single-page frontend */
    class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"/".equals(ex.getRequestURI().getPath())
                    && !"/index.html".equals(ex.getRequestURI().getPath())) {
                ex.sendResponseHeaders(404, 0);
                ex.getResponseBody().close();
                return;
            }
            InputStream is = getClass().getResourceAsStream("/web/index.html");
            byte[] html;
            if (is != null) {
                html = is.readAllBytes();
                is.close();
            } else {
                // Fallback: read from file system
                Path filePath = Path.of("src/grader/web/index.html");
                if (!Files.exists(filePath)) {
                    String msg = "Frontend not found";
                    ex.sendResponseHeaders(404, msg.length());
                    ex.getResponseBody().write(msg.getBytes());
                    ex.getResponseBody().close();
                    return;
                }
                html = Files.readAllBytes(filePath);
            }
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, html.length);
            ex.getResponseBody().write(html);
            ex.getResponseBody().close();
        }
    }

    /** Handles multipart file uploads for submissions and testers */
    class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (grading.get()) {
                sendJson(ex, 409, "{\"error\":\"Grading in progress\"}");
                return;
            }

            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("multipart/form-data")) {
                sendJson(ex, 400, "{\"error\":\"Expected multipart/form-data\"}");
                return;
            }

            String boundary = extractBoundary(contentType);
            if (boundary == null) {
                sendJson(ex, 400, "{\"error\":\"Missing boundary\"}");
                return;
            }

            // Clean previous uploads
            deleteDir(new File(UPLOAD_BASE));
            new File(UPLOAD_SUBMISSIONS).mkdirs();
            new File(UPLOAD_TESTERS).mkdirs();
            new File(UPLOAD_TEMPLATE).mkdirs();

            byte[] body = ex.getRequestBody().readAllBytes();
            List<MultipartFile> files = parseMultipart(body, boundary);

            int submissionCount = 0;
            int testerCount = 0;
            int templateCount = 0;
            for (MultipartFile mf : files) {
                if ("submissions".equals(mf.fieldName)) {
                    Path dest = Path.of(UPLOAD_SUBMISSIONS, sanitizeFilename(mf.filename));
                    Files.write(dest, mf.data);
                    submissionCount++;
                } else if ("testers".equals(mf.fieldName)) {
                    Path dest = Path.of(UPLOAD_TESTERS, sanitizeFilename(mf.filename));
                    Files.write(dest, mf.data);
                    testerCount++;
                } else if ("template".equals(mf.fieldName)) {
                    Path dest = Path.of(UPLOAD_TEMPLATE, sanitizeFilename(mf.filename));
                    Files.write(dest, mf.data);
                    templateCount++;
                }
            }

            // Extract the tester zip to get .java files
            if (testerCount > 0) {
                File testerUploadDir = new File(UPLOAD_TESTERS);
                File[] testerZips = testerUploadDir.listFiles((d, n) -> n.toLowerCase().endsWith(".zip"));
                if (testerZips != null && testerZips.length > 0) {
                    File extractTo = new File(UPLOAD_TESTERS, "extracted");
                    FileUtil.unzip(testerZips[0].getAbsolutePath(), extractTo);
                    // Find all *Tester.java files recursively and move to UPLOAD_TESTERS root
                    List<File> javaFiles = findJavaFilesRecursive(extractTo);
                    for (File jf : javaFiles) {
                        Files.move(jf.toPath(),
                                Path.of(UPLOAD_TESTERS, jf.getName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    // Clean up zip and extracted dir
                    for (File z : testerZips) z.delete();
                    deleteDir(extractTo);
                }
            }

            // Extract the template zip to get the folder structure
            if (templateCount > 0) {
                File templateUploadDir = new File(UPLOAD_TEMPLATE);
                File[] templateZips = templateUploadDir.listFiles((d, n) -> n.toLowerCase().endsWith(".zip"));
                if (templateZips != null && templateZips.length > 0) {
                    File extractTo = new File(UPLOAD_TEMPLATE, "extracted");
                    FileUtil.unzip(templateZips[0].getAbsolutePath(), extractTo);
                    // Find the root containing Q* folders
                    File root = FileUtil.findSubmissionRoot(extractTo);
                    if (root != null) {
                        File finalTemplate = new File(UPLOAD_BASE, "template-final");
                        finalTemplate.mkdirs();
                        for (File f : root.listFiles()) {
                            if (f.isDirectory()) {
                                f.renameTo(new File(finalTemplate, f.getName()));
                            } else {
                                Files.move(f.toPath(), finalTemplate.toPath().resolve(f.getName()),
                                        StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        deleteDir(templateUploadDir);
                        finalTemplate.renameTo(templateUploadDir);
                    }
                }
            }

            String json = String.format(
                    "{\"submissions\":%d,\"testers\":%d,\"template\":%d}",
                    submissionCount, testerCount, templateCount);
            sendJson(ex, 200, json);
        }
    }

    /** Triggers the grading pipeline in a background thread */
    class GradeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (grading.get()) {
                sendJson(ex, 409, "{\"error\":\"Grading already in progress\"}");
                return;
            }

            File subDir = new File(UPLOAD_SUBMISSIONS);
            File testDir = new File(UPLOAD_TESTERS);
            File templateDir = new File(UPLOAD_TEMPLATE);

            if (!subDir.exists() || subDir.listFiles() == null || subDir.listFiles().length == 0) {
                sendJson(ex, 400, "{\"error\":\"No submissions uploaded\"}");
                return;
            }
            if (!testDir.exists() || testDir.listFiles() == null || testDir.listFiles().length == 0) {
                sendJson(ex, 400, "{\"error\":\"No tester files uploaded\"}");
                return;
            }
            if (!templateDir.exists() || templateDir.listFiles() == null || templateDir.listFiles().length == 0) {
                sendJson(ex, 400, "{\"error\":\"No exam template uploaded\"}");
                return;
            }

            // Start grading in background
            grading.set(true);
            lastError.set(null);
            progressMessages.clear();
            currentPhase.set("starting");

            Thread gradeThread = new Thread(() -> runGradingPipeline());
            gradeThread.setDaemon(true);
            gradeThread.start();

            sendJson(ex, 200, "{\"status\":\"started\"}");
        }
    }

    /** SSE endpoint for real-time progress */
    class ProgressSSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().set("Content-Type", "text/event-stream");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("Connection", "keep-alive");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, 0);

            OutputStream os = ex.getResponseBody();
            sseClients.add(os);

            // Send current state
            String initMsg = "event: phase\ndata: " + currentPhase.get() + "\n\n";
            os.write(initMsg.getBytes());
            os.flush();

            // Keep connection open — will be closed when client disconnects
            // or when we remove it from sseClients
        }
    }

    /** Serves the HTML grading report */
    class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            Path reportPath = Path.of(RESULTS_DIR, "report.html");
            if (!Files.exists(reportPath)) {
                sendJson(ex, 404, "{\"error\":\"Report not generated yet\"}");
                return;
            }
            byte[] html = Files.readAllBytes(reportPath);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, html.length);
            ex.getResponseBody().write(html);
            ex.getResponseBody().close();
        }
    }

    /** Serves the CSV results for download */
    class CsvHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            Path csvPath = Path.of(RESULTS_DIR, "results.csv");
            if (!Files.exists(csvPath)) {
                sendJson(ex, 404, "{\"error\":\"CSV not generated yet\"}");
                return;
            }
            byte[] csv = Files.readAllBytes(csvPath);
            ex.getResponseHeaders().set("Content-Type", "text/csv");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=results.csv");
            ex.sendResponseHeaders(200, csv.length);
            ex.getResponseBody().write(csv);
            ex.getResponseBody().close();
        }
    }

    /** Returns current grading status as JSON */
    class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String json = String.format(
                    "{\"grading\":%s,\"phase\":\"%s\",\"error\":%s}",
                    grading.get(),
                    currentPhase.get(),
                    lastError.get() != null ? "\"" + escapeJson(lastError.get()) + "\"" : "null");
            sendJson(ex, 200, json);
        }
    }

    // ============================================================
    // Grading Pipeline (runs in background thread)
    // ============================================================

    private void runGradingPipeline() {
        try {
            // Clean work/results dirs
            deleteDir(new File(WORK_DIR));
            deleteDir(new File(RESULTS_DIR));
            new File(WORK_DIR).mkdirs();
            new File(RESULTS_DIR).mkdirs();

            // Load config
            Properties config = new Properties();
            try (InputStream is = new FileInputStream("config.properties")) {
                config.load(is);
            } catch (IOException e) {
                broadcastProgress("WARNING: config.properties not found, using defaults.");
            }

            String submissionsDir = UPLOAD_SUBMISSIONS;
            String testersDir = UPLOAD_TESTERS;
            String templateDir = UPLOAD_TEMPLATE;

            // 1. VALIDATE
            broadcastPhase("validating");
            broadcastProgress("Scanning submissions...");

            // Handle single-zip-of-zips: if only one zip is uploaded, check if it
            // contains student zips inside and extract them
            File subDir = new File(submissionsDir);
            File[] initialZips = subDir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
            if (initialZips != null && initialZips.length == 1) {
                broadcastProgress("Single zip detected — checking for nested student zips...");
                File tempExtract = new File(WORK_DIR, "_bulk_extract");
                tempExtract.mkdirs();
                try {
                    FileUtil.unzip(initialZips[0].getAbsolutePath(), tempExtract);
                    // Find any .zip files inside
                    List<File> nestedZips = findZipsRecursive(tempExtract);
                    if (!nestedZips.isEmpty()) {
                        broadcastProgress("Found " + nestedZips.size() + " student zip(s) inside.");
                        // Move nested zips to submissions dir
                        initialZips[0].delete();
                        for (File nz : nestedZips) {
                            Files.move(nz.toPath(),
                                    Path.of(submissionsDir, nz.getName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException e) {
                    broadcastProgress("Could not extract bulk zip, treating as single submission.");
                }
                deleteDir(tempExtract);
            }

            File[] zipFiles = subDir.listFiles((d, name) ->
                    name.toLowerCase().endsWith(".zip") && !name.startsWith("._"));
            if (zipFiles == null || zipFiles.length == 0) {
                broadcastError("No .zip files found in submissions.");
                grading.set(false);
                return;
            }

            // Load template for structural validation
            Validator validator;
            try {
                validator = new Validator(templateDir, true);
                broadcastProgress("Template loaded from: " + templateDir);
            } catch (IOException e) {
                broadcastError("Could not load template: " + e.getMessage());
                grading.set(false);
                return;
            }

            List<File> validZips = new ArrayList<>();
            int invalidCount = 0;
            for (File zip : zipFiles) {
                ValidationResult vr = validator.validateZipSubmission(zip.getAbsolutePath(), WORK_DIR);
                validZips.add(zip);
                if (!vr.isOk()) {
                    invalidCount++;
                    broadcastProgress("⚠ " + vr.getStudentId() + ": " + String.join("; ", vr.getAnomalies()));
                } else {
                    broadcastProgress("✓ " + vr.getStudentId() + ": valid");
                }
            }
            broadcastProgress(String.format("Validation complete: %d valid, %d with issues",
                    zipFiles.length - invalidCount, invalidCount));

            // 2. COMPILE & EXECUTE
            broadcastPhase("grading");
            broadcastProgress("Starting compilation and execution...");

            Runner runner = new Runner(config);

            if (!runner.isDockerAvailable()) {
                broadcastError("Docker is not running. Please start Docker Desktop.");
                runner.shutdown();
                grading.set(false);
                return;
            }

            Grader grader = new Grader();
            ArrayList<GradeResult> allResults = new ArrayList<>();
            String[][] testCases = discoverTestCases(config, templateDir, testersDir);

            if (testCases.length == 0) {
                broadcastError("No test cases found. Check template and tester files.");
                runner.shutdown();
                grading.set(false);
                return;
            }

            broadcastProgress("Found " + testCases.length + " test case(s).");

            int total = validZips.size();
            int completed = 0;
            for (File zip : validZips) {
                String studentId = Validator.deriveStudentIdFromZip(zip.getName());
                broadcastProgress("Grading " + studentId + " (" + (completed + 1) + "/" + total + ")...");

                GradeResult result = processStudent(zip, testCases, runner, grader, templateDir,
                        testersDir, config);
                allResults.add(result);
                completed++;

                // Send score update
                broadcastProgress(String.format("  %s → %.1f pts", studentId, result.getTotalScore()));
                broadcast("count", completed + "/" + total);
            }

            runner.shutdown();
            broadcastProgress("All submissions graded.");

            // 3. REPORT
            broadcastPhase("reporting");
            broadcastProgress("Generating report...");

            String[] questionKeys = getQuestionKeys(testCases);
            Reporter reporter = new Reporter(questionKeys);

            String reportPath = RESULTS_DIR + "/report.html";
            reporter.writeHtmlReport(reportPath, allResults);
            broadcastProgress("HTML report generated.");

            // CSV
            String csvPath = RESULTS_DIR + "/results.csv";
            // Try to find a scoresheet in uploads, otherwise generate bare CSV
            String scoresheetPath = config.getProperty("path.scoresheet", "scoresheet.csv");
            try {
                reporter.writeCsv(scoresheetPath, csvPath, allResults);
                broadcastProgress("CSV results generated.");
            } catch (IOException e) {
                broadcastProgress("CSV skipped (no scoresheet template): " + e.getMessage());
            }

            broadcastPhase("complete");
            broadcastComplete("/api/report");

        } catch (Exception e) {
            broadcastError("Pipeline error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            grading.set(false);
        }
    }

    // ============================================================
    // Grading helpers (adapted from GradingPipeline)
    // ============================================================

    private String[][] discoverTestCases(Properties config, String templateDir, String testersDir) {
        // Discover from testers directory
        Map<String, String> testerKeyByLower = new HashMap<>();
        Set<String> testerKeys = new HashSet<>();
        File testersRoot = new File(testersDir);
        File[] testerFiles = testersRoot.listFiles((dir, name) -> name.endsWith("Tester.java"));
        if (testerFiles != null) {
            for (File f : testerFiles) {
                String name = f.getName();
                String key = name.substring(0, name.length() - "Tester.java".length());
                testerKeys.add(key);
                testerKeyByLower.putIfAbsent(key.toLowerCase(), key);
            }
        }

        List<String[]> list = new ArrayList<>();
        Set<String> added = new HashSet<>();
        File templateRoot = new File(templateDir);
        File[] qFolders = templateRoot.listFiles((dir, name) -> name.matches("Q[0-9]+.*"));
        if (qFolders != null) {
            Arrays.sort(qFolders, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File folder : qFolders) {
                if (!folder.isDirectory()) continue;
                File[] javaFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".java"));
                if (javaFiles == null) continue;
                Arrays.sort(javaFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File file : javaFiles) {
                    String fileName = file.getName();
                    String base = fileName.substring(0, fileName.length() - 5);
                    String testerKey = testerKeyByLower.get(base.toLowerCase());
                    if (testerKey != null) {
                        String key = folder.getName() + ":" + testerKey;
                        if (added.add(key)) {
                            list.add(new String[] { folder.getName(), testerKey });
                        }
                    }
                }
            }
        }

        if (!list.isEmpty()) return list.toArray(new String[0][0]);

        // Fallback from tester names
        if (!testerKeys.isEmpty()) {
            List<String[]> fallback = new ArrayList<>();
            for (String key : testerKeys) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("^Q(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(key);
                if (m.find()) {
                    fallback.add(new String[] { "Q" + m.group(1), key });
                }
            }
            fallback.sort((a, b) -> a[0].compareToIgnoreCase(b[0]) != 0
                    ? a[0].compareToIgnoreCase(b[0])
                    : a[1].compareToIgnoreCase(b[1]));
            return fallback.toArray(new String[0][0]);
        }

        return new String[0][0];
    }

    private String[] getQuestionKeys(String[][] testCases) {
        String[] keys = new String[testCases.length];
        for (int i = 0; i < testCases.length; i++) {
            keys[i] = testCases[i][1];
        }
        return keys;
    }

    private GradeResult processStudent(File zip, String[][] testCases, Runner runner,
            Grader grader, String templateDir, String testersDir, Properties config) {
        String studentId = Validator.deriveStudentIdFromZip(zip.getName());
        String studentExtractDir = WORK_DIR + "/" + studentId;
        GradeResult result = new GradeResult(studentId, true);

        try {
            Validator studentValidator = new Validator(templateDir, true);
            ValidationResult vr = studentValidator.validateZipSubmission(zip.getAbsolutePath(), WORK_DIR);
            for (String a : vr.getAnomalies()) {
                result.addAnomaly("Validation: " + a);
            }
            FileUtil.unzip(zip.getAbsolutePath(), new File(studentExtractDir));

            File submissionRoot = FileUtil.findSubmissionRoot(new File(studentExtractDir));
            if (submissionRoot == null) {
                result.addAnomaly("Structure: Question folders not found.");
                return result;
            }

            for (String[] tc : testCases) {
                String folder = tc[0];
                String questionKey = tc[1];
                String testerName = questionKey + "Tester";

                Path submissionPath = submissionRoot.toPath().resolve(folder);
                Path testerSource = Path.of(testersDir, testerName + ".java");
                Path testerTarget = submissionPath.resolve(testerName + ".java");

                try {
                    Files.copy(testerSource, testerTarget, StandardCopyOption.REPLACE_EXISTING);
                    String safeFolder = folder.replaceAll("['\";|&]", "");
                    String safeTesterName = testerName.replaceAll("['\";|&]", "");

                    String cmd = String.format("cd \"%s\" && javac *.java && java -cp . %s",
                            safeFolder, safeTesterName);
                    Runner.RunOutput runResult = runner.compileAndRun(
                            submissionRoot.getAbsolutePath(), studentId, cmd);

                    if (runResult.dockerError) {
                        result.addAnomaly("Docker: Failed to run container.");
                    }
                    if (runResult.timedOut) {
                        result.addAnomaly("Timeout: " + questionKey);
                    }
                    result.setQuestionScore(questionKey, grader.parseScoreFromOutput(runResult.output));
                } catch (IOException e) {
                    result.setQuestionScore(questionKey, 0.0);
                    result.addAnomaly("Tester Setup: Failed to copy " + testerName);
                }
            }
        } catch (Exception e) {
            result.addAnomaly("Pipeline Error: " + e.getMessage());
        }
        return result;
    }

    // ============================================================
    // Multipart parser (minimal, no external deps)
    // ============================================================

    /** Recursively find .zip files inside a directory, skipping macOS metadata */
    private List<File> findZipsRecursive(File dir) {
        List<File> zips = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return zips;
        for (File f : files) {
            String name = f.getName();
            // Skip macOS resource forks and metadata
            if (name.startsWith("._") || name.equals("__MACOSX")) continue;
            if (f.isDirectory()) {
                zips.addAll(findZipsRecursive(f));
            } else if (name.toLowerCase().endsWith(".zip")) {
                zips.add(f);
            }
        }
        return zips;
    }

    /** Recursively find *Tester.java files inside a directory, skipping macOS metadata */
    private List<File> findJavaFilesRecursive(File dir) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return javaFiles;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith("._") || name.equals("__MACOSX")) continue;
            if (f.isDirectory()) {
                javaFiles.addAll(findJavaFilesRecursive(f));
            } else if (name.endsWith("Tester.java")) {
                javaFiles.add(f);
            }
        }
        return javaFiles;
    }

    static class MultipartFile {
        String fieldName;
        String filename;
        byte[] data;

        MultipartFile(String fieldName, String filename, byte[] data) {
            this.fieldName = fieldName;
            this.filename = filename;
            this.data = data;
        }
    }

    private String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                String b = part.substring("boundary=".length());
                if (b.startsWith("\"") && b.endsWith("\"")) {
                    b = b.substring(1, b.length() - 1);
                }
                return b;
            }
        }
        return null;
    }

    private List<MultipartFile> parseMultipart(byte[] body, String boundary) {
        List<MultipartFile> files = new ArrayList<>();
        byte[] delimiter = ("--" + boundary).getBytes();
        byte[] endDelimiter = ("--" + boundary + "--").getBytes();

        List<Integer> boundaryPositions = new ArrayList<>();
        for (int i = 0; i <= body.length - delimiter.length; i++) {
            boolean match = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (body[i + j] != delimiter[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                boundaryPositions.add(i);
            }
        }

        for (int idx = 0; idx < boundaryPositions.size() - 1; idx++) {
            int start = boundaryPositions.get(idx) + delimiter.length;
            int end = boundaryPositions.get(idx + 1);

            // Skip \r\n after boundary
            if (start < body.length && body[start] == '\r') start++;
            if (start < body.length && body[start] == '\n') start++;

            // Find header/body separator (\r\n\r\n)
            int headerEnd = -1;
            for (int i = start; i < end - 3; i++) {
                if (body[i] == '\r' && body[i + 1] == '\n' && body[i + 2] == '\r' && body[i + 3] == '\n') {
                    headerEnd = i;
                    break;
                }
            }
            if (headerEnd == -1) continue;

            String headers = new String(body, start, headerEnd - start);
            int dataStart = headerEnd + 4;
            int dataEnd = end;

            // Trim trailing \r\n before next boundary
            if (dataEnd >= 2 && body[dataEnd - 1] == '\n' && body[dataEnd - 2] == '\r') {
                dataEnd -= 2;
            }

            String fieldName = extractHeaderValue(headers, "name");
            String filename = extractHeaderValue(headers, "filename");

            if (fieldName != null && filename != null && !filename.isEmpty()) {
                byte[] data = new byte[dataEnd - dataStart];
                System.arraycopy(body, dataStart, data, 0, data.length);
                files.add(new MultipartFile(fieldName, filename, data));
            }
        }
        return files;
    }

    private String extractHeaderValue(String headers, String key) {
        String search = key + "=\"";
        int idx = headers.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = headers.indexOf("\"", start);
        if (end == -1) return null;
        return headers.substring(start, end);
    }

    // ============================================================
    // Utilities
    // ============================================================

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private static String sanitizeFilename(String name) {
        // Prevent path traversal
        return Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        try {
            FileUtil.deleteDirectory(dir);
        } catch (IOException e) {
            // Best effort
        }
    }
}
