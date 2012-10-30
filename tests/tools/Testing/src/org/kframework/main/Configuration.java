package org.kframework.main;

import java.io.File;

public class Configuration {

	public static String getHomeDir() {
		return HOME_DIR;
	}
	
	public static String HOME_DIR = new File(System.getProperty("user.dir")).getParentFile()
			.getParentFile().getParentFile().getAbsolutePath();
	public static final String FS = System.getProperty("file.separator");
	public static String kompile = HOME_DIR + FS + "dist" + FS + "bin" + FS + "kompile";
	public static String krun = HOME_DIR + FS + "dist" + FS + "bin" + FS + "krun";
	public static final String config = HOME_DIR + FS + "tests" + FS + "config.xml";
	public static final long KOMPILE_ALL_TIMEOUT = 3;
	public static final String JR = "junit-reports";
}
