package org.kframework.main;

import java.util.Stack;


public class Configuration {

	public static Stack<String> home = new Stack<String>();
	public static String HOME_DIR = home.pop();
	public static final String FS = System.getProperty("file.separator");
	public static String kompile = HOME_DIR + FS + "dist" + FS + "bin" + FS + "kompile";
	public static String krun = HOME_DIR + FS + "dist" + FS + "bin" + FS + "krun";
	public static final String config = HOME_DIR + FS + "tests" + FS + "config.xml";
	public static final long KOMPILE_ALL_TIMEOUT = 3;
	public static final String JR = "junit-reports";
}
