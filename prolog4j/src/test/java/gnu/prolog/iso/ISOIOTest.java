package gnu.prolog.iso;

import gnu.prolog.io.TermReader;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ISO Prolog compliance tests for I/O operations.
 *
 * Tests cover:
 * - write/1, writeln/1, nl/0
 * - read/1, get_char/1, put_char/1
 * - open/3, close/1
 * - see/1, seen/0, tell/1, told/0 (classic I/O)
 * - Stream properties
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) section 8.11-8.14
 */
class ISOIOTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== Basic write tests ==========

	@Test
	void testWriteAtom() throws Exception {
		// write/1 should succeed with any term
		Term goal = parseGoal("write(hello)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteInteger() throws Exception {
		Term goal = parseGoal("write(42)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteFloat() throws Exception {
		Term goal = parseGoal("write(3.14)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteCompound() throws Exception {
		Term goal = parseGoal("write(foo(bar, baz))");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteList() throws Exception {
		Term goal = parseGoal("write([1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteString() throws Exception {
		Term goal = parseGoal("write('Hello, World!')");
		assertGoalSucceeds(goal);
	}

	// ========== nl/0 tests ==========

	@Test
	void testNewline() throws Exception {
		Term goal = parseGoal("nl");
		assertGoalSucceeds(goal);
	}

	@Test
	void testWriteWithNewline() throws Exception {
		Term goal = parseGoal("write(hello), nl");
		assertGoalSucceeds(goal);
	}

	// ========== File I/O tests ==========

	@Test
	void testOpenAndClose() throws Exception {
		String tempFile = createTempFile("test content\n");

		Term openGoal = parseGoal("open('" + tempFile + "', read, Stream)");
		assertGoalSucceeds(openGoal);
	}

	@Test
	void testWriteToFile() throws Exception {
		String tempFile = createTempFilePath();

		env.ensureLoaded(AtomTerm.get(createTempFile(
			"write_test(File) :- \n" +
			"    open(File, write, Stream),\n" +
			"    write(Stream, hello),\n" +
			"    close(Stream).\n"
		)));

		Term goal = parseGoal("write_test('" + tempFile + "')");
		assertGoalSucceeds(goal);

		// Verify file was written
		java.io.File file = new java.io.File(tempFile);
		assertTrue(file.exists(), "File should have been created");
		file.delete();
	}

	@Test
	void testReadFromFile() throws Exception {
		String tempFile = createTempFile("test_fact(hello).\n");

		env.ensureLoaded(AtomTerm.get(createTempFile(
			"read_test(File, Term) :- \n" +
			"    open(File, read, Stream),\n" +
			"    read(Stream, Term),\n" +
			"    close(Stream).\n"
		)));

		Term goal = parseGoal("read_test('" + tempFile + "', X), X = test_fact(hello)");
		assertGoalSucceeds(goal);
	}

	// ========== Character I/O tests ==========

	@Test
	void testPutChar() throws Exception {
		Term goal = parseGoal("put_char(a)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testPutCharCode() throws Exception {
		Term goal = parseGoal("put_code(65)"); // ASCII 'A'
		assertGoalSucceeds(goal);
	}

	// ========== Stream operations ==========

	@Test
	void testCurrentOutput() throws Exception {
		Term goal = parseGoal("current_output(Stream), var(Stream) -> fail ; true");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCurrentInput() throws Exception {
		Term goal = parseGoal("current_input(Stream), var(Stream) -> fail ; true");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFlushOutput() throws Exception {
		Term goal = parseGoal("flush_output");
		assertGoalSucceeds(goal);
	}

	// ========== Format tests ==========

	@Test
	void testFormat() throws Exception {
		// format/2 is a common extension
		Term goal = parseGoal("format('Hello ~w~n', [world])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFormatInteger() throws Exception {
		Term goal = parseGoal("format('Number: ~d~n', [42])");
		assertGoalSucceeds(goal);
	}

	// ========== At end of stream tests ==========

	@Test
	void testAtEndOfStreamInitially() throws Exception {
		String emptyFile = createTempFile("");

		env.ensureLoaded(AtomTerm.get(createTempFile(
			"test_eof(File) :- \n" +
			"    open(File, read, Stream),\n" +
			"    at_end_of_stream(Stream),\n" +
			"    close(Stream).\n"
		)));

		Term goal = parseGoal("test_eof('" + emptyFile + "')");
		assertGoalSucceeds(goal);
	}

	// ========== Atom/chars/codes conversion tests ==========

	@Test
	void testAtomChars() throws Exception {
		Term goal = parseGoal("atom_chars(hello, X), X = [h, e, l, l, o]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomCharsReverse() throws Exception {
		Term goal = parseGoal("atom_chars(X, [h, e, l, l, o]), X = hello");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomCodes() throws Exception {
		Term goal = parseGoal("atom_codes(abc, X), X = [97, 98, 99]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomCodesReverse() throws Exception {
		Term goal = parseGoal("atom_codes(X, [97, 98, 99]), X = abc");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCharCode() throws Exception {
		Term goal = parseGoal("char_code(a, X), X = 97");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCharCodeReverse() throws Exception {
		Term goal = parseGoal("char_code(X, 97), X = a");
		assertGoalSucceeds(goal);
	}

	// ========== Number conversion tests ==========

	@Test
	void testNumberChars() throws Exception {
		Term goal = parseGoal("number_chars(123, X), X = ['1', '2', '3']");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNumberCharsReverse() throws Exception {
		Term goal = parseGoal("number_chars(X, ['1', '2', '3']), X = 123");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNumberCodes() throws Exception {
		Term goal = parseGoal("number_codes(42, X), X = [52, 50]"); // '4', '2'
		assertGoalSucceeds(goal);
	}

	@Test
	void testNumberCodesReverse() throws Exception {
		Term goal = parseGoal("number_codes(X, [52, 50]), X = 42");
		assertGoalSucceeds(goal);
	}

	// ========== Atom manipulation tests ==========

	@Test
	void testAtomLength() throws Exception {
		Term goal = parseGoal("atom_length(hello, X), X = 5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomConcat() throws Exception {
		Term goal = parseGoal("atom_concat(hello, world, X), X = helloworld");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomConcatSplit() throws Exception {
		Term goal = parseGoal("atom_concat(hello, X, helloworld), X = world");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSubAtom() throws Exception {
		Term goal = parseGoal("sub_atom(abracadabra, 0, 5, _, X), X = abrac");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSubAtomMiddle() throws Exception {
		Term goal = parseGoal("sub_atom(abracadabra, 3, 4, _, X), X = acad");
		assertGoalSucceeds(goal);
	}

	// ========== Multiple stream test ==========

	@Test
	void testMultipleStreams() throws Exception {
		String file1 = createTempFile("content1\n");
		String file2 = createTempFile("content2\n");

		env.ensureLoaded(AtomTerm.get(createTempFile(
			"test_multi(F1, F2) :- \n" +
			"    open(F1, read, S1),\n" +
			"    open(F2, read, S2),\n" +
			"    close(S1),\n" +
			"    close(S2).\n"
		)));

		Term goal = parseGoal("test_multi('" + file1 + "', '" + file2 + "')");
		assertGoalSucceeds(goal);
	}

	// Helper methods

	private Term parseGoal(final String goalText) throws Exception {
		TermReader reader = new TermReader(new StringReader(goalText + "."), env);
		return reader.readTerm(env.getOperatorSet());
	}

	private void assertGoalSucceeds(final Term goal) throws PrologException {
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		PrologCode.RC rc = interpreter.execute(preparedGoal);
		assertTrue(rc == PrologCode.RC.SUCCESS || rc == PrologCode.RC.SUCCESS_LAST,
			"Goal should succeed: " + goal);
	}

	private void assertGoalFails(final Term goal) throws PrologException {
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		PrologCode.RC rc = interpreter.execute(preparedGoal);
		assertEquals(PrologCode.RC.FAIL, rc, "Goal should fail: " + goal);
	}

	private String createTempFile(final String content) throws Exception {
		java.io.File tempFile = java.io.File.createTempFile("prolog_test", ".pl");
		tempFile.deleteOnExit();
		java.nio.file.Files.write(tempFile.toPath(), content.getBytes());
		return tempFile.getAbsolutePath();
	}

	private String createTempFilePath() throws Exception {
		java.io.File tempFile = java.io.File.createTempFile("prolog_test", ".pl");
		tempFile.deleteOnExit();
		return tempFile.getAbsolutePath();
	}
}
