package gnu.prolog.iso;

import gnu.prolog.io.TermReader;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.IntegerTerm;
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
 * ISO Prolog compliance tests for DCG (Definite Clause Grammars).
 *
 * DCGs are a notation for expressing grammar rules that get translated
 * into standard Prolog predicates. The --> operator defines DCG rules,
 * and phrase/2 is used to invoke them.
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard)
 */
class ISODCGTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	/**
	 * Test basic DCG rule with terminals
	 */
	@Test
	void testBasicDCGWithTerminals() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"sentence --> [hello], [world].\n"
		)));

		Term goal = parseGoal("phrase(sentence, [hello, world])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testBasicDCGWithTerminalsFails() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"sentence --> [hello], [world].\n"
		)));

		Term goal = parseGoal("phrase(sentence, [hello, universe])");
		assertGoalFails(goal);
	}

	/**
	 * Test DCG with non-terminals
	 */
	@Test
	void testDCGWithNonTerminals() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"sentence --> noun, verb.\n" +
			"noun --> [cat].\n" +
			"noun --> [dog].\n" +
			"verb --> [runs].\n" +
			"verb --> [walks].\n"
		)));

		Term goal = parseGoal("phrase(sentence, [cat, runs])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDCGWithNonTerminalsAlternative() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"sentence --> noun, verb.\n" +
			"noun --> [cat].\n" +
			"noun --> [dog].\n" +
			"verb --> [runs].\n" +
			"verb --> [walks].\n"
		)));

		Term goal = parseGoal("phrase(sentence, [dog, walks])");
		assertGoalSucceeds(goal);
	}

	/**
	 * Test DCG with arguments (parameterized non-terminals)
	 */
	@Test
	void testDCGWithArguments() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"replicate(0, _) --> [].\n" +
			"replicate(N, X) --> {N > 0}, [X], {N1 is N - 1}, replicate(N1, X).\n"
		)));

		Term goal = parseGoal("phrase(replicate(3, a), [a, a, a])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDCGWithArgumentsFails() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"replicate(0, _) --> [].\n" +
			"replicate(N, X) --> {N > 0}, [X], {N1 is N - 1}, replicate(N1, X).\n"
		)));

		Term goal = parseGoal("phrase(replicate(3, a), [a, a, b])");
		assertGoalFails(goal);
	}

	/**
	 * Test DCG with embedded Prolog goals using {}
	 */
	@Test
	void testDCGWithEmbeddedGoals() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"number(N) --> [N], {integer(N)}.\n"
		)));

		Term goal = parseGoal("phrase(number(42), [42])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDCGWithEmbeddedGoalsFails() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"number(N) --> [N], {integer(N)}.\n"
		)));

		Term goal = parseGoal("phrase(number(X), [hello])");
		assertGoalFails(goal);
	}

	/**
	 * Test DCG with difference lists
	 */
	@Test
	void testDCGWithDifferenceLists() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"append_dl(A-B, B-C, A-C).\n" +
			"simple --> [a], [b].\n"
		)));

		// phrase/2 should work with difference lists
		Term goal = parseGoal("phrase(simple, L, Rest)");
		assertGoalSucceeds(goal);
	}

	/**
	 * Test DCG with empty production
	 */
	@Test
	void testDCGWithEmptyProduction() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"optional_word --> [word].\n" +
			"optional_word --> [].\n"
		)));

		Term goal1 = parseGoal("phrase(optional_word, [word])");
		assertGoalSucceeds(goal1);

		Term goal2 = parseGoal("phrase(optional_word, [])");
		assertGoalSucceeds(goal2);
	}

	/**
	 * Test DCG with disjunction
	 */
	@Test
	void testDCGWithDisjunction() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"word --> [hello] ; [hi] ; [greetings].\n"
		)));

		Term goal1 = parseGoal("phrase(word, [hello])");
		assertGoalSucceeds(goal1);

		Term goal2 = parseGoal("phrase(word, [hi])");
		assertGoalSucceeds(goal2);

		Term goal3 = parseGoal("phrase(word, [greetings])");
		assertGoalSucceeds(goal3);
	}

	/**
	 * Test DCG with cut
	 */
	@Test
	void testDCGWithCut() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"deterministic --> [a], !, [b].\n" +
			"deterministic --> [a], [c].\n"
		)));

		// First clause should commit due to cut
		Term goal = parseGoal("phrase(deterministic, [a, b])");
		assertGoalSucceeds(goal);

		// Second clause should not be tried after cut
		Term goal2 = parseGoal("phrase(deterministic, [a, c])");
		assertGoalFails(goal2);
	}

	/**
	 * Test DCG generating lists (phrase/2 with unbound list)
	 */
	@Test
	void testDCGGeneration() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"simple --> [a], [b], [c].\n"
		)));

		Term goal = parseGoal("phrase(simple, X), X = [a, b, c]");
		assertGoalSucceeds(goal);
	}

	/**
	 * Test DCG with pushback (semicontext notation)
	 * Note: This is an extension in some Prolog systems
	 */
	@Test
	void testDCGRecursion() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"zeros([]) --> [].\n" +
			"zeros([0|Zs]) --> [0], zeros(Zs).\n"
		)));

		Term goal = parseGoal("phrase(zeros([0, 0, 0]), [0, 0, 0])");
		assertGoalSucceeds(goal);
	}

	/**
	 * Test complex DCG example - simple expression grammar
	 */
	@Test
	void testComplexDCGExpression() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"expr(N) --> term(N).\n" +
			"expr(N) --> term(T), [+], expr(E), {N is T + E}.\n" +
			"term(N) --> [N], {integer(N)}.\n"
		)));

		Term goal = parseGoal("phrase(expr(X), [1, +, 2], []), X = 3");
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
}
