package grader.util;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Shared utility for file system operations.
 */
public class FileUtil {

  /**
   * Extracts a ZIP file using native Java (cross-platform).
   * 
   * @param zipPath Absolute or relative path to zip
   * @param destDir Target extraction directory
   * @throws IOException If extraction fails
   */
  public static void unzip(String zipPath, File destDir) throws IOException {
    if (!destDir.exists())
      destDir.mkdirs();

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
      ZipEntry entry;
      byte[] buffer = new byte[4096];

      while ((entry = zis.getNextEntry()) != null) {
        String entryName = entry.getName().replace('\\', '/');
        File outFile = new File(destDir, entryName);

        // Basic Zip-Slip protection
        if (!outFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)
            && !outFile.getCanonicalPath().equals(destDir.getCanonicalPath())) {
          zis.closeEntry();
          continue;
        }

        if (entry.isDirectory()) {
          outFile.mkdirs();
        } else {
          outFile.getParentFile().mkdirs();
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        zis.closeEntry();
      }
    }
  }

  /**
   * Recursively finds the submission root (the folder containing Q1, Q2, etc.).
   */
  public static File findSubmissionRoot(File dir) {
    if (dir == null || !dir.isDirectory())
      return null;

    File[] children = dir.listFiles(File::isDirectory);
    if (children != null) {
      for (File child : children) {
        if (child.getName().startsWith("Q"))
          return dir;
      }
      for (File child : children) {
        File found = findSubmissionRoot(child);
        if (found != null)
          return found;
      }
    }
    return null;
  }

  /**
   * Recursively deletes a directory or file.
   */
  public static void deleteDirectory(File dir) throws IOException {
    if (dir == null || !dir.exists())
      return;
    Path path = dir.toPath();
    try (var stream = Files.walk(path)) {
      stream.sorted(java.util.Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }
}
