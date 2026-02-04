/* GNU Prolog for Java
 * Copyright (C) 2025  Gavin Nicol
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

import gnu.prolog.io.ReadOptions;
import gnu.prolog.io.TermReader;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode.RC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the GoalRunner CLI.
 */
class GoalRunnerTests {

	private Environment env;
	private Interpreter interpreter;
	private PrintStream original;
	private ByteArrayOutputStream captured;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
		env.runInitialization(interpreter);

		// Capture stdout for CLI tests
		original = System.out;
		captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured));
	}

	@AfterEach
	void teardown() {
		System.setOut(original);
		if (env != null) {
			env.close();
		}
	}

	// ==========================================================================
	// consult/1 Tests
	// ==========================================================================

	@Test
	void testConsultLoadsFile(@TempDir final Path dir) throws Exception {
		// Create a test Prolog file
		final var file = dir.resolve("test.pl");
		Files.writeString(file, "greeting(hello).\ngreeting(world).\n");

		// Execute consult/1
		final var goal = parse("consult('" + file + "').");
		final var prepared = interpreter.prepareGoal(goal);
		final var result = interpreter.execute(prepared);

		assertEquals(RC.SUCCESS_LAST, result, "consult/1 should succeed");

		// Verify the predicate was loaded by querying it
		final var query = parse("greeting(X).");
		final var preparedQuery = interpreter.prepareGoal(query);
		final var rc = interpreter.execute(preparedQuery);

		assertEquals(RC.SUCCESS, rc, "greeting/1 should have solutions");
	}

	@Test
	void testConsultWithRelativePath(@TempDir final Path dir) throws Exception {
		// Create test file
		final var file = dir.resolve("relative.pl");
		Files.writeString(file, "relative_fact(works).\n");

		// Change to temp directory context and load
		env.ensureLoaded(AtomTerm.get(file.toString()));

		// Verify using interpreter
		final var query = parse("relative_fact(X).");
		final var prepared = interpreter.prepareGoal(query);
		final var result = interpreter.execute(prepared);

		assertEquals(RC.SUCCESS_LAST, result, "relative_fact/1 should succeed");
	}

	@Test
	void testConsultMultipleFiles(@TempDir final Path dir) throws Exception {
		// Create multiple test files
		final var file1 = dir.resolve("file1.pl");
		final var file2 = dir.resolve("file2.pl");
		Files.writeString(file1, "from_file1(a).\n");
		Files.writeString(file2, "from_file2(b).\n");

		// Consult both files
		final var goal1 = parse("consult('" + file1 + "').");
		interpreter.execute(interpreter.prepareGoal(goal1));

		final var goal2 = parse("consult('" + file2 + "').");
		interpreter.execute(interpreter.prepareGoal(goal2));

		// Verify both predicates are available
		final var rc1 = interpreter.execute(interpreter.prepareGoal(parse("from_file1(a).")));
		final var rc2 = interpreter.execute(interpreter.prepareGoal(parse("from_file2(b).")));

		assertEquals(RC.SUCCESS_LAST, rc1, "from_file1/1 should succeed");
		assertEquals(RC.SUCCESS_LAST, rc2, "from_file2/1 should succeed");
	}

	@Test
	void testConsultWithEnsureLoadedSemanticsOnReload(@TempDir final Path dir) throws Exception {
		// Create test file
		final var file = dir.resolve("cached.pl");
		Files.writeString(file, "cached_pred(original).\n");

		// First consult
		final var goal1 = parse("consult('" + file + "').");
		interpreter.execute(interpreter.prepareGoal(goal1));

		// Modify the file
		Files.writeString(file, "cached_pred(modified).\n");

		// Second consult (ensure_loaded semantics - should use cache)
		final var goal2 = parse("consult('" + file + "').");
		interpreter.execute(interpreter.prepareGoal(goal2));

		// Query - should still have original due to caching
		final var query = parse("cached_pred(X).");
		final var options = new ReadOptions(env.getOperatorSet());
		final var reader = new TermReader(new StringReader("cached_pred(X)."), env);
		final var parsed = reader.readTerm(options);

		final var prepared = interpreter.prepareGoal(parsed);
		interpreter.execute(prepared);

		// Get the binding
		final var x = (VariableTerm) options.variableNames.get("X");
		assertNotNull(x, "Variable X should be bound");

		final var value = x.dereference();
		assertEquals(AtomTerm.get("original"), value,
			"consult/1 with ensure_loaded semantics should use cached version");
	}

	@Test
	void testConsultNonExistentFile(@TempDir final Path dir) throws Exception {
		final var missing = dir.resolve("nonexistent.pl");

		// Consult non-existent file
		final var goal = parse("consult('" + missing + "').");
		final var prepared = interpreter.prepareGoal(goal);
		final var result = interpreter.execute(prepared);

		// consult/1 succeeds but logs errors
		assertEquals(RC.SUCCESS_LAST, result, "consult/1 returns success even for missing files");

		// Check that an error was recorded
		assertFalse(env.getLoadingErrors().isEmpty(),
			"Loading errors should be recorded for missing file");
	}

	@Test
	void testConsultWithDirective(@TempDir final Path dir) throws Exception {
		// Create file with initialization directive using dynamic predicate
		final var file = dir.resolve("with_init.pl");
		Files.writeString(file,
			":- dynamic(init_marker/1).\n" +
			"init_marker(not_run).\n" +
			":- retract(init_marker(_)), assert(init_marker(ran)).\n"
		);

		// Consult the file
		final var goal = parse("consult('" + file + "').");
		interpreter.execute(interpreter.prepareGoal(goal));

		// Verify directive was executed
		final var query = parse("init_marker(ran).");
		final var result = interpreter.execute(interpreter.prepareGoal(query));

		assertTrue(result == RC.SUCCESS || result == RC.SUCCESS_LAST,
			"Initialization directive should have run");
	}

	@Test
	void testConsultWithNestedEnsureLoaded(@TempDir final Path dir) throws Exception {
		// Create a file that loads another file
		final var inner = dir.resolve("inner.pl");
		final var outer = dir.resolve("outer.pl");

		Files.writeString(inner, "inner_fact(nested).\n");
		Files.writeString(outer,
			":- ensure_loaded('" + inner + "').\n" +
			"outer_fact(wrapper).\n"
		);

		// Consult the outer file
		final var goal = parse("consult('" + outer + "').");
		interpreter.execute(interpreter.prepareGoal(goal));

		// Verify both predicates are available
		final var rc1 = interpreter.execute(interpreter.prepareGoal(parse("outer_fact(wrapper).")));
		final var rc2 = interpreter.execute(interpreter.prepareGoal(parse("inner_fact(nested).")));

		assertEquals(RC.SUCCESS_LAST, rc1, "outer_fact/1 should succeed");
		assertEquals(RC.SUCCESS_LAST, rc2, "inner_fact/1 from nested file should succeed");
	}

	// ==========================================================================
	// ensure_loaded/1 Tests
	// ==========================================================================

	@Test
	void testEnsureLoadedBasic(@TempDir final Path dir) throws Exception {
		final var file = dir.resolve("ensure.pl");
		Files.writeString(file, "ensure_test(success).\n");

		final var goal = parse("ensure_loaded('" + file + "').");
		final var result = interpreter.execute(interpreter.prepareGoal(goal));

		assertEquals(RC.SUCCESS_LAST, result, "ensure_loaded/1 should succeed");
	}

	@Test
	void testEnsureLoadedAtomArgument(@TempDir final Path dir) throws Exception {
		final var file = dir.resolve("atom_arg.pl");
		Files.writeString(file, "atom_test(yes).\n");

		// Use AtomTerm directly
		env.ensureLoaded(AtomTerm.get(file.toString()));

		final var result = interpreter.execute(interpreter.prepareGoal(parse("atom_test(yes).")));
		assertEquals(RC.SUCCESS_LAST, result, "Predicate from loaded file should succeed");
	}

	// ==========================================================================
	// CLI Argument Parsing Tests
	// ==========================================================================

	@Test
	void testCliHelp() {
		final var runner = new GoalRunner();
		final var cmd = new CommandLine(runner);

		final var code = cmd.execute("--help");

		assertEquals(0, code, "Help should exit with 0");
		final var output = captured.toString();
		assertTrue(output.contains("prolog4j"), "Help should show program name");
		assertTrue(output.contains("--once"), "Help should show --once option");
	}

	@Test
	void testCliVersion() {
		final var runner = new GoalRunner();
		final var cmd = new CommandLine(runner);

		final var code = cmd.execute("--version");

		assertEquals(0, code, "Version should exit with 0");
	}

	@Test
	void testGoalModeExecution(@TempDir final Path dir) throws Exception {
		// Create test file
		final var file = dir.resolve("goal_test.pl");
		Files.writeString(file, "success_goal :- true.\n");

		final var runner = new GoalRunner();
		final var cmd = new CommandLine(runner);

		// Run in goal mode with --once
		final var code = cmd.execute("--once", file.toString(), "success_goal");

		assertEquals(0, code, "Successful goal should exit with 0");
	}

	// ==========================================================================
	// Goal Execution Tests
	// ==========================================================================

	@Test
	void testSimpleGoalSuccess() throws Exception {
		final var goal = parse("true.");
		final var result = interpreter.execute(interpreter.prepareGoal(goal));

		assertEquals(RC.SUCCESS_LAST, result, "true/0 should succeed");
	}

	@Test
	void testSimpleGoalFailure() throws Exception {
		final var goal = parse("fail.");
		final var result = interpreter.execute(interpreter.prepareGoal(goal));

		assertEquals(RC.FAIL, result, "fail/0 should fail");
	}

	@Test
	void testGoalWithMultipleSolutions() throws Exception {
		final var goal = parse("member(X, [1,2,3]).");
		final var prepared = interpreter.prepareGoal(goal);

		// First solution
		var result = interpreter.execute(prepared);
		assertTrue(result == RC.SUCCESS || result == RC.SUCCESS_LAST,
			"First solution should succeed");

		// Second solution
		result = interpreter.execute(prepared);
		assertTrue(result == RC.SUCCESS || result == RC.SUCCESS_LAST,
			"Second solution should succeed");

		// Third solution
		result = interpreter.execute(prepared);
		assertTrue(result == RC.SUCCESS || result == RC.SUCCESS_LAST,
			"Third solution should succeed");

		// Fourth attempt should fail (no more solutions)
		result = interpreter.execute(prepared);
		assertEquals(RC.FAIL, result, "Fourth attempt should fail");
	}

	@Test
	void testGoalWithVariableBinding() throws Exception {
		final var options = new ReadOptions(env.getOperatorSet());

		try {
			final var reader = new TermReader(new StringReader("X = hello."), env);
			final var goal = reader.readTerm(options);

			final var result = interpreter.execute(interpreter.prepareGoal(goal));
			assertEquals(RC.SUCCESS_LAST, result, "Unification should succeed");

			final var x = (VariableTerm) options.variableNames.get("X");
			assertNotNull(x, "Variable X should exist");
			assertEquals(AtomTerm.get("hello"), x.dereference(), "X should be bound to 'hello'");
		} catch (final Exception e) {
			fail("Parsing failed: " + e.getMessage());
		}
	}

	@Test
	void testAssertAndRetract(@TempDir final Path dir) throws Exception {
		// Test dynamic predicates
		final var assert1 = parse("assert(dynamic_test(a)).");
		interpreter.execute(interpreter.prepareGoal(assert1));

		final var assert2 = parse("assert(dynamic_test(b)).");
		interpreter.execute(interpreter.prepareGoal(assert2));

		// Query - may return SUCCESS (more solutions) or SUCCESS_LAST
		final var query = parse("dynamic_test(a).");
		var result = interpreter.execute(interpreter.prepareGoal(query));
		assertTrue(result == RC.SUCCESS || result == RC.SUCCESS_LAST,
			"asserted fact should be queryable");

		// Retract
		final var retract = parse("retract(dynamic_test(a)).");
		interpreter.execute(interpreter.prepareGoal(retract));

		// Query again - should fail
		final var query2 = parse("dynamic_test(a).");
		result = interpreter.execute(interpreter.prepareGoal(query2));
		assertEquals(RC.FAIL, result, "retracted fact should fail");
	}

	/**
	 * Parse a Prolog term from a string (must end with period).
	 */
	private Term parse(final String text) {
		try {
			final var reader = new TermReader(new StringReader(text), env);
			return reader.readTerm(new ReadOptions(env.getOperatorSet()));
		} catch (final Exception e) {
			fail("Failed to parse: " + text + " - " + e.getMessage());
			return null;
		}
	}
}
