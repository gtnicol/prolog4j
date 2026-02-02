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
 * ISO Prolog compliance tests for type testing predicates.
 *
 * Tests cover:
 * - var/1, nonvar/1 (ISO 8.3.1-8.3.2)
 * - atom/1 (ISO 8.3.3)
 * - integer/1, float/1, number/1 (ISO 8.3.4-8.3.6)
 * - atomic/1 (ISO 8.3.7)
 * - compound/1 (ISO 8.3.8)
 * - callable/1
 * - ground/1
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) section 8.3
 */
class ISOTypeTestingTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== var/1 and nonvar/1 tests ==========

	@Test
	void testVarWithVariable() throws Exception {
		Term goal = parseGoal("var(X)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testVarWithAtom() throws Exception {
		Term goal = parseGoal("var(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testVarWithInteger() throws Exception {
		Term goal = parseGoal("var(42)");
		assertGoalFails(goal);
	}

	@Test
	void testVarWithCompound() throws Exception {
		Term goal = parseGoal("var(foo(bar))");
		assertGoalFails(goal);
	}

	@Test
	void testNonvarWithAtom() throws Exception {
		Term goal = parseGoal("nonvar(atom)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNonvarWithVariable() throws Exception {
		Term goal = parseGoal("nonvar(X)");
		assertGoalFails(goal);
	}

	@Test
	void testNonvarWithBoundVariable() throws Exception {
		Term goal = parseGoal("X = 5, nonvar(X)");
		assertGoalSucceeds(goal);
	}

	// ========== atom/1 tests ==========

	@Test
	void testAtomWithAtom() throws Exception {
		Term goal = parseGoal("atom(hello)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomWithEmptyList() throws Exception {
		Term goal = parseGoal("atom([])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomWithInteger() throws Exception {
		Term goal = parseGoal("atom(42)");
		assertGoalFails(goal);
	}

	@Test
	void testAtomWithFloat() throws Exception {
		Term goal = parseGoal("atom(3.14)");
		assertGoalFails(goal);
	}

	@Test
	void testAtomWithCompound() throws Exception {
		Term goal = parseGoal("atom(foo(bar))");
		assertGoalFails(goal);
	}

	@Test
	void testAtomWithVariable() throws Exception {
		Term goal = parseGoal("atom(X)");
		assertGoalFails(goal);
	}

	// ========== integer/1 tests ==========

	@Test
	void testIntegerWithInteger() throws Exception {
		Term goal = parseGoal("integer(42)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIntegerWithNegative() throws Exception {
		Term goal = parseGoal("integer(-17)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIntegerWithZero() throws Exception {
		Term goal = parseGoal("integer(0)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIntegerWithFloat() throws Exception {
		Term goal = parseGoal("integer(3.14)");
		assertGoalFails(goal);
	}

	@Test
	void testIntegerWithAtom() throws Exception {
		Term goal = parseGoal("integer(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testIntegerWithVariable() throws Exception {
		Term goal = parseGoal("integer(X)");
		assertGoalFails(goal);
	}

	// ========== float/1 tests ==========

	@Test
	void testFloatWithFloat() throws Exception {
		Term goal = parseGoal("float(3.14)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFloatWithNegativeFloat() throws Exception {
		Term goal = parseGoal("float(-2.5)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFloatWithInteger() throws Exception {
		Term goal = parseGoal("float(42)");
		assertGoalFails(goal);
	}

	@Test
	void testFloatWithAtom() throws Exception {
		Term goal = parseGoal("float(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testFloatWithVariable() throws Exception {
		Term goal = parseGoal("float(X)");
		assertGoalFails(goal);
	}

	// ========== number/1 tests ==========

	@Test
	void testNumberWithInteger() throws Exception {
		Term goal = parseGoal("number(42)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNumberWithFloat() throws Exception {
		Term goal = parseGoal("number(3.14)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNumberWithAtom() throws Exception {
		Term goal = parseGoal("number(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testNumberWithVariable() throws Exception {
		Term goal = parseGoal("number(X)");
		assertGoalFails(goal);
	}

	// ========== atomic/1 tests ==========

	@Test
	void testAtomicWithAtom() throws Exception {
		Term goal = parseGoal("atomic(hello)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomicWithInteger() throws Exception {
		Term goal = parseGoal("atomic(42)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomicWithFloat() throws Exception {
		Term goal = parseGoal("atomic(3.14)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAtomicWithCompound() throws Exception {
		Term goal = parseGoal("atomic(foo(bar))");
		assertGoalFails(goal);
	}

	@Test
	void testAtomicWithVariable() throws Exception {
		Term goal = parseGoal("atomic(X)");
		assertGoalFails(goal);
	}

	@Test
	void testAtomicWithEmptyList() throws Exception {
		Term goal = parseGoal("atomic([])");
		assertGoalSucceeds(goal);
	}

	// ========== compound/1 tests ==========

	@Test
	void testCompoundWithCompound() throws Exception {
		Term goal = parseGoal("compound(foo(bar))");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCompoundWithList() throws Exception {
		Term goal = parseGoal("compound([1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCompoundWithAtom() throws Exception {
		Term goal = parseGoal("compound(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testCompoundWithInteger() throws Exception {
		Term goal = parseGoal("compound(42)");
		assertGoalFails(goal);
	}

	@Test
	void testCompoundWithVariable() throws Exception {
		Term goal = parseGoal("compound(X)");
		assertGoalFails(goal);
	}

	@Test
	void testCompoundWithEmptyList() throws Exception {
		Term goal = parseGoal("compound([])");
		assertGoalFails(goal);
	}

	// ========== callable/1 tests (extension) ==========

	@Test
	void testCallableWithAtom() throws Exception {
		Term goal = parseGoal("callable(atom)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCallableWithCompound() throws Exception {
		Term goal = parseGoal("callable(foo(bar))");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCallableWithInteger() throws Exception {
		Term goal = parseGoal("callable(42)");
		assertGoalFails(goal);
	}

	@Test
	void testCallableWithVariable() throws Exception {
		Term goal = parseGoal("callable(X)");
		assertGoalFails(goal);
	}

	// ========== ground/1 tests (extension) ==========

	@Test
	void testGroundWithAtom() throws Exception {
		Term goal = parseGoal("ground(atom)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testGroundWithInteger() throws Exception {
		Term goal = parseGoal("ground(42)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testGroundWithCompound() throws Exception {
		Term goal = parseGoal("ground(foo(bar, 123))");
		assertGoalSucceeds(goal);
	}

	@Test
	void testGroundWithVariable() throws Exception {
		Term goal = parseGoal("ground(X)");
		assertGoalFails(goal);
	}

	@Test
	void testGroundWithCompoundContainingVariable() throws Exception {
		Term goal = parseGoal("ground(foo(X))");
		assertGoalFails(goal);
	}

	@Test
	void testGroundWithList() throws Exception {
		Term goal = parseGoal("ground([1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testGroundWithListContainingVariable() throws Exception {
		Term goal = parseGoal("ground([1, X, 3])");
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
}
