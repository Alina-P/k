package org.kframework.parser.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.kframework.kil.ASTNode;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.utils.general.GlobalSettings;

import aterm.ATerm;
import aterm.pure.PureFactory;
import aterm.pure.binary.BAFReader;

public class Sglr {

	/**
	 * The main parsing function that accesses the C parser in native way.
	 * The library is loaded only once and it is persistent.
	 * @param tablePath - the path to the parse table. Note that it will be cached.
	 * @param input - the string to parse
	 * @param startSymbol - the start sort
	 * @param location - this is required to annotate the nodes with location information. It can be any string.
	 * @return the ASTNode corresponding to the DAG parse forest returned by the parser.
	 */
	public static ASTNode run_sglri(String tablePath, String startSymbol, String input, String location) {
		tablePath = new File(tablePath).getAbsolutePath();
		// should parse both ways and compare, but JNI way is broken on 64 bits.
		//byte[] parsed = SglrJNI.parseString(tablePath, input, startSymbol, location);
		byte[] parsed = null;
        if (GlobalSettings.isUnixOS()) {
            try {
                byte[] served = SglrServer.parseString(tablePath, input, startSymbol, location);
                parsed = served;
                /*
                if (!Arrays.equals(parsed, served)) {
                    System.err.println("Error, different results from sglr through jni and server");
                    System.exit(1);
                }
                */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(parsed);
			inputStream.read(); // the BAF format starts with a 0 that has to go away first.
			ATerm aterm = new BAFReader(new PureFactory(), inputStream).readFromBinaryFile(false);
			JavaClassesFactory.clearCache();

			return JavaClassesFactory.getTerm(aterm);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
