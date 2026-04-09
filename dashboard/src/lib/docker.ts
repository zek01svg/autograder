import { exec } from "child_process";
import { promisify } from "util";
import logger from "./pino";

const execAsync = promisify(exec);

/**
 * Checks if the Docker engine is running and accessible.
 * Throws an error with a descriptive message if the engine is offline.
 */
export async function checkDocker(): Promise<void> {
  try {
    // 'docker info' is a standard way to check if the daemon is responsive
    await execAsync("docker info");
    logger.info("[Docker] Engine verified as online.");
  } catch (error) {
    logger.error("[Docker] Engine check failed. Daemon likely offline.");
    throw new Error(
      "Docker engine is not running or accessible. Please start Docker Desktop and ensure it is working correctly."
    );
  }
}
