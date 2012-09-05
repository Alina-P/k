package commands;

import java.net.Socket;
import java.util.logging.Logger;

import jfkbits.LispParser;
import jfkbits.LispTokenizer;
import jfkbits.LispParser.Expr;

public class CommandSmtlib extends Command {

	private long ID;
	private String smtlib;

	public CommandSmtlib(String[] args, Socket socket, Logger logger) {
		super(args, socket, logger);
		try {
			ID = Long.parseLong(args[1]);
			smtlib = args[2];
		} catch (NumberFormatException nfe) {
			fail("smt connection aborted: " + nfe.getLocalizedMessage());
		}
	}

	public void run() {
	
		if (smtlib.startsWith("sat") && !smtlib.trim().equals("sat")) {
			smtlib = smtlib.replaceFirst("sat", "");
		}
		if (smtlib.startsWith("unsat") && !smtlib.trim().equals("unsat")) {
			smtlib = smtlib.replaceFirst("unsat", "");
		}
		
		// call corresponding method on file
		try {
			String output = smtlib;

			// parsing
			LispTokenizer lt = new LispTokenizer(output);
			LispParser lp = new LispParser(lt);
			try{
				Expr parsed = lp.parseExpr();
				String out = parsed.getKIF().trim();
				if (out.trim().equals("")){
					//System.out.println("EMPTY: >>" + out + "<<");
				} else output = out;
			}
			catch(Exception e){
				fail("unknown");
			}
			
			// success
			succeed(new String[] { output.toString() });

		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Smtlib parser problem " + ID);
			e.printStackTrace();
		}
	}

}
