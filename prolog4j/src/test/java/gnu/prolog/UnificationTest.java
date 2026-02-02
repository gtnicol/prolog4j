package gnu.prolog;

import gnu.prolog.term.*;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Prolog term unification.
 */
class UnificationTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	@Test
	void testUnifyIdenticalAtoms() throws PrologException {
		AtomTerm atom1 = AtomTerm.get("foo");
		AtomTerm atom2 = AtomTerm.get("foo");

		PrologCode.RC result = interpreter.unify(atom1, atom2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyDifferentAtoms() throws PrologException {
		AtomTerm atom1 = AtomTerm.get("foo");
		AtomTerm atom2 = AtomTerm.get("bar");

		PrologCode.RC result = interpreter.unify(atom1, atom2);
		assertEquals(PrologCode.RC.FAIL, result);
	}

	@Test
	void testUnifyIdenticalIntegers() throws PrologException {
		IntegerTerm int1 = IntegerTerm.get(42);
		IntegerTerm int2 = IntegerTerm.get(42);

		PrologCode.RC result = interpreter.unify(int1, int2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyDifferentIntegers() throws PrologException {
		IntegerTerm int1 = IntegerTerm.get(42);
		IntegerTerm int2 = IntegerTerm.get(17);

		PrologCode.RC result = interpreter.unify(int1, int2);
		assertEquals(PrologCode.RC.FAIL, result);
	}

	@Test
	void testUnifyVariableWithAtom() throws PrologException {
		VariableTerm var = new VariableTerm("X");
		AtomTerm atom = AtomTerm.get("foo");

		PrologCode.RC result = interpreter.unify(var, atom);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// After unification, variable should be bound
		Term dereferenced = var.dereference();
		assertEquals(atom, dereferenced);
	}

	@Test
	void testUnifyVariableWithInteger() throws PrologException {
		VariableTerm var = new VariableTerm("X");
		IntegerTerm num = IntegerTerm.get(42);

		PrologCode.RC result = interpreter.unify(var, num);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// After unification, variable should be bound
		Term dereferenced = var.dereference();
		assertEquals(num, dereferenced);
	}

	@Test
	void testUnifyTwoUnboundVariables() throws PrologException {
		VariableTerm var1 = new VariableTerm("X");
		VariableTerm var2 = new VariableTerm("Y");

		PrologCode.RC result = interpreter.unify(var1, var2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// One variable should be bound to the other
		Term deref1 = var1.dereference();
		Term deref2 = var2.dereference();

		// Both should dereference to the same term
		assertSame(deref1, deref2);
	}

	@Test
	void testUnifyBoundVariable() throws PrologException {
		VariableTerm var1 = new VariableTerm("X");
		VariableTerm var2 = new VariableTerm("Y");
		AtomTerm atom = AtomTerm.get("foo");

		// Bind X to foo
		var1.value = atom;

		// Unify Y with X (which is bound to foo)
		PrologCode.RC result = interpreter.unify(var2, var1);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// Y should now be bound to foo (transitively)
		assertEquals(atom, var2.dereference());
	}

	@Test
	void testUnifySimpleCompoundTerms() throws PrologException {
		CompoundTerm ct1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			AtomTerm.get("b")
		);
		CompoundTerm ct2 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			AtomTerm.get("b")
		);

		PrologCode.RC result = interpreter.unify(ct1, ct2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyCompoundTermsDifferentFunctor() throws PrologException {
		CompoundTerm ct1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			AtomTerm.get("b")
		);
		CompoundTerm ct2 = new CompoundTerm(
			CompoundTermTag.get("bar", 2),
			AtomTerm.get("a"),
			AtomTerm.get("b")
		);

		PrologCode.RC result = interpreter.unify(ct1, ct2);
		assertEquals(PrologCode.RC.FAIL, result);
	}

	@Test
	void testUnifyCompoundTermsDifferentArity() throws PrologException {
		CompoundTerm ct1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			AtomTerm.get("b")
		);
		CompoundTerm ct2 = new CompoundTerm(
			CompoundTermTag.get("foo", 1),
			AtomTerm.get("a")
		);

		PrologCode.RC result = interpreter.unify(ct1, ct2);
		assertEquals(PrologCode.RC.FAIL, result);
	}

	@Test
	void testUnifyCompoundTermWithVariable() throws PrologException {
		VariableTerm var = new VariableTerm("X");
		CompoundTerm ct = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			IntegerTerm.get(42)
		);

		PrologCode.RC result = interpreter.unify(var, ct);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// Variable should be bound to the compound term
		assertEquals(ct, var.dereference());
	}

	@Test
	void testUnifyCompoundTermsWithVariables() throws PrologException {
		VariableTerm varX = new VariableTerm("X");
		VariableTerm varY = new VariableTerm("Y");

		CompoundTerm ct1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			varX,
			AtomTerm.get("b")
		);
		CompoundTerm ct2 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			varY
		);

		PrologCode.RC result = interpreter.unify(ct1, ct2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// X should be bound to 'a', Y should be bound to 'b'
		assertEquals(AtomTerm.get("a"), varX.dereference());
		assertEquals(AtomTerm.get("b"), varY.dereference());
	}

	@Test
	void testUnifyWithOccursCheckDecimalTerms() throws PrologException {
		Term left = new DecimalTerm("1.0");
		Term right = new DecimalTerm("1.0");

		PrologCode.RC result = gnu.prolog.vm.builtins.unification.Predicates.UNIFY_WITH_OCCURS_CHECK.execute(
			interpreter,
			false,
			new Term[] { left, right }
		);

		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyNestedCompoundTerms() throws PrologException {
		// foo(bar(a), b)
		CompoundTerm inner1 = new CompoundTerm(
			CompoundTermTag.get("bar", 1),
			AtomTerm.get("a")
		);
		CompoundTerm outer1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			inner1,
			AtomTerm.get("b")
		);

		// foo(bar(a), b)
		CompoundTerm inner2 = new CompoundTerm(
			CompoundTermTag.get("bar", 1),
			AtomTerm.get("a")
		);
		CompoundTerm outer2 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			inner2,
			AtomTerm.get("b")
		);

		PrologCode.RC result = interpreter.unify(outer1, outer2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyList() throws PrologException {
		// [1, 2]
		CompoundTerm list1 = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(1),
			new CompoundTerm(
				CompoundTermTag.get(".", 2),
				IntegerTerm.get(2),
				AtomTerm.get("[]")
			)
		);

		// [1, 2]
		CompoundTerm list2 = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(1),
			new CompoundTerm(
				CompoundTermTag.get(".", 2),
				IntegerTerm.get(2),
				AtomTerm.get("[]")
			)
		);

		PrologCode.RC result = interpreter.unify(list1, list2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyListWithVariable() throws PrologException {
		VariableTerm varH = new VariableTerm("H");
		VariableTerm varT = new VariableTerm("T");

		// [H|T]
		CompoundTerm list1 = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			varH,
			varT
		);

		// [1, 2, 3]
		CompoundTerm expectedTail = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(2),
			new CompoundTerm(
				CompoundTermTag.get(".", 2),
				IntegerTerm.get(3),
				AtomTerm.get("[]")
			)
		);

		CompoundTerm list2 = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(1),
			expectedTail
		);

		PrologCode.RC result = interpreter.unify(list1, list2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);

		// H should be bound to 1
		assertEquals(IntegerTerm.get(1), varH.dereference());

		// T should be bound to [2, 3]
		Term derefT = varT.dereference();
		assertInstanceOf(CompoundTerm.class, derefT);
	}

	@Test
	void testUnifyEmptyLists() throws PrologException {
		AtomTerm empty1 = AtomTerm.get("[]");
		AtomTerm empty2 = AtomTerm.get("[]");

		PrologCode.RC result = interpreter.unify(empty1, empty2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyFloats() throws PrologException {
		FloatTerm float1 = new FloatTerm(3.14);
		FloatTerm float2 = new FloatTerm(3.14);

		PrologCode.RC result = interpreter.unify(float1, float2);
		assertEquals(PrologCode.RC.SUCCESS_LAST, result);
	}

	@Test
	void testUnifyDifferentFloats() throws PrologException {
		FloatTerm float1 = new FloatTerm(3.14);
		FloatTerm float2 = new FloatTerm(2.71);

		PrologCode.RC result = interpreter.unify(float1, float2);
		assertEquals(PrologCode.RC.FAIL, result);
	}
}
