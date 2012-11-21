package org.kframework.krun.ioserver.commands;

import org.kframework.krun.ioserver.resources.FileResource;
import org.kframework.krun.ioserver.resources.ResourceSystem;

import java.net.Socket;
import java.util.logging.Logger;

public class CommandWritebyte extends Command {


	private long ID;
	private byte ascii;

	public CommandWritebyte(String[] args, Socket socket, Logger logger) { //, Long maudeId) {
		super(args, socket, logger); //, maudeId);

		try {
			ID = Long.parseLong(args[1]);
			// ascii = Byte.parseByte(args[2]);
			int signedByte = Integer.parseInt(args[2]);
			ascii = (byte)signedByte;
		} catch (NumberFormatException nfe) {
			fail("write operation aborted: " + nfe.getLocalizedMessage());
		}
	}

	public void run() {
		
		// get resource
		FileResource resource = (FileResource)ResourceSystem.getResource(ID);
		
		try {
			resource.writebyte(ascii);
			succeed(new String[] { "success" });
		} catch (Exception e) {
			fail("seek: cannot write " + ascii + " in resource " + ID);
		}
	}

}
