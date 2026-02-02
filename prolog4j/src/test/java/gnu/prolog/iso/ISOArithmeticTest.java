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
 * ISO Prolog compliance tests for arithmetic evaluation and comparison.
 *
 * Tests cover:
 * - is/2 (arithmetic evaluation)
 * - Arithmetic comparison: =:=/2, =\=/2, </2, >/2, =</2, >=/2
 * - Arithmetic operators: +, -, *, /, //, mod, rem, ^, abs, sign, etc.
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) sections 9.1-9.4
 */
class ISOArithmeticTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== is/2 tests (ISO 9.1.7) ==========

	@Test
	void testIsBasicEvaluation() throws Exception {
		Term goal = parseGoal("X is 3 + 4, X = 7");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsMultiplication() throws Exception {
		Term goal = parseGoal("X is 3 * 4, X = 12");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsSubtraction() throws Exception {
		Term goal = parseGoal("X is 10 - 3, X = 7");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsFloatDivision() throws Exception {
		Term goal = parseGoal("X is 7 / 2, X = 3.5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsIntegerDivision() throws Exception {
		Term goal = parseGoal("X is 7 // 2, X = 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsModulo() throws Exception {
		Term goal = parseGoal("X is 7 mod 3, X = 1");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsRemainder() throws Exception {
		Term goal = parseGoal("X is 7 rem 3, X = 1");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsPower() throws Exception {
		Term goal = parseGoal("X is 2 ^ 3, X = 8");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsNegation() throws Exception {
		Term goal = parseGoal("X is -(5), X = -5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsAbsoluteValue() throws Exception {
		Term goal = parseGoal("X is abs(-5), X = 5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsSign() throws Exception {
		Term goal1 = parseGoal("X is sign(-5), X = -1");
		assertGoalSucceeds(goal1);

		Term goal2 = parseGoal("Y is sign(5), Y = 1");
		assertGoalSucceeds(goal2);

		Term goal3 = parseGoal("Z is sign(0), Z = 0");
		assertGoalSucceeds(goal3);
	}

	@Test
	void testIsMin() throws Exception {
		Term goal = parseGoal("X is min(3, 5), X = 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsMax() throws Exception {
		Term goal = parseGoal("X is max(3, 5), X = 5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsComplexExpression() throws Exception {
		Term goal = parseGoal("X is (3 + 4) * 2 - 1, X = 13");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsIntegerOverflowRaisesError() throws Exception {
		Term goal = parseGoal("X is 2147483647 + 1");
		PrologException ex = assertThrows(PrologException.class, () -> executeGoal(goal));
		assertTrue(ex.getMessage().contains("evaluation_error(int_overflow)"));
	}

	// ========== Arithmetic comparison tests (ISO 9.1.3-9.1.8) ==========

	@Test
	void testArithmeticEqual() throws Exception {
		Term goal = parseGoal("3 + 4 =:= 7");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArithmeticEqualFails() throws Exception {
		Term goal = parseGoal("3 + 4 =:= 8");
		assertGoalFails(goal);
	}

	@Test
	void testArithmeticNotEqual() throws Exception {
		Term goal = parseGoal("3 + 4 =\\= 8");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArithmeticNotEqualFails() throws Exception {
		Term goal = parseGoal("3 + 4 =\\= 7");
		assertGoalFails(goal);
	}

	@Test
	void testLessThan() throws Exception {
		Term goal = parseGoal("3 < 5");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLessThanFails() throws Exception {
		Term goal = parseGoal("5 < 3");
		assertGoalFails(goal);
	}

	@Test
	void testGreaterThan() throws Exception {
		Term goal = parseGoal("5 > 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testGreaterThanFails() throws Exception {
		Term goal = parseGoal("3 > 5");
		assertGoalFails(goal);
	}

	@Test
	void testLessThanOrEqual() throws Exception {
		Term goal1 = parseGoal("3 =< 5");
		assertGoalSucceeds(goal1);

		Term goal2 = parseGoal("5 =< 5");
		assertGoalSucceeds(goal2);
	}

	@Test
	void testLessThanOrEqualFails() throws Exception {
		Term goal = parseGoal("6 =< 5");
		assertGoalFails(goal);
	}

	@Test
	void testGreaterThanOrEqual() throws Exception {
		Term goal1 = parseGoal("5 >= 3");
		assertGoalSucceeds(goal1);

		Term goal2 = parseGoal("5 >= 5");
		assertGoalSucceeds(goal2);
	}

	@Test
	void testGreaterThanOrEqualFails() throws Exception {
		Term goal = parseGoal("3 >= 5");
		assertGoalFails(goal);
	}

	// ========== Floating point tests ==========

	@Test
	void testFloatArithmetic() throws Exception {
		Term goal = parseGoal("X is 3.5 + 2.5, X = 6.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFloatComparison() throws Exception {
		Term goal = parseGoal("3.5 < 4.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMixedIntegerFloat() throws Exception {
		Term goal = parseGoal("X is 3 + 2.5, X = 5.5");
		assertGoalSucceeds(goal);
	}

	// ========== Transcendental functions ==========

	@Test
	void testSqrt() throws Exception {
		Term goal = parseGoal("X is sqrt(4), X = 2.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSin() throws Exception {
		Term goal = parseGoal("X is sin(0), X = 0.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCos() throws Exception {
		Term goal = parseGoal("X is cos(0), X = 1.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testExp() throws Exception {
		Term goal = parseGoal("X is exp(0), X = 1.0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLog() throws Exception {
		Term goal = parseGoal("X is log(1), X = 0.0");
		assertGoalSucceeds(goal);
	}

	// ========== Bitwise operations ==========

	@Test
	void testBitwiseAnd() throws Exception {
		Term goal = parseGoal("X is 12 /\\ 10, X = 8");
		assertGoalSucceeds(goal);
	}

	@Test
	void testBitwiseOr() throws Exception {
		Term goal = parseGoal("X is 12 \\/ 10, X = 14");
		assertGoalSucceeds(goal);
	}

	@Test
	void testBitwiseXor() throws Exception {
		// Note: xor is not ISO standard, using functional notation
		Term goal = parseGoal("X is xor(12, 10), X = 6");
		assertGoalSucceeds(goal);
	}

	@Test
	void testBitwiseNot() throws Exception {
		Term goal = parseGoal("X is \\(0), X = -1");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLeftShift() throws Exception {
		Term goal = parseGoal("X is 3 << 2, X = 12");
		assertGoalSucceeds(goal);
	}

	@Test
	void testRightShift() throws Exception {
		Term goal = parseGoal("X is 12 >> 2, X = 3");
		assertGoalSucceeds(goal);
	}

	// Helper methods

	private Term parseGoal(final String goalText) throws Exception {
		TermReader reader = new TermReader(new StringReader(goalText + "."), env);
		return reader.readTerm(env.getOperatorSet());
	}

	private void assertGoalSucceeds(final Term goal) throws PrologException {
		PrologCode.RC rc = executeGoal(goal);
		assertTrue(rc == PrologCode.RC.SUCCESS || rc == PrologCode.RC.SUCCESS_LAST,
			"Goal should succeed: " + goal);
	}

	private void assertGoalFails(final Term goal) throws PrologException {
		PrologCode.RC rc = executeGoal(goal);
		assertEquals(PrologCode.RC.FAIL, rc, "Goal should fail: " + goal);
	}

	private PrologCode.RC executeGoal(final Term goal) throws PrologException {
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		return interpreter.execute(preparedGoal);
	}
}
