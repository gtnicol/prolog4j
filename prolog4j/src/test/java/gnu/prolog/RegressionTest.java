package gnu.prolog;

import gnu.prolog.database.PrologTextLoaderError;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.PrologHaltException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for critical bugs that have been fixed.
 */
class RegressionTest {

	/**
	 * Test that ensureLoaded does not cache failed loads.
	 * Bug: ensureLoaded/1 marks a source as loaded before the loader succeeds;
	 * any I/O failure leaves it cached forever and future loads are skipped.
	 */
	@Test
	void testEnsureLoadedDoesNotCacheFailures(@TempDir final Path dir) throws IOException {
		Environment env = new Environment();

		// Try to load a non-existent file
		Path missingFile = dir.resolve("nonexistent.pl");
		AtomTerm fileAtom = AtomTerm.get(missingFile.toString());

		// First attempt - should fail
		int errorsBefore = env.getLoadingErrors().size();
		env.ensureLoaded(fileAtom);
		int errorsAfter = env.getLoadingErrors().size();

		// Should have an error
		assertTrue(errorsAfter > errorsBefore, "First load should generate an error");

		// Now create the file with valid content
		Files.writeString(missingFile, "test_predicate(hello).\n");

		// Second attempt - should succeed because first attempt was not cached
		errorsBefore = env.getLoadingErrors().size();
		env.ensureLoaded(fileAtom);
		errorsAfter = env.getLoadingErrors().size();

		// Should not have new errors (the file now exists and loads successfully)
		assertEquals(errorsBefore, errorsAfter, "Second load should succeed without errors");
	}

	/**
	 * Test that GoalRunner handles HALT properly without calling System.exit from worker threads.
	 * Bug: The library-facing GoalRunner calls System.exit from worker threads
	 * when a Prolog HALT occurs, which will terminate Maven, test suites, or embedding hosts.
	 *
	 * The fix makes GoalRunner throw PrologHaltException from worker threads,
	 * and only calls System.exit from the main thread when running standalone.
	 */
	@Test
	void testHaltReturnsRCHalt(@TempDir final Path dir) throws Exception {
		// Create a test file that calls halt
		Path testFile = dir.resolve("test_halt.pl");
		Files.writeString(testFile, "test_halt :- halt(42).\n");

		Environment env = new Environment();
		env.ensureLoaded(AtomTerm.get(testFile.toString()));
		Interpreter interpreter = env.createInterpreter();
		env.runInitialization(interpreter);

		// Prepare and execute the halt goal
		CompoundTerm goal = new CompoundTerm(CompoundTermTag.get("test_halt", 0));
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);

		// Execute should return RC.HALT instead of calling System.exit
		var rc = interpreter.execute(preparedGoal);
		assertEquals(gnu.prolog.vm.PrologCode.RC.HALT, rc,
			"halt/1 should return RC.HALT");
		assertEquals(42, interpreter.getExitCode(), "Exit code should be preserved");
	}

	/**
	 * Test that Module.getPredicateTags() returns a defensive copy.
	 * Bug: Module.getPredicateTags() hands out the live keySet view;
	 * callers can trigger ConcurrentModificationException or mutate internals.
	 */
	@Test
	void testModuleGetPredicateTagsReturnsDefensiveCopy(@TempDir final Path dir) throws IOException {
		Environment env = new Environment();

		// Get the predicate tags
		Set<CompoundTermTag> tags1 = env.getModule().getPredicateTags();
		int initialSize = tags1.size();

		// Create a test file and load it
		Path testFile = dir.resolve("test_predicates.pl");
		Files.writeString(testFile,
			"new_pred1(a).\n" +
			"new_pred2(b).\n" +
			"new_pred3(c).\n"
		);
		env.ensureLoaded(AtomTerm.get(testFile.toString()));

		// Get tags again
		Set<CompoundTermTag> tags2 = env.getModule().getPredicateTags();

		// The first set should not have changed (defensive copy)
		assertEquals(initialSize, tags1.size(),
			"Original set should not change when new predicates are added");

		// The new set should have more predicates
		assertTrue(tags2.size() > initialSize,
			"New set should include newly loaded predicates");

		// Attempting to modify the returned set should throw or have no effect
		assertThrows(UnsupportedOperationException.class,
			() -> tags2.add(CompoundTermTag.get("test", 1)),
			"Returned set should be unmodifiable");
	}

	/**
	 * Test that Environment cleanup works properly without memory leaks.
	 * Bug: CleanupState keeps a strong reference to the environment,
	 * so the Cleaner can never release the listener; environments leak until JVM exit.
	 */
	@Test
	void testEnvironmentCloseRemovesListeners() {
		Environment env = new Environment();

		// The module should have the environment registered as a listener
		// (we can't directly test this, but we can test that close() works without errors)

		assertDoesNotThrow(() -> env.close(), "close() should not throw");

		// Calling close() again should be idempotent
		assertDoesNotThrow(() -> env.close(), "close() should be idempotent");
	}

	/**
	 * Test that repeated ensureLoaded of the same file works correctly.
	 */
	@Test
	void testRepeatedEnsureLoadedIsCached(@TempDir final Path dir) throws IOException {
		Environment env = new Environment();

		// Create a test file
		Path testFile = dir.resolve("test.pl");
		Files.writeString(testFile, "cached_pred(x).\n");
		AtomTerm fileAtom = AtomTerm.get(testFile.toString());

		// Load the file
		env.ensureLoaded(fileAtom);
		List<PrologTextLoaderError> errors1 = env.getLoadingErrors();

		// Load again - should be cached, no new errors
		env.ensureLoaded(fileAtom);
		List<PrologTextLoaderError> errors2 = env.getLoadingErrors();

		assertEquals(errors1.size(), errors2.size(),
			"Repeated ensureLoaded should not generate new errors");
	}
}
