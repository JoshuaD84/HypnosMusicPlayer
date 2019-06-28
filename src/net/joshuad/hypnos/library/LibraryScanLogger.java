package net.joshuad.hypnos.library;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class LibraryScanLogger {
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private final PrintStream ps = System.out;//new PrintStream(buffer, true, StandardCharsets.UTF_8);

	public synchronized void println(String string) {
		ps.println(string);
	}

	public synchronized String dumpBuffer() {
		String retMe = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
		buffer.reset();
		return retMe;
	}
}
