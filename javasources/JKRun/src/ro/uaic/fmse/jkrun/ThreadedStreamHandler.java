package ro.uaic.fmse.jkrun;

import java.io.*;

public class ThreadedStreamHandler extends Thread {

	InputStream inputStream;
	StringBuilder content = new StringBuilder();

	ThreadedStreamHandler(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public void run() {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line + K.lineSeparator);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				// ignore this one
			}
		}
	}

	/**
	 * @return the content
	 */
	public StringBuilder getContent() {
		return content;
	}
}
