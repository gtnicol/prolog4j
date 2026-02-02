package gnu.prolog;

import gnu.prolog.term.*;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Evaluate;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Prolog arithmetic evaluation and expression evaluation.
 */
class EvaluationTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
		// Note: No need to load init file for arithmetic evaluation tests
		// ensureLoaded errors are only logged, so we avoid loading non-existent files
	}

	@Test
	void testEvaluateInteger() throws PrologException {
		Term result = Evaluate.evaluate(IntegerTerm.get(42));
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(42, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateFloat() throws PrologException {
		Term result = Evaluate.evaluate(new FloatTerm(3.14));
		assertInstanceOf(FloatTerm.class, result);
		assertEquals(3.14, ((FloatTerm) result).value, 0.0001);
	}

	@Test
	void testEvaluateAddition() throws PrologException {
		// 2 + 3 = 5
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("+", 2),
			IntegerTerm.get(2),
			IntegerTerm.get(3)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(5, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateSubtraction() throws PrologException {
		// 10 - 3 = 7
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("-", 2),
			IntegerTerm.get(10),
			IntegerTerm.get(3)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(7, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateMultiplication() throws PrologException {
		// 4 * 5 = 20
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("*", 2),
			IntegerTerm.get(4),
			IntegerTerm.get(5)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(20, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateDivision() throws PrologException {
		// 15 / 3 = 5.0
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("/", 2),
			IntegerTerm.get(15),
			IntegerTerm.get(3)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(FloatTerm.class, result);
		assertEquals(5.0, ((FloatTerm) result).value, 0.0001);
	}

	@Test
	void testEvaluateIntegerDivision() throws PrologException {
		// 15 // 4 = 3
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("//", 2),
			IntegerTerm.get(15),
			IntegerTerm.get(4)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(3, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateModulo() throws PrologException {
		// 15 mod 4 = 3
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("mod", 2),
			IntegerTerm.get(15),
			IntegerTerm.get(4)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(3, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateNegation() throws PrologException {
		// -5
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("-", 1),
			IntegerTerm.get(5)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(-5, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateAbs() throws PrologException {
		// abs(-7) = 7
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("abs", 1),
			IntegerTerm.get(-7)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(7, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateNestedExpression() throws PrologException {
		// (2 + 3) * 4 = 20
		CompoundTerm inner = new CompoundTerm(
			CompoundTermTag.get("+", 2),
			IntegerTerm.get(2),
			IntegerTerm.get(3)
		);
		CompoundTerm outer = new CompoundTerm(
			CompoundTermTag.get("*", 2),
			inner,
			IntegerTerm.get(4)
		);
		Term result = Evaluate.evaluate(outer);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(20, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateComplexExpression() throws PrologException {
		// (10 - 2) / (3 + 1) = 2.0
		CompoundTerm numerator = new CompoundTerm(
			CompoundTermTag.get("-", 2),
			IntegerTerm.get(10),
			IntegerTerm.get(2)
		);
		CompoundTerm denominator = new CompoundTerm(
			CompoundTermTag.get("+", 2),
			IntegerTerm.get(3),
			IntegerTerm.get(1)
		);
		CompoundTerm division = new CompoundTerm(
			CompoundTermTag.get("/", 2),
			numerator,
			denominator
		);
		Term result = Evaluate.evaluate(division);
		assertInstanceOf(FloatTerm.class, result);
		assertEquals(2.0, ((FloatTerm) result).value, 0.0001);
	}

	@Test
	void testEvaluateMixedTypes() throws PrologException {
		// 3 + 2.5 = 5.5
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("+", 2),
			IntegerTerm.get(3),
			new FloatTerm(2.5)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(FloatTerm.class, result);
		assertEquals(5.5, ((FloatTerm) result).value, 0.0001);
	}

	@Test
	void testEvaluateDivisionByZeroThrows() {
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("/", 2),
			IntegerTerm.get(10),
			IntegerTerm.get(0)
		);
		assertThrows(PrologException.class, () -> Evaluate.evaluate(expr));
	}

	@Test
	void testEvaluatePower() throws PrologException {
		// 2 ** 3 = 8
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("**", 2),
			IntegerTerm.get(2),
			IntegerTerm.get(3)
		);
		Term result = Evaluate.evaluate(expr);
		// Result might be float or integer depending on implementation
		assertTrue(result instanceof NumericTerm);
		double value = result instanceof IntegerTerm
			? ((IntegerTerm) result).value
			: ((FloatTerm) result).value;
		assertEquals(8.0, value, 0.0001);
	}

	@Test
	void testEvaluateBitwiseAnd() throws PrologException {
		// 12 /\ 10 = 8 (binary: 1100 /\ 1010 = 1000)
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("/\\", 2),
			IntegerTerm.get(12),
			IntegerTerm.get(10)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(8, ((IntegerTerm) result).value);
	}

	@Test
	void testEvaluateBitwiseOr() throws PrologException {
		// 12 \/ 10 = 14 (binary: 1100 \/ 1010 = 1110)
		CompoundTerm expr = new CompoundTerm(
			CompoundTermTag.get("\\/", 2),
			IntegerTerm.get(12),
			IntegerTerm.get(10)
		);
		Term result = Evaluate.evaluate(expr);
		assertInstanceOf(IntegerTerm.class, result);
		assertEquals(14, ((IntegerTerm) result).value);
	}

}
