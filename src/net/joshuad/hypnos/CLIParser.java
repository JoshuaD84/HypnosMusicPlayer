package net.joshuad.hypnos;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIParser {
	private static final Logger LOGGER = Logger.getLogger(CLIParser.class.getName());
	
	// REFACTOR: Combine these into an Enum with the things in SingleInstanceController and the strings in constructor below.
	private static final String HELP = "help";
	private static final String NEXT = "next";
	private static final String PREVIOUS = "previous";
	private static final String PAUSE = "pause";
	private static final String PLAY = "play";
	private static final String TOGGLE_PAUSE = "play-pause";
	private static final String STOP = "stop";
	private static final String TOGGLE_MINIMIZED = "toggle-window";
	private static final String VOLUME_DOWN = "volume-down";
	private static final String VOLUME_UP = "volume-up";
	private static final String SEEK_BACK = "seek-back";
	private static final String SEEK_FORWARD = "seek-forward";
	private static final String BASE_DIR = "base-dir";

	CommandLineParser parser;
	Options options;
	Options helpOptions;

	public CLIParser() {
		options = new Options();
		options.addOption(null, HELP, false, "Print this message");
		options.addOption(null, NEXT, false, "Jump to next track");
		options.addOption(null, PREVIOUS, false, "Jump to previous track");
		options.addOption(null, PAUSE, false, "Pause Playback");
		options.addOption(null, PLAY, false, "Start Playback");
		options.addOption(null, TOGGLE_PAUSE, false, "Toggle play/pause mode");
		options.addOption(null, STOP, false, "Stop playback");
		options.addOption(null, TOGGLE_MINIMIZED, false, "Toggle the minimized state");
		options.addOption(null, VOLUME_DOWN, false, "Turn down the volume");
		options.addOption(null, VOLUME_UP, false, "Turn up the volume");
		options.addOption(null, SEEK_BACK, false, "Seek back 5 seconds");
		options.addOption(null, SEEK_FORWARD, false, "Seek forward 5 seconds");
		options.addOption(null, BASE_DIR, true, "Define the base directory from which to interpret relative file arguments");
		parser = new DefaultParser();
		helpOptions = new Options();
		for (Option option : options.getOptions()) {
			if (!option.getLongOpt().equals(BASE_DIR)) {
				helpOptions.addOption(option);
			}
		}
	}

	public ArrayList<SocketCommand> parseCommands(String[] args) {
		ArrayList<SocketCommand> retMe = new ArrayList<SocketCommand>();
		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption(HELP)) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("hypnos <options>", helpOptions);
				System.exit(0);
			}
			if (line.hasOption(PLAY)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.PLAY));
			}
			if (line.hasOption(NEXT)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.NEXT));
			}
			if (line.hasOption(PREVIOUS)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.PREVIOUS));
			}
			if (line.hasOption(TOGGLE_PAUSE)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.TOGGLE_PAUSE));
			}
			if (line.hasOption(PAUSE)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.PAUSE));
			}
			if (line.hasOption(STOP)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.STOP));
			}
			if (line.hasOption(TOGGLE_MINIMIZED)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.TOGGLE_MINIMIZED));
			}
			if (line.hasOption(VOLUME_DOWN)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.VOLUME_DOWN));
			}
			if (line.hasOption(VOLUME_UP)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.VOLUME_UP));
			}
			if (line.hasOption(SEEK_BACK)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.SEEK_BACK));
			}
			if (line.hasOption(SEEK_FORWARD)) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.CONTROL, SocketCommand.SEEK_FORWARD));
			}
			Path baseDir = Paths.get(System.getProperty("user.dir"));

			if (line.hasOption(BASE_DIR)) {
				baseDir = Paths.get(line.getOptionValue(BASE_DIR));
			}
			ArrayList<File> filesToLoad = new ArrayList<File>();
			for (String leftOverArgument : line.getArgList()) {
				Path argumentPath = Paths.get(leftOverArgument).normalize();
				if (!argumentPath.isAbsolute()) {
					argumentPath = baseDir.resolve(argumentPath).toAbsolutePath().normalize();
				}
				filesToLoad.add(argumentPath.toFile());
			}
			if (filesToLoad.size() > 0) {
				retMe.add(new SocketCommand(SocketCommand.CommandType.SET_TRACKS, filesToLoad));
			}
		} catch (ParseException e) {
			LOGGER.log(Level.WARNING, "Unable to parse command line arguments. ", e);
		}
		return retMe;
	}
}
