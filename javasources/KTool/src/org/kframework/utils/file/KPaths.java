package org.kframework.utils.file;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class KPaths {
	public static String windowfyPath(String file) {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			file = file.replaceFirst("([a-zA-Z]):(.*)", "/cygdrive/$1$2");
			file = file.replaceAll("\\\\", "/");
		}
		return file;
	}

	public static boolean isDebugMode() {
		String path = new File(KPaths.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
		return !path.endsWith(".jar");
	}

	public static String JAR_PATH = "lib/java/k3.jar";
	public static String MAUDE_DIR = "lib/maude/binaries";
	public static String MAUDE_LIB_DIR = "/lib/maude/lib";
	public static String VERSION_FILE = "/lib/version.txt";

	/**
	 * Returns the K installation directory
	 * 
	 * @param windowfy
	 *            - if true, then the path will be transformed into /cygdirve/c/... when on windows (just for maude)
	 * @return The path to the K installation
	 */
	public static String getKBase(boolean windowfy) {
		// String env = System.getenv("K_BASE");
		String path = new File(KPaths.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
		if (!path.endsWith(".jar"))
			path = new File(path).getParentFile().getParentFile().getParentFile().getAbsolutePath() + "/dist/" + JAR_PATH;
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			File parent = new File(decodedPath).getParentFile().getParentFile().getParentFile();
			if (windowfy)
				return windowfyPath(parent.getAbsolutePath());
			else
				return parent.getAbsolutePath();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
