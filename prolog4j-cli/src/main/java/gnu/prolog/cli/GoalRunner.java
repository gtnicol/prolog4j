/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2010       Daniel Thomas
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package gnu.prolog.cli;

import gnu.prolog.PrologHaltException;
import gnu.prolog.Version;
import gnu.prolog.database.PrologTextLoaderError;
import gnu.prolog.io.OperatorSet;
import gnu.prolog.io.ParseException;
import gnu.prolog.io.ReadOptions;
import gnu.prolog.io.TermReader;
import gnu.prolog.io.TermWriter;
import gnu.prolog.io.WriteOptions;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.PrologCode.RC;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * Interactive REPL and goal runner for GNU Prolog for Java.
 *
 * When invoked without arguments, starts an interactive REPL.
 * When invoked with file and goal arguments, runs in legacy goal execution mode.
 *
 * @see #main(String[])
 */
@Command(
	name = "prolog4j",
	description = "Prolog4J - Interactive REPL and Goal Runner",
	mixinStandardHelpOptions = true,
	version = {
		"Prolog4J Goal Runner",
		"(c) Constantine Plotnikov, 1997-1999",
		"(c) Gavin Nicol, 2025, 2026"
	}
)
public class GoalRunner implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(GoalRunner.class);

	@Option(names = {"-o", "--once"}, description = "Get first solution only (goal mode)")
	private boolean once = false;

	@Option(names = {"-t", "--threads"}, description = "Number of concurrent threads (goal mode, default: 1)")
	private int threads = 1;

	@Option(names = {"-i", "--iterations"}, description = "Number of iterations (goal mode, default: 1)")
	private int iterations = 1;

	@Option(names = {"-r", "--repl"}, description = "Force REPL mode even with file argument")
	private boolean forceRepl = false;

	@Parameters(index = "0", arity = "0..1", description = "Prolog file to load")
	private String textToLoad;

	@Parameters(index = "1", arity = "0..1", description = "Goal to run (goal mode)")
	private String goalToRun;

	@Override
	public Integer call() throws Exception {
		System.out.println("GNU Prolog for Java (" + Version.getVersion() + ")");
		System.out.println("(c) Constantine Plotnikov, 1997-1999");
		System.out.println("(c) Gavin Nicol, 2025, 2026");

		// Determine mode: REPL if no args, or --repl flag, or only file provided with --repl
		final boolean replMode = forceRepl || (textToLoad == null && goalToRun == null) || (goalToRun == null && !forceRepl);

		if (replMode && goalToRun == null) {
			return runRepl();
		} else {
			return runGoal();
		}
	}

	/**
	 * Run interactive REPL mode
	 */
	private int runRepl() throws Exception {
		System.out.println("Type ':help' for help, ':quit' to exit\n");

		final Environment env = new Environment();
		final Interpreter interpreter = env.createInterpreter();
		env.runInitialization(interpreter);

		// Load initial file if provided
		if (textToLoad != null) {
			env.ensureLoaded(AtomTerm.get(textToLoad));
			System.out.println("Loaded: " + textToLoad);
			printLoadingErrors(env);
		}

		// Setup JLine for line editing and history
		try (final Terminal terminal = TerminalBuilder.builder().system(true).build()) {
			final LineReader reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.history(new DefaultHistory())
				.build();

			final TermWriter out = new TermWriter(new OutputStreamWriter(System.out));
			final WriteOptions writeOps = new WriteOptions(new OperatorSet());

			// Main REPL loop
			while (true) {
				try {
					final String line = reader.readLine("?- ");

					if (line == null || line.trim().isEmpty()) {
						continue;
					}

					final String trimmed = line.trim();

					// Handle meta-commands
					if (trimmed.startsWith(":")) {
						if (!handleMetaCommand(trimmed, env, interpreter)) {
							return 0; // :quit was called
						}
						continue;
					}

					// Parse and execute query
					executeQuery(trimmed, env, interpreter, out, writeOps, reader);

				} catch (final UserInterruptException e) {
					System.out.println("\nInterrupted");
				} catch (final EndOfFileException e) {
					System.out.println("\nBye.");
					return 0;
				} catch (final Exception e) {
					System.err.println("Error: " + e.getMessage());
					if (java.lang.Boolean.getBoolean("java.debug")) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Execute a Prolog query in REPL mode
	 */
	private void executeQuery(final String query, final Environment env, final Interpreter interpreter,
	                          final TermWriter out, final WriteOptions writeOps, final LineReader reader) {
		try {
			// Add period if not present
			String queryWithPeriod = query.trim();
			if (!queryWithPeriod.endsWith(".")) {
				queryWithPeriod += ".";
			}

			final StringReader rd = new StringReader(queryWithPeriod);
			final TermReader termReader = new TermReader(rd, env);
			final ReadOptions readOps = new ReadOptions(env.getOperatorSet());

			final Term goalTerm = termReader.readTerm(readOps);
			final Interpreter.Goal goal = interpreter.prepareGoal(goalTerm);

			boolean firstSolution = true;
			while (true) {
				final long start = System.currentTimeMillis();
				final RC rc = interpreter.execute(goal);
				final long elapsed = System.currentTimeMillis() - start;

				env.getUserOutput().flushOutput(null);

				switch (rc) {
					case SUCCESS: {
						printBindings(readOps, out, writeOps);
						System.out.println("(" + elapsed + "ms)");

						// Ask for more solutions
						final String response = reader.readLine("More? (y/n) ");
						if (response == null || !response.trim().equalsIgnoreCase("y")) {
							interpreter.stop(goal);
							return;
						}
						firstSolution = false;
						break;
					}
					case SUCCESS_LAST: {
						printBindings(readOps, out, writeOps);
						System.out.println("(" + elapsed + "ms)");
						System.out.println("yes");
						return;
					}
					case FAIL:
						if (firstSolution) {
							System.out.println("no");
						} else {
							System.out.println("no (more solutions)");
						}
						return;
					case HALT:
						env.closeStreams();
						System.out.println("halt");
						throw new PrologHaltException(interpreter.getExitCode());
				}
			}
		} catch (final ParseException e) {
			System.err.println("Parse error: " + e.getMessage());
		} catch (final PrologException e) {
			System.err.println("Prolog error: " + e.getMessage());
		} catch (final IOException e) {
			System.err.println("I/O error: " + e.getMessage());
		} catch (final PrologHaltException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print variable bindings
	 */
	private void printBindings(final ReadOptions readOps, final TermWriter out, final WriteOptions writeOps)
			throws IOException {
		if (readOps.variableNames.isEmpty()) {
			// No variables to show
			return;
		}

		final Iterator<String> vars = readOps.variableNames.keySet().iterator();
		while (vars.hasNext()) {
			final String name = vars.next();
			out.print(name + " = ");
			out.print(writeOps, readOps.variableNames.get(name).dereference());
			if (vars.hasNext()) {
				out.print(", ");
			}
		}
		out.println();
		out.flush();
	}

	/**
	 * Handle meta-commands like :help, :load, :quit
	 * @return false if should exit, true to continue
	 */
	private boolean handleMetaCommand(final String cmd, final Environment env, final Interpreter interpreter) {
		final String[] parts = cmd.substring(1).split("\\s+", 2);
		final String command = parts[0].toLowerCase();
		final String arg = parts.length > 1 ? parts[1] : null;

		switch (command) {
			case "quit":
			case "exit":
			case "q":
				System.out.println("Bye.");
				return false;

			case "help":
			case "h":
			case "?":
				printHelp();
				break;

			case "load":
			case "consult":
			case "l":
				if (arg == null) {
					System.err.println("Usage: :load <filename>");
				} else {
					loadFile(arg, env);
				}
				break;

			case "trace":
				if (arg == null) {
					final boolean active = interpreter.getTracer().isActive();
					System.out.println("Trace: " + (active ? "on" : "off"));
				} else if ("on".equalsIgnoreCase(arg)) {
					interpreter.getTracer().setActive(true);
					System.out.println("Trace: on");
				} else if ("off".equalsIgnoreCase(arg)) {
					interpreter.getTracer().setActive(false);
					System.out.println("Trace: off");
				} else {
					System.err.println("Usage: :trace [on|off]");
				}
				break;

			default:
				System.err.println("Unknown command: " + cmd);
				System.err.println("Type ':help' for available commands");
		}

		return true;
	}

	/**
	 * Load a Prolog file
	 */
	private void loadFile(final String filename, final Environment env) {
		env.ensureLoaded(AtomTerm.get(filename));
		System.out.println("Loaded: " + filename);
		printLoadingErrors(env);
	}

	/**
	 * Print REPL help
	 */
	private void printHelp() {
		System.out.println("Meta-commands:");
		System.out.println("  :help, :h, :?           Show this help");
		System.out.println("  :quit, :exit, :q        Exit the REPL");
		System.out.println("  :load <file>, :l <file> Load a Prolog file");
		System.out.println("  :trace [on|off]         Enable/disable trace mode");
		System.out.println();
		System.out.println("Query execution:");
		System.out.println("  Type a Prolog query at the ?- prompt");
		System.out.println("  Press Ctrl+D to exit");
		System.out.println("  Press Ctrl+C to interrupt current query");
	}

	/**
	 * Print any loading errors
	 */
	private void printLoadingErrors(final Environment env) {
		for (final PrologTextLoaderError error : env.getLoadingErrors()) {
			System.err.println(error);
		}
	}

	/**
	 * Run in legacy goal execution mode
	 */
	private int runGoal() throws Exception {
		System.out.println("Goal runner mode");

		final Environment env = new Environment();
		env.ensureLoaded(AtomTerm.get(textToLoad));

		final Runner[] runners = new Runner[threads];
		for (int j = 0; j < iterations; ++j) {
			for (int i = 0; i < threads; ++i) {
				runners[i] = new Runner("it: " + j + " t:" + i, env, once, goalToRun);
				runners[i].start();
			}
			for (int i = 0; i < threads; ++i) {
				runners[i].join();
				if (runners[i].hasHalted()) {
					return runners[i].getExitCode();
				}
				runners[i] = null;
			}
		}
		return 0;
	}

	public static void main(final String[] args) {
		final int exitCode = new CommandLine(new GoalRunner()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * Thread runner for goal execution mode
	 */
	private static class Runner extends Thread {
		private final Environment env;
		private final boolean once;
		private final String goalToRun;
		private volatile int exitCode = 0;
		private volatile boolean halted = false;

		public Runner(final String name, final Environment environment, final boolean once, final String goalToRun) {
			super(name);
			this.env = environment;
			this.once = once;
			this.goalToRun = goalToRun;
		}

		public boolean hasHalted() {
			return halted;
		}

		public int getExitCode() {
			return exitCode;
		}

		@Override
		public void run() {
			final Interpreter interpreter = env.createInterpreter();
			env.runInitialization(interpreter);
			for (final PrologTextLoaderError element : env.getLoadingErrors()) {
				final PrologTextLoaderError err = element;
				System.err.println(err);
			}
			final LineNumberReader kin = new LineNumberReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
			final StringReader rd = new StringReader(goalToRun);
			final TermReader trd = new TermReader(rd, env);
			final TermWriter out = new TermWriter(new OutputStreamWriter(System.out));
			final ReadOptions rdOps = new ReadOptions(env.getOperatorSet());
			try {
				final Term goalTerm = trd.readTermEof(rdOps);

				Interpreter.Goal goal = interpreter.prepareGoal(goalTerm);
				String response;
				do {
					final long startTime = System.currentTimeMillis();
					final RC rc = interpreter.execute(goal);
					final long stopTime = System.currentTimeMillis();
					env.getUserOutput().flushOutput(null);
					System.out.println("time = " + (stopTime - startTime) + "ms");
					response = "n";
					switch (rc) {
						case SUCCESS: {
							final WriteOptions options = new WriteOptions(new OperatorSet());
							final Iterator<String> ivars = rdOps.variableNames.keySet().iterator();
							while (ivars.hasNext()) {
								final String name = ivars.next();
								out.print(name + " = ");
								out.print(options, (rdOps.variableNames.get(name)).dereference());
								out.print("; ");
							}
							out.println();
							if (once) {
								out.print(rc + ". redo suppressed by command line option \"-once\"");
								return;
							}
							out.print(rc + ". redo (y/n/a)?");
							out.flush();
							response = kin.readLine();

							if ("a".equals(response)) {
								interpreter.stop(goal);
								goal = interpreter.prepareGoal(goalTerm);
							}

							if ("n".equals(response)) {
								return;
							}
							break;
						}
						case SUCCESS_LAST: {
							final WriteOptions options = new WriteOptions(new OperatorSet());
							final Iterator<String> ivars2 = rdOps.variableNames.keySet().iterator();
							while (ivars2.hasNext()) {
								final String name = ivars2.next();
								out.print(name + " = ");
								out.print(options, (rdOps.variableNames.get(name)).dereference());
								out.print("; ");
							}
							out.println();
							out.println(rc);
							out.flush();
							return;
						}
						case FAIL:
							out.println(rc);
							out.flush();
							return;
						case HALT:
							env.closeStreams();
							out.println(rc);
							out.flush();
							throw new PrologHaltException(interpreter.getExitCode());
					}
				} while (true);
			} catch (final PrologHaltException ex) {
				halted = true;
				exitCode = ex.getExitCode();
			} catch (final PrologException | IOException ex) {
				logger.error("Error executing goal", ex);
			}
		}
	}
}
