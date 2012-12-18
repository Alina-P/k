package org.kframework.utils;

import java.io.*;

public class ResourceExtractor {

	public static void Extract(String resource, File destination) throws IOException {
		BufferedInputStream k2 = new BufferedInputStream(Object.class.getResourceAsStream(resource));
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(destination));

		while (true) {
			int reader = k2.read();
			if (reader >= 0) {
				os.write(reader);
			} else
				break;
		}
		os.close();
	}

	public static void ExtractAllSDF(File basePath) throws IOException {
		basePath.mkdirs();
		Extract("/sdf/Concrete.sdf", new File(basePath.getAbsoluteFile() + "/Concrete.sdf"));
		Extract("/sdf/Common.sdf", new File(basePath.getAbsoluteFile() + "/Common.sdf"));
		Extract("/sdf/KBuiltinsBasic.sdf", new File(basePath.getAbsoluteFile() + "/KBuiltinsBasic.sdf"));
		Extract("/sdf/KTechnique.sdf", new File(basePath.getAbsoluteFile() + "/KTechnique.sdf"));
		Extract("/sdf/Variables.sdf", new File(basePath.getAbsoluteFile() + "/Variables.sdf"));
	}

	public static void ExtractProgramSDF(File basePath) throws IOException {
		basePath.mkdirs();
		Extract("/sdf/Common.sdf", new File(basePath.getAbsoluteFile() + "/Common.sdf"));
		Extract("/sdf/KBuiltinsBasic.sdf", new File(basePath.getAbsoluteFile() + "/KBuiltinsBasic.sdf"));
	}
}
