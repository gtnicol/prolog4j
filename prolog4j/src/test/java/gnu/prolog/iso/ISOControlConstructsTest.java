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
 * ISO Prolog compliance tests for control constructs:
 * - true/0
 * - fail/0
 * - !/0 (cut)
 * - ,/2 (conjunction)
 * - ;/2 (disjunction)
 * - ->/2 (if-then)
 * - ->/2 ; /2 (if-then-else)
 * - call/1
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard)
 */
class ISOControlConstructsTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	/**
	 * Test true/0 - always succeeds
	 * ISO 7.8.2
	 */
	@Test
	void testTrue() throws Exception {
		Term goal = parseGoal("true");
		assertGoalSucceeds(goal);
	}

	/**
	 * Test fail/0 - always fails
	 * ISO 7.8.3
	 */
	@Test
	void testFail() throws Exception {
		Term goal = parseGoal("fail");
		assertGoalFails(goal);
	}

	/**
	 * Test conjunction (,/2) - both goals must succeed
	 * ISO 7.8.4
	 */
	@Test
	void testConjunctionBothSucceed() throws Exception {
		Term goal = parseGoal("true, true");
		assertGoalSucceeds(goal);
	}

	@Test
	void testConjunctionFirstFails() throws Exception {
		Term goal = parseGoal("fail, true");
		assertGoalFails(goal);
	}

	@Test
	void testConjunctionSecondFails() throws Exception {
		Term goal = parseGoal("true, fail");
		assertGoalFails(goal);
	}

	/**
	 * Test disjunction (;/2) - at least one goal must succeed
	 * ISO 7.8.5
	 */
	@Test
	void testDisjunctionFirstSucceeds() throws Exception {
		Term goal = parseGoal("true ; fail");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDisjunctionSecondSucceeds() throws Exception {
		Term goal = parseGoal("fail ; true");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDisjunctionBothFail() throws Exception {
		Term goal = parseGoal("fail ; fail");
		assertGoalFails(goal);
	}

	/**
	 * Test if-then-else (-> ; /2)
	 * ISO 7.8.6
	 */
	@Test
	void testIfThenElseConditionTrue() throws Exception {
		Term goal = parseGoal("(true -> true ; fail)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIfThenElseConditionFalse() throws Exception {
		Term goal = parseGoal("(fail -> fail ; true)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIfThenElseThenBranchFails() throws Exception {
		Term goal = parseGoal("(true -> fail ; true)");
		assertGoalFails(goal);
	}

	@Test
	void testIfThenElseElseBranchFails() throws Exception {
		Term goal = parseGoal("(fail -> true ; fail)");
		assertGoalFails(goal);
	}

	/**
	 * Test cut (!/0) - commits to current clause, removes choice points
	 * ISO 7.8.1
	 */
	@Test
	void testCutRemovesChoicePoints() throws Exception {
		// Define: test_cut :- (true ; true), !, fail.
		// Without cut, this would backtrack to second 'true'
		// With cut, it commits and fails
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"test_cut :- (true ; true), !, fail.\n" +
			"test_cut :- true.\n"
		)));

		Term goal = parseGoal("test_cut");
		assertGoalFails(goal);
	}

	/**
	 * Test call/1 - meta-call
	 * ISO 7.8.7
	 */
	@Test
	void testCallWithAtom() throws Exception {
		Term goal = parseGoal("call(true)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCallWithCompound() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"foo(a).\n"
		)));

		Term goal = parseGoal("call(foo(a))");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCallFails() throws Exception {
		Term goal = parseGoal("call(fail)");
		assertGoalFails(goal);
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
