package commands;

import java.io.EOFException;
import java.net.Socket;
import java.util.logging.Logger;
import resources.Resource;
import resources.ResourceSystem;

public class CommandReadbyte extends Command {


	private long ID;

	public CommandReadbyte(String[] args, Socket socket, Logger logger) { //, Long maudeId) {
		super(args, socket, logger); //, maudeId);

		try {
			ID = Long.parseLong(args[1]);
		} catch (NumberFormatException nfe) {
			fail("read byte operation aborted: " + nfe.getLocalizedMessage());
		}
	}

	public void run() {
		// retrieve file struct
		Resource resource = ResourceSystem.getResource(ID);

		// call corresponding method on file
		Byte ascii = null;
		try {
			ascii = resource.readbyte();

			// success
			succeed(new String[] { ascii.toString() });
		} catch (EOFException eof) {
			fail("EOF");
		} catch (Exception e) {
			fail("Cannot read byte from resource " + ID);
		}
	}
}
